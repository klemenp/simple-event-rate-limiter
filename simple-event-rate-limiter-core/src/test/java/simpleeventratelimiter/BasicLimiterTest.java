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

/**
 * Created by klemen on 13.12.2016.
 */
public class BasicLimiterTest extends BaseLimiterTest {

    @Test
    public void testEventLimitException() throws Exception
    {
        Limiter limiter = BasicLimiter.getInstance();
        testEventLimitException(limiter);
    }

    @Test
    public void testEventLimitExceptionWithDelays() throws Exception
    {
        Limiter limiter = BasicLimiter.getInstance();
        testEventLimitExceptionWithDelays(limiter);
    }

    @Test
    public void testEventLimit1SecondException() throws Exception
    {
        Limiter limiter = BasicLimiter.getInstance();
        testEventLimit1SecondException(limiter);
    }

    @Test
    public void testEventLimit1SecondExceptionWithDelays() throws Exception
    {
        Limiter limiter = BasicLimiter.getInstance();
        testEventLimit1SecondExceptionWithDelays(limiter);
    }
}
