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

import simpleeventratelimiter.Limiter;
import simpleeventratelimiter.exception.EventLimitException;
import simpleeventratelimiter.exception.EventRegisteredException;
import simpleeventratelimiter.exception.NoEventRegisteredException;

import java.util.concurrent.TimeUnit;

/**
 * Guava RateLimiter base rare limiter
 *
 * Created by Klemen Polanec on 19.12.2016.
 */
public class GuavaLimiter implements Limiter {

    public void logEvent(String eventKey) throws EventLimitException, NoEventRegisteredException {
        // TODO
        throw new IllegalStateException("Not imeplemented yet");
    }

    public void logEvent(String eventKey, int limit, int interval, TimeUnit unit) throws EventLimitException {
        // TODO
        throw new IllegalStateException("Not imeplemented yet");
    }

    public void registerEvent(String eventKey, int limit, long interval, TimeUnit unit) throws EventRegisteredException {
        // TODO
        throw new IllegalStateException("Not imeplemented yet");
    }

    public void purgeEventLogbooks() {
        // TODO
        throw new IllegalStateException("Not imeplemented yet");
    }
}
