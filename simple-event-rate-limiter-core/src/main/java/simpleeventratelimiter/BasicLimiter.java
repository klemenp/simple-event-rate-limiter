/**
 * Copyright © 2016 Klemen Polanec
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package simpleeventratelimiter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simpleeventratelimiter.exception.EventLimitException;
import simpleeventratelimiter.exception.EventRegisteredException;
import simpleeventratelimiter.exception.NoEventRegisteredException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * Thread safe non-persistent limiter for single JVM.
 *
 * Created by Klemen Polanec on 13.12.2016.
 */
public class BasicLimiter implements Limiter {
    private static final Logger log = LoggerFactory.getLogger(BasicLimiter.class);

    private static BasicLimiter instance = null;

    private Map<String,EventLogbook> eventLogbooks;
    private Map<String,AtomicLong> nextAllowedTimestamps;

    private BasicLimiter()
    {
        eventLogbooks = new ConcurrentHashMap<String, EventLogbook>();
        nextAllowedTimestamps = new ConcurrentHashMap<String, AtomicLong>();
    }

    private EventLogbook getEventLogbook(String eventKey)
    {
        return eventLogbooks.get(eventKey);
    }

    /**
     * Logs event or throws @{@link EventLimitException} if rate limit reached.
     * Throws @{@link NoEventRegisteredException} if there's is no event registered.
     *
     * @param eventKey
     * @throws EventLimitException
     * @throws NoEventRegisteredException
     */
    public void logEvent(String eventKey) throws EventLimitException, NoEventRegisteredException
    {
        long logTimestamp = System.currentTimeMillis();
        EventLogbook eventLogbook = getEventLogbook(eventKey);
        if (eventLogbook==null)
        {
            throw new NoEventRegisteredException("No event registered for event key " + eventKey);
        }
        if (eventLogbook.getUnhandledLogs().incrementAndGet()==1)
        {
            eventLogbook.atLeastOnceHandled=false;
        }

        Thread thread = new Thread(()-> {
            eventLogbook.eventTimestamps.add(logTimestamp);
            long oldestTimestamp = logTimestamp - eventLogbook.milllisInterval;
            // Clean up old logs and find new nextAllowedTimestamp;

            int count = 0;
            synchronized (eventLogbook.eventTimestamps) {
                Iterator<Long> logsIterator = eventLogbook.eventTimestamps.iterator();
                while (logsIterator.hasNext()) {
                    long currentTimestamp = logsIterator.next();
                    if (currentTimestamp < oldestTimestamp) {
                        logsIterator.remove();
                    } else {
                        if (oldestTimestamp > currentTimestamp) {
                            oldestTimestamp = currentTimestamp;
                        }
                        count++;
                    }
                }
            }
            if (count>=eventLogbook.limit)
            {
                AtomicLong nextAllowedTimestampObj =  nextAllowedTimestamps.get(eventKey);
                if (nextAllowedTimestampObj==null)
                {
                    nextAllowedTimestamps.put(eventKey, new AtomicLong(oldestTimestamp + eventLogbook.milllisInterval));
                }
                else
                {
                    nextAllowedTimestampObj.set(oldestTimestamp + eventLogbook.milllisInterval);
                }
            }
            else
            {
                nextAllowedTimestamps.remove(eventKey);
            }
            eventLogbook.getUnhandledLogs().decrementAndGet();
            eventLogbook.atLeastOnceHandled|=true;
        });
        thread.start();

        long shortTermRequetsLeft = eventLogbook.getShortTermEventLogsLeftInInterval(logTimestamp);
        if (shortTermRequetsLeft < 0) {
            throw new EventLimitException("Limit reached for event key " + eventKey + ". Short term counter at limit");
        }

        AtomicLong nextAllowedTimestamp = nextAllowedTimestamps.get(eventKey);
        if (nextAllowedTimestamp==null)
        {
            return;
        }
        else if (nextAllowedTimestamp.longValue()>logTimestamp)
        {
            throw new EventLimitException("Limit reached for event key " + eventKey + ". Next event allowed on " + nextAllowedTimestamp.longValue());
        }
    }

    private boolean isEventRegistered(String eventKey)
    {
        return eventLogbooks.containsKey(eventKey);
    }

    /**
     * Logs event or throws @{@link EventLimitException} if rate limit reached.
     * Event does not need to be registered beforehand. It gets registered on the fly if it is not yet.
     * If it is already registered, no registration is executed, only logging.
     *
     * @param eventKey
     * @param limit
     * @param interval
     * @param unit
     * @throws EventLimitException
     */
    public void logEvent(String eventKey, int limit, int interval, TimeUnit unit) throws EventLimitException
    {
        if (!isEventRegistered(eventKey))
        {
            EventLogbook eventLogbook = new EventLogbook(eventKey, limit, true, interval, unit);
            synchronized (eventLogbook) {
                eventLogbooks.putIfAbsent(eventKey, eventLogbook);
                try {
                    logEvent(eventKey);
                }
                catch (Exception e)
                {
                    log.warn(e.getMessage());
                }
            }
        }
        else {
            try {
                logEvent(eventKey);
            } catch (NoEventRegisteredException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

    /**
     * Registers event or throws @{@link EventRegisteredException} if event is already registered.
     *
     * @param eventKey
     * @param limit
     * @param interval
     * @param unit
     * @throws EventLimitException
     */
    public void registerEvent(String eventKey, int limit, long interval, TimeUnit unit) throws EventRegisteredException
    {
        EventLogbook eventLogbook = getEventLogbook(eventKey);
        if (eventLogbook!=null)
        {
            throw new EventRegisteredException("Event is already registered for key " + eventKey);
        }
        eventLogbook = new EventLogbook(eventKey, limit, true, interval, unit);
        eventLogbooks.putIfAbsent(eventKey, eventLogbook);

    }

    /**
     * Returns singleton instance.
     *
     * @return singleton instance
     */
    public static BasicLimiter getInstance()
    {
        if (instance == null)
        {
            synchronized (BasicLimiter.class) {
                if (instance == null) {
                    instance = new BasicLimiter();
                }
            }
        }
        return instance;
    }

    /**
     * Clears expired logs from event logbooks
     */
    public void purgeEventLogbooks()
    {
        // TODO Needs to be implemented
    }

    private static class EventLogbook
    {
        private final String eventKey;
        private final List<Long> eventTimestamps;
        private final int limit;
        private final long milllisInterval;
        private AtomicLong unhandledLogs;
        private boolean atLeastOnceHandled;

        public EventLogbook(String eventKey, int limit, boolean registered, long interval, TimeUnit timeUnit) {
            this.eventKey = eventKey;
            this.eventTimestamps = new LinkedList<Long>();
            this.limit = limit;
            this.milllisInterval = TimeUnit.MILLISECONDS.convert(interval, timeUnit);
            unhandledLogs = new AtomicLong(0L);
            atLeastOnceHandled = false;
        }

        public AtomicLong getUnhandledLogs() {
            return unhandledLogs;
        }

        public long getShortTermEventLogsLeftInInterval(long timestamp)
        {
            long shortTermEventLogsCount = getShortTermEventLogsCount(timestamp);
            return limit-shortTermEventLogsCount;
        }

        public long getShortTermEventLogsCount(long timestamp)
        {
            synchronized (this)
            {
                long unhandled = unhandledLogs.longValue();
                if (unhandled>0) {
                    if (atLeastOnceHandled) {
                        return eventTimestamps.size() + unhandled;
                    }
                    else
                    {
                        long oldestTimestamp = timestamp - milllisInterval;
                        synchronized (eventTimestamps) {
                            Iterator<Long> logsIterator = eventTimestamps.iterator();
                            int count = 0;
                            while (logsIterator.hasNext()) {
                                long currentTimestamp = logsIterator.next();
                                if (currentTimestamp >= oldestTimestamp) {
                                    if (oldestTimestamp > currentTimestamp) {
                                        oldestTimestamp = currentTimestamp;
                                    }
                                    count++;
                                }
                            }
                            return count + unhandled;
                        }
                    }
                }
                else
                {
                    return 0;
                }
            }
        }
    }
}
