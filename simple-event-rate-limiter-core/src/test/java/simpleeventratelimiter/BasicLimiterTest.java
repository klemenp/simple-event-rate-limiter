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

import junit.framework.TestCase;
import org.junit.Test;
import simpleeventratelimiter.exception.EventLimitException;

import java.util.concurrent.TimeUnit;

/**
 * Created by klemen on 13.12.2016.
 */
public class BasicLimiterTest extends TestCase {

    @Test
    public void testEventLimitException() throws Exception
    {
        Limiter limiter = BasicLimiter.getInstance();
        for (int eventCnt = 1; eventCnt<=20;eventCnt++)
        {
            if (eventCnt>10)
            {
                try
                {
                    limiter.logEvent("test event", 10, 1, TimeUnit.MINUTES);
                    fail("EventLimitException should be thrown");
                }
                catch (EventLimitException e)
                {
                    // all good
                }
            }
            else
            {
                limiter.logEvent("test event", 10, 1, TimeUnit.MINUTES);
            }
        }


    }
}
