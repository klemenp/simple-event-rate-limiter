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

import org.junit.Test;
import simpleeventratelimiter.test.BaseLimiterTest;

import java.util.concurrent.TimeUnit;

/**
 * Created by klemen on 13.12.2016.
 */
public class BasicLimiterTest extends BaseLimiterTest {
    @Test
    public void testEventLimitException() throws Exception
    {
        Limiter limiter = BasicLimiter.getInstance();
        super.testEventLimitException(limiter, 1000, 10, 1, TimeUnit.SECONDS);
    }

    @Test
    public void testEventLimitExceptionWithDelays() throws Exception
    {
        Limiter limiter = BasicLimiter.getInstance();
        super.testEventLimitExceptionWithDelays(limiter, 1000, 16000, 10, 15, TimeUnit.SECONDS);
    }

    @Test
    public void testEventLimitException2() throws Exception
    {
        Limiter limiter = BasicLimiter.getInstance();
        super.testEventLimitException(limiter, 16000, 10, 15, TimeUnit.SECONDS);
    }

    @Test
    public void testEventLimitExceptionWithDelays2() throws Exception
    {
        Limiter limiter = BasicLimiter.getInstance();
        super.testEventLimitExceptionWithDelays(limiter, 80, 1000, 10, 1, TimeUnit.SECONDS);
    }
}
