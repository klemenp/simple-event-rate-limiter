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
package simpleeventratelimiter.couchbase;

import com.couchbase.client.java.document.JsonLongDocument;
import com.couchbase.client.java.document.SerializableDocument;
import com.couchbase.client.java.error.DocumentAlreadyExistsException;
import com.couchbase.client.java.error.DocumentDoesNotExistException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simpleeventratelimiter.Limiter;
import simpleeventratelimiter.exception.EventLimitException;
import simpleeventratelimiter.exception.EventRegisteredException;
import simpleeventratelimiter.exception.NoEventRegisteredException;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Couchbase based rate limiter
 *
 * Created by Klemen Polanec on 19.12.2016.
 */
public class CouchbaseLimiter implements Limiter {
    private static final Logger log = LoggerFactory.getLogger(CouchbaseLimiter.class);

    private static CouchbaseLimiter instance = null;

    private CouchbaseClientManager couchbaseClientManager = null;


    private CouchbaseLimiter()
    {
        super();
        couchbaseClientManager = CouchbaseClientManagerImpl.getInstance();
        couchbaseClientManager.initializeBucket();
    }

    public static CouchbaseLimiter getInstance()
    {
        if (instance == null)
        {
            synchronized (CouchbaseLimiter.class) {
                if (instance == null) {
                    instance = new CouchbaseLimiter();
                }
            }
        }
        return instance;
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
        try {
            EventLogbook eventLogbook = getEventLogbook(eventKey);

            if (eventLogbook==null)
            {
                throw new NoEventRegisteredException("No event registered for event key " + eventKey);
            }
            long shortTermCounter;
            try {
                shortTermCounter = couchbaseClientManager.getClient().counter(createShortTermCounterKey(eventKey), 1).content().longValue();
                if (shortTermCounter==1)
                {
                    eventLogbook.atLeastOnceHandled=false;
                }
            }
            catch (DocumentDoesNotExistException e)
            {
                throw new NoEventRegisteredException("No event registered for event key " + eventKey);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
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
                    try {
                        replaceNextAllowedTimestamp(eventKey, oldestTimestamp + eventLogbook.milllisInterval);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                else
                {
                    try {
                        couchbaseClientManager.getClient().remove(createNextAllowedTimestampKey(eventKey));
                    } catch (Exception e) {
                        throw new RuntimeException(e.getMessage(), e);
                    }
                }
                try {
                    couchbaseClientManager.getClient().counter(createShortTermCounterKey(eventKey), -1);
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
                eventLogbook.atLeastOnceHandled|=true;
            });
            thread.start();

            long shortTermRequetsLeft = getShortTermEventLogsLeftInInterval(eventLogbook, shortTermCounter, logTimestamp);
            if (shortTermRequetsLeft < 0) {
                throw new EventLimitException("Limit reached for event key " + eventKey + ". Short term counter at limit");
            }


            Long nextAllowedTimestamp = null;
            try {
                nextAllowedTimestamp = getNextAllowedTimestamp(eventKey);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            if (nextAllowedTimestamp==null)
            {
                return;
            }
            else if (nextAllowedTimestamp.longValue()>logTimestamp)
            {
                throw new EventLimitException("Limit reached for event key " + eventKey + ". Next event allowed on " + nextAllowedTimestamp.longValue());
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private EventLogbook getEventLogbook(String eventKey) throws Exception {
        SerializableDocument document = couchbaseClientManager.getClient().get(createEventLogbookKey(eventKey), SerializableDocument.class);
        if (document!=null) {
            return (EventLogbook)document.content();
        }
        else
        {
            return null;
        }
    }

    private void replaceEventLogbook(String eventKey, EventLogbook eventLogbook) throws Exception {
        couchbaseClientManager.getClient().replace(SerializableDocument.create(createEventLogbookKey(eventKey), eventLogbook));
    }

    private Long getNextAllowedTimestamp(String eventKey) throws Exception {
        JsonLongDocument document = couchbaseClientManager.getClient().get(createNextAllowedTimestampKey(eventKey), JsonLongDocument.class);
        if (document!=null) {
            return (Long)document.content();
        }
        else
        {
            return null;
        }
    }

    private void replaceNextAllowedTimestamp(String eventKey, Long nextAllowedTimestamp) throws Exception {
        couchbaseClientManager.getClient().replace(JsonLongDocument.create(createEventLogbookKey(eventKey), nextAllowedTimestamp));
    }

    private String createShortTermCounterKey(String eventKey)
    {
        return "L_STC_" + eventKey;
    }

    private String createEventLogbookKey(String eventKey)
    {
        return "L_ELB_" + eventKey;
    }

    private String createNextAllowedTimestampKey(String eventKey)
    {
        return "L_NAT_" + eventKey;
    }

    private boolean isEventRegistered(String eventKey)
    {
        // TODO
        return false;
//        return eventLogbooks.containsKey(eventKey);
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
            CouchbaseLimiter.EventLogbook eventLogbook = new CouchbaseLimiter.EventLogbook(eventKey, limit, true, interval, unit);
//            synchronized (eventLogbook) {
            try {
                couchbaseClientManager.getClient().insert(SerializableDocument.create(createEventLogbookKey(eventKey), eventLogbook));
            } catch (DocumentAlreadyExistsException e) {
                log.debug("Must have been just regiostered: " + e.getMessage());
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            try {
                    logEvent(eventKey);
                }
                catch (Exception e)
                {
                    log.warn(e.getMessage());
                }
//            }
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


    }

    private long getShortTermEventLogsLeftInInterval(EventLogbook eventLogbook, long unhandledLogs, long timestamp)
    {
        long shortTermEventLogsCount = getShortTermEventLogsCount(eventLogbook, unhandledLogs, timestamp);
        return eventLogbook.limit-shortTermEventLogsCount;
    }

    private long getShortTermEventLogsCount(EventLogbook eventLogbook, long unhandledLogs, long timestamp)
    {
//        synchronized (this)
//        {
            if (unhandledLogs>0) {
                if (eventLogbook.atLeastOnceHandled) {
                    return eventLogbook.eventTimestamps.size() + unhandledLogs;

                }
                else
                {
                    long oldestTimestamp = timestamp - eventLogbook.milllisInterval;
//                    synchronized (eventLogbook.eventTimestamps) {
                        Iterator<Long> logsIterator = eventLogbook.eventTimestamps.iterator();
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
                        return count + unhandledLogs;
//                    }
                }
            }
            else
            {
                return 0;
            }
//        }
    }

    /**
     * Clears expired logs from event logbooks
     */
    @Deprecated
    public void purgeEventLogbooks()
    {
        // TODO Needs to be implemented
    }

    private static class EventLogbook implements Serializable
    {
        private final String eventKey;
        private final List<Long> eventTimestamps;
        private final int limit;
        private final long milllisInterval;
        private boolean atLeastOnceHandled;

        public EventLogbook(String eventKey, int limit, boolean registered, long interval, TimeUnit timeUnit) {
            this.eventKey = eventKey;
            this.eventTimestamps = new LinkedList<Long>();
            this.limit = limit;
            this.milllisInterval = TimeUnit.MILLISECONDS.convert(interval, timeUnit);
            //unhandledLogs = new AtomicLong(0L);
            atLeastOnceHandled = false;
        }
    }
}
