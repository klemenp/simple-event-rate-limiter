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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simpleeventratelimiter.Limiter;
import simpleeventratelimiter.exception.EventLimitException;
import simpleeventratelimiter.exception.EventRegisteredException;
import simpleeventratelimiter.exception.NoEventRegisteredException;

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
//        long logTimestamp = System.currentTimeMillis();
//        BasicLimiter.EventLogbook eventLogbook = getEventLogbook(eventKey);
//        if (eventLogbook==null)
//        {
//            throw new NoEventRegisteredException("No event registered for event key " + eventKey);
//        }
//        if (eventLogbook.getUnhandledLogs().incrementAndGet()==1)
//        {
//            eventLogbook.atLeastOnceHandled=false;
//        }
//
//        Thread thread = new Thread(()-> {
//            eventLogbook.eventTimestamps.add(logTimestamp);
//            long oldestTimestamp = logTimestamp - eventLogbook.milllisInterval;
//            // Clean up old logs and find new nextAllowedTimestamp;
//
//            int count = 0;
//            synchronized (eventLogbook.eventTimestamps) {
//                Iterator<Long> logsIterator = eventLogbook.eventTimestamps.iterator();
//                while (logsIterator.hasNext()) {
//                    long currentTimestamp = logsIterator.next();
//                    if (currentTimestamp < oldestTimestamp) {
//                        logsIterator.remove();
//                    } else {
//                        if (oldestTimestamp > currentTimestamp) {
//                            oldestTimestamp = currentTimestamp;
//                        }
//                        count++;
//                    }
//                }
//            }
//            if (count>=eventLogbook.limit)
//            {
//                AtomicLong nextAllowedTimestampObj =  nextAllowedTimestamps.get(eventKey);
//                if (nextAllowedTimestampObj==null)
//                {
//                    nextAllowedTimestamps.put(eventKey, new AtomicLong(oldestTimestamp + eventLogbook.milllisInterval));
//                }
//                else
//                {
//                    nextAllowedTimestampObj.set(oldestTimestamp + eventLogbook.milllisInterval);
//                }
//            }
//            else
//            {
//                nextAllowedTimestamps.remove(eventKey);
//            }
//            eventLogbook.getUnhandledLogs().decrementAndGet();
//            eventLogbook.atLeastOnceHandled|=true;
//        });
//        thread.start();
//
//        long shortTermRequetsLeft = eventLogbook.getShortTermEventLogsLeftInInterval(logTimestamp);
//        if (shortTermRequetsLeft < 0) {
//            throw new EventLimitException("Limit reached for event key " + eventKey + ". Short term counter at limit");
//        }
//
//
//        AtomicLong nextAllowedTimestamp = nextAllowedTimestamps.get(eventKey);
//        if (nextAllowedTimestamp==null)
//        {
//            return;
//        }
//        else if (nextAllowedTimestamp.longValue()>logTimestamp)
//        {
//            throw new EventLimitException("Limit reached for event key " + eventKey + ". Next event allowed on " + nextAllowedTimestamp.longValue());
//        }
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
//        if (!isEventRegistered(eventKey))
//        {
//            BasicLimiter.EventLogbook eventLogbook = new BasicLimiter.EventLogbook(eventKey, limit, true, interval, unit);
//            synchronized (eventLogbook) {
//                eventLogbooks.putIfAbsent(eventKey, eventLogbook);
//                try {
//                    logEvent(eventKey);
//                }
//                catch (Exception e)
//                {
//                    log.warn(e.getMessage());
//                }
//            }
//        }
//        else {
//            try {
//                logEvent(eventKey);
//            } catch (NoEventRegisteredException e) {
//                throw new RuntimeException(e.getMessage(), e);
//            }
//        }
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

    /**
     * Clears expired logs from event logbooks
     */
    @Deprecated
    public void purgeEventLogbooks()
    {
        // TODO Needs to be implemented
    }


}
