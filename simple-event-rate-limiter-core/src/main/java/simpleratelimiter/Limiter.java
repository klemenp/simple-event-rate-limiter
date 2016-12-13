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

import java.util.concurrent.TimeUnit;

/**
 * Created by klemen on 13.12.2016.
 */
public interface Limiter {

    /**
     * Logs event or throws @{@link EventLimitException} if rate limit reached.
     * Throws @{@link NoEventRegisteredException} if there's is no event registered.
     *
     * @param eventKey
     * @throws EventLimitException
     */
    void logEvent(String eventKey) throws EventLimitException, NoEventRegisteredException;

    /**
     * Logs event or throws @{@link EventLimitException} if rate limit reached.
     * Event does not need to be registered beforehand.
     *
     * @param eventKey
     * @param limit
     * @param interval
     * @param unit
     * @throws EventLimitException
     */
    void logEvent(String eventKey, int limit, int interval, TimeUnit unit) throws EventLimitException;

    /**
     * Registers event or throws @{@link EventRegisteredException} if event is already registered.
     *
     * @param eventKey
     * @param limit
     * @param interval
     * @param unit
     * @throws EventLimitException
     */
    void registerEvent(String eventKey, int limit, int interval, TimeUnit unit) throws EventRegisteredException;
}
