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

import com.couchbase.client.deps.com.fasterxml.jackson.databind.ObjectMapper;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.JsonLongDocument;
import com.couchbase.client.java.document.SerializableDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.DocumentAlreadyExistsException;
import com.couchbase.client.java.error.DocumentDoesNotExistException;
import com.couchbase.client.java.view.Stale;
import com.couchbase.client.java.view.ViewQuery;
import com.couchbase.client.java.view.ViewResult;
import com.couchbase.client.java.view.ViewRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simpleeventratelimiter.Limiter;
import simpleeventratelimiter.exception.EventLimitException;
import simpleeventratelimiter.exception.EventRegisteredException;
import simpleeventratelimiter.exception.NoEventRegisteredException;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
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

    private ObjectMapper jsonMapper;

    private CouchbaseLimiter()
    {
        super();
        couchbaseClientManager = CouchbaseClientManagerImpl.getInstance();
        couchbaseClientManager.initializeBucket();
        jsonMapper = new ObjectMapper();
    }

    public CouchbaseClientManager getCouchbaseClientManager()
    {
        return couchbaseClientManager;
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

    public long counter(String eventKey, int delta, int initial) throws Exception
    {
        String id = createShortTermCounterKey(eventKey);
//        System.out.println("counter id: " + id);
        long cnt =  couchbaseClientManager.getClient().counter(id, delta, initial).content().longValue();
//        System.out.println("counter id: " + id + " new val:" + cnt);
        return cnt;
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
//        try {
        EventLogbook eventLogbook = getEventLogbook(eventKey);

        if (eventLogbook==null)
            {
                throw new NoEventRegisteredException("No event registered for event key " + eventKey);
            }
            long shortTermCounter;
            try {
//                synchronized (this) {
//                    Thread.sleep(100);
                    shortTermCounter = counter(eventKey, 1, 1);
//                }
                if (shortTermCounter==1)
                {
                    eventLogbook.atLeastOnceHandled=false;
                }
            }
            catch (DocumentDoesNotExistException e)
            {
                throw new NoEventRegisteredException("No event registered for event key " + eventKey);
            } catch (Exception e) {
                System.out.println("ERROR: " + e.getMessage());
                throw new RuntimeException(e.getMessage(), e);
            }

            System.out.println("DEBUG shortTermCounter: " + shortTermCounter + "  " + eventKey);
            List<EventLogTimestamp> eventTimestamps = getTimestamps(eventKey);
            Thread thread = new Thread(()-> {
                try {

                    long oldestTimestamp = logTimestamp - eventLogbook.milllisInterval;
                    // Clean up old logs and find new nextAllowedTimestamp;


                    int count = 0;
                    for (EventLogTimestamp eventLogTimestamp : eventTimestamps)
                    {
                        long currentTimestamp = eventLogTimestamp.eventTimestamp;
                        if (oldestTimestamp > currentTimestamp) {
                            oldestTimestamp = currentTimestamp;
                        }
                        count++;

                    }

                    if (count >= eventLogbook.limit) {
                        try {
                            replaceNextAllowedTimestamp(eventKey, oldestTimestamp + eventLogbook.milllisInterval);
                        } catch (Exception e) {
                            System.out.println("ERROR: " + e.getMessage());
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            couchbaseClientManager.getClient().remove(createNextAllowedTimestampKey(eventKey));
                        } catch (DocumentDoesNotExistException e) {
                            log.debug("Looks like already removed: " + e.getMessage());
                        } catch (Exception e) {
                            System.out.println("ERROR: " + e.getMessage());
                            e.printStackTrace();
                            throw new RuntimeException(e.getMessage(), e);
                        }
                    }

                    eventLogbook.atLeastOnceHandled |= true;

                    try {
                        couchbaseClientManager.getClient().replace(SerializableDocument.create(createEventLogbookKey(eventKey), eventLogbook));
                        Thread.sleep(100);
                    } catch (Exception e) {
                        System.out.println("ERROR: " + e.getMessage());
                        e.printStackTrace();
                    }

                    insertEventTimestamp(eventKey,logTimestamp);
                    long newShortTermCounter = counter(eventKey, -1, 0);
                    System.out.println("DEBUG newShortTermCounter: " + newShortTermCounter);
                }
                catch (Exception e)
                {
                    System.out.println("ERROR: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            thread.start();

            long shortTermRequestsLeft = getShortTermEventLogsLeftInInterval(eventLogbook, eventTimestamps, shortTermCounter, logTimestamp);
            System.out.println(("DEBUG shortTermRequetsLeft: " + shortTermRequestsLeft));
            if (shortTermRequestsLeft < 0) {
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
//        } catch (Exception e) {
//            throw new RuntimeException(e.getMessage(), e);
//        }
    }

    private List<EventLogTimestamp> getTimestamps(String eventKey)
    {
        ViewResult result = query(VIEW_EVENT_LOG_TIMESTAMP, Stale.FALSE, null, null, eventKey, null, null, false);
        return
    }

    private ViewResult query(String viewName, Stale stale, String key, JsonArray keys, String startKey, String endKey, Integer groupLevel, boolean reduce) {
        Bucket client = null;
        try {
            client = couchbaseClientManager.getClient();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        ViewQuery query = ViewQuery.from(couchbaseClientManager.getBucketName(), viewName);
        query.stale(stale);
        query.reduce(reduce);
        if (key != null) {
            query.key(key);
        }
        if (keys != null) {
            query.keys(keys);
        }
        if (groupLevel != null) {
            query.groupLevel(groupLevel);
        }
        if(startKey != null){
            query.startKey(startKey);
        }
        if(endKey != null){
            query.endKey(endKey);
        }
        log.trace("Query: " + query.toString());
        ViewResult response = client.query(query);
        return response;
    }

    private <T> List<T> mapResults (ViewResult result, Class<T> clazz) {
        List<T> results = new ArrayList<>();

        Iterator<ViewRow> iterator = result.iterator();

        while (iterator.hasNext()) {
            ViewRow viewRow = iterator.next();
            String value = ((JsonObject) viewRow.value()).toString();
            try {
                results.add(jsonMapper.readValue(value, clazz));
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        return results;
    }

    private EventLogbook getEventLogbook(String eventKey) {
        SerializableDocument document = null;
        try {
            document = couchbaseClientManager.getClient().get(createEventLogbookKey(eventKey), SerializableDocument.class);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (document!=null) {
            return (EventLogbook)document.content();
        }
        else
        {
            return null;
        }
    }

    private void replaceEventLogbook(String eventKey, EventLogbook eventLogbook) throws Exception {
        couchbaseClientManager.getClient().replace(JsonDocument.create(createEventLogbookKey(eventKey), JsonObject.fromJson(jsonMapper.writeValueAsString(eventLogbook))));
    }

    private void insertEventTimestamp(String eventKey, Long timestamp) {
        EventLogTimestamp eventLogTimestamp = new EventLogTimestamp();
        eventLogTimestamp.eventKey = eventKey;
        eventLogTimestamp.eventTimestamp = timestamp;

        try
        {
            couchbaseClientManager.getClient().insert(JsonDocument.create(createEventLogTimestampKey(eventKey),  JsonObject.fromJson(jsonMapper.writeValueAsString(eventLogTimestamp))));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private Long getNextAllowedTimestamp(String eventKey) throws Exception {
        try {
            JsonLongDocument document = couchbaseClientManager.getClient().get(createNextAllowedTimestampKey(eventKey), JsonLongDocument.class);
            if (document != null) {
                return (Long) document.content();
            }
        }
        catch (DocumentDoesNotExistException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    private void replaceNextAllowedTimestamp(String eventKey, Long nextAllowedTimestamp) throws Exception {
        try {
            couchbaseClientManager.getClient().upsert(JsonLongDocument.create(createNextAllowedTimestampKey(eventKey), nextAllowedTimestamp));
        }
        catch (Exception e)
        {
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static String createShortTermCounterKey(String eventKey)
    {
        return "L_STC_" + eventKey;
    }

    public static String createEventLogbookKey(String eventKey)
    {
        return "L_ELB_" + eventKey;
    }

    public static String createEventLogTimestampKey(String eventKey)
    {
        return "L_ELT_" + eventKey + "_" + UUID.randomUUID().toString();
    }

    public static String createNextAllowedTimestampKey(String eventKey)
    {
        return "L_NAT_" + eventKey;
    }

    private boolean isEventRegistered(String eventKey)
    {
        String id = createShortTermCounterKey(eventKey);
//        System.out.println("counter id: " + id);
        try {
            return couchbaseClientManager.getClient().exists(id);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        }
//
//        // TODO
//        return false;
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
        System.out.println("debug logging event 1");
        if (!isEventRegistered(eventKey))
        {
            try {
                registerEvent(eventKey, limit, interval, unit);
            } catch (EventRegisteredException e) {
                log.debug(e.getMessage());
            }
        }

        try {
            System.out.println("debug logging event 2");
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
        System.out.println("debug Registering event " + eventKey);
        CouchbaseLimiter.EventLogbook eventLogbook = new CouchbaseLimiter.EventLogbook(eventKey, limit, interval, unit);
//            synchronized (eventLogbook) {
        try {
            couchbaseClientManager.getClient().insert(SerializableDocument.create(createEventLogbookKey(eventKey), eventLogbook));
            counter(eventKey, 0,0);
        } catch (DocumentAlreadyExistsException e) {
            log.debug("Must have been just registered: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        }
//        try {
//            System.out.println("debug logging event");
//            logEvent(eventKey);
//        }
//        catch (Exception e)
//        {
//            e.printStackTrace();
//            log.warn(e.getMessage());
//        }
//            }

    }

    private long getShortTermEventLogsLeftInInterval(EventLogbook eventLogbook, List<EventLogTimestamp> timestamps, long unhandledLogs, long timestamp)
    {
        long shortTermEventLogsCount = getShortTermEventLogsCount(eventLogbook, timestamps, unhandledLogs, timestamp);
        return eventLogbook.limit-shortTermEventLogsCount;
    }

    private long getShortTermEventLogsCount(EventLogbook eventLogbook, List<EventLogTimestamp> timestamps, long unhandledLogs, long timestamp)
    {
//        synchronized (this)
//        {
            if (unhandledLogs>0) {
                if (eventLogbook.atLeastOnceHandled) {
                    return timestamps.size() + unhandledLogs;
                }
                else
                {
                    long oldestTimestamp = timestamp - eventLogbook.milllisInterval;

                    int count = 0;
                    for (EventLogTimestamp eventLogTimestamp : timestamps)
                    {
                        long currentTimestamp = eventLogTimestamp.eventTimestamp;
                        if (oldestTimestamp > currentTimestamp) {
                            oldestTimestamp = currentTimestamp;
                        }
                        count++;

                    }
                    return count;
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
        String type = "EventLogbook";
        String eventKey;
        int limit;
        long milllisInterval;
        boolean atLeastOnceHandled;

        public EventLogbook(String eventKey, int limit, long interval, TimeUnit timeUnit) {
            this.eventKey = eventKey;
            this.limit = limit;
            this.milllisInterval = TimeUnit.MILLISECONDS.convert(interval, timeUnit);
            atLeastOnceHandled = false;
        }
    }

    private static class EventLogTimestamp implements Serializable
    {
        String type = "EventLogTimestamp";
        String eventKey;
        Long eventTimestamp;


    }
}
