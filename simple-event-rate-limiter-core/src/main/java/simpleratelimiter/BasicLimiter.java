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
package simpleratelimiter;

import simpleratelimiter.exception.EventLimitException;
import simpleratelimiter.exception.EventRegisteredException;
import simpleratelimiter.exception.NoEventRegisteredException;

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
        AtomicLong nextAllowedTimestamp = nextAllowedTimestamps.get(eventKey);
        Thread thread = new Thread(()-> {
            eventLogbook.eventTimestamps.add(logTimestamp);
            long oldestTimestamp = logTimestamp - eventLogbook.milllisInterval;
            // Clean up old logs and find new nextAllowedTimestamp;
            Iterator<Long> logsIterator = eventLogbook.eventTimestamps.iterator();
            int count = 0;
            long oldestValid = logTimestamp;
            while (logsIterator.hasNext())
            {
                long currentTimestamp = logsIterator.next();
                if (currentTimestamp<oldestTimestamp)
                {
                    logsIterator.remove();
                }
                else
                {
                    if (oldestTimestamp>currentTimestamp)
                    {
                        oldestTimestamp = currentTimestamp;
                    }
                    count++;
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
        });
        thread.start();

        if (nextAllowedTimestamp==null)
        {
            return;
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
            eventLogbooks.putIfAbsent(eventKey, eventLogbook);
        }
        try {
            logEvent(eventKey);
        } catch (NoEventRegisteredException e) {
            throw new RuntimeException(e.getMessage(), e);
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

    private static class EventLogbook
    {
        private final String eventKey;
        private final List<Long> eventTimestamps;
        private final int limit;
        private final boolean registered;
        private final long milllisInterval;

        public EventLogbook(String eventKey, int limit, boolean registered, long interval, TimeUnit timeUnit) {
            this.eventKey = eventKey;
            this.eventTimestamps = Collections.synchronizedList(new ArrayList<Long>());
            this.limit = limit;
            this.registered = registered;
            this.milllisInterval = TimeUnit.MILLISECONDS.convert(interval, timeUnit);
        }

        public String getEventKey() {
            return eventKey;
        }

        public List<Long> getEventTimestamps() {
            return eventTimestamps;
        }

        public int getLimit() {
            return limit;
        }

        public boolean isRegistered() {
            return registered;
        }
    }
}
