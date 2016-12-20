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
package simpleeventratelimiter.guava;

import com.google.common.util.concurrent.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simpleeventratelimiter.Limiter;
import simpleeventratelimiter.exception.EventLimitException;
import simpleeventratelimiter.exception.EventRegisteredException;
import simpleeventratelimiter.exception.NoEventRegisteredException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Guava RateLimiter based rate limiter
 *
 * Created by Klemen Polanec on 19.12.2016.
 */
public class GuavaLimiter implements Limiter {
    private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);

    private static GuavaLimiter instance = null;
    private Map<String,RateLimiter> limiters;


    private GuavaLimiter()
    {
        limiters = new ConcurrentHashMap<String, RateLimiter>();
    }

    public static GuavaLimiter getInstance()
    {
        if (instance == null)
        {
            synchronized (GuavaLimiter.class) {
                if (instance == null) {
                    instance = new GuavaLimiter();
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
        RateLimiter limiter = limiters.get(eventKey);
        if (limiter==null)
        {
            throw new NoEventRegisteredException("No event registered for event key " + eventKey);
        }

        if (!limiter.tryAcquire())
        {
            throw new EventLimitException("Limit reached for event key " + eventKey);
        }
    }

    private boolean isEventRegistered(String eventKey)
    {
        return limiters.containsKey(eventKey);
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
        if (!unit.equals(TimeUnit.SECONDS))
        {
            throw new IllegalArgumentException("Only TimeUnit.SECONDS unit is alowed for guava based implementation");
        }
        if (!isEventRegistered(eventKey))
        {
            RateLimiter limiter = RateLimiter.create(limit, 1, TimeUnit.SECONDS);
            synchronized (limiters) {
                limiters.putIfAbsent(eventKey, limiter);
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
        if (!unit.equals(TimeUnit.SECONDS))
        {
            throw new IllegalArgumentException("Only TimeUnit.SECONDS unit is alowed for guava based implementation");
        }

        RateLimiter limiter = limiters.get(eventKey);
        if (limiter!=null)
        {
            throw new EventRegisteredException("Event is already registered for key " + eventKey);
        }
        limiter = RateLimiter.create(limit, 1, TimeUnit.SECONDS);
        limiters.putIfAbsent(eventKey, limiter);

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
