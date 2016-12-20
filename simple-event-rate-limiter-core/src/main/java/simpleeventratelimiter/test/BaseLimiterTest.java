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
package simpleeventratelimiter.test;

import junit.framework.TestCase;
import simpleeventratelimiter.Limiter;
import simpleeventratelimiter.exception.EventLimitException;

import java.util.concurrent.TimeUnit;

/**
 * Created by klemen on 13.12.2016.
 */
public abstract class BaseLimiterTest extends TestCase {

    protected void testEventLimitException(Limiter limiter) throws Exception
    {
        for (int eventCnt = 1; eventCnt<=11;eventCnt++)
        {
            System.out.println("Event: " + eventCnt);
            if (eventCnt>10)
            {
                long startMillis = System.currentTimeMillis();
                try
                {
                    limiter.logEvent("test event 1", 10, 15, TimeUnit.SECONDS);
                    TestCase.fail("EventLimitException should be thrown");
                }
                catch (EventLimitException e)
                {
                    // all good
                }
                System.out.println("Event logged in millis: "+ (System.currentTimeMillis()-startMillis));
            }
            else
            {
                long startMillis = System.currentTimeMillis();
                limiter.logEvent("test event 1", 10, 15, TimeUnit.SECONDS);
                System.out.println("Event logged in millis: "+ (System.currentTimeMillis()-startMillis));
            }
        }
        System.out.println("Waiting ...");
        Thread.sleep(16000);
        for (int eventCnt = 1; eventCnt<=11;eventCnt++)
        {
            System.out.println("Event: " + eventCnt);
            if (eventCnt>10)
            {
                long startMillis = System.currentTimeMillis();
                try
                {
                    limiter.logEvent("test event 1", 10, 15, TimeUnit.SECONDS);
                    TestCase.fail("EventLimitException should be thrown");
                }
                catch (EventLimitException e)
                {
                    // all good
                }
                System.out.println("Event logged in millis: "+ (System.currentTimeMillis()-startMillis));
            }
            else
            {
                long startMillis = System.currentTimeMillis();
                limiter.logEvent("test event 1", 10, 15, TimeUnit.SECONDS);
                System.out.println("Event logged in millis: "+ (System.currentTimeMillis()-startMillis));
            }
        }
    }

    protected void testEventLimitExceptionWithDelays(Limiter limiter) throws Exception
    {
        for (int eventCnt = 1; eventCnt<=11;eventCnt++)
        {
            System.out.println("Event: " + eventCnt);
            if (eventCnt>10)
            {
                long startMillis = System.currentTimeMillis();
                try
                {
                    limiter.logEvent("test event 2", 10, 15, TimeUnit.SECONDS);
                    TestCase.fail("EventLimitException should be thrown");
                }
                catch (EventLimitException e)
                {
                    // all good
                }
                System.out.println("Event logged in millis: "+ (System.currentTimeMillis()-startMillis));
            }
            else
            {
                long startMillis = System.currentTimeMillis();
                limiter.logEvent("test event 2", 10, 15, TimeUnit.SECONDS);
                System.out.println("Event logged in millis: "+ (System.currentTimeMillis()-startMillis));
            }
            Thread.sleep(1000);
        }
        Thread.sleep(16000);
        for (int eventCnt = 1; eventCnt<=11;eventCnt++)
        {
            System.out.println("Event: " + eventCnt);
            if (eventCnt>10)
            {
                long startMillis = System.currentTimeMillis();
                try
                {
                    limiter.logEvent("test event 2", 10, 15, TimeUnit.SECONDS);
                    TestCase.fail("EventLimitException should be thrown");
                }
                catch (EventLimitException e)
                {
                    // all good
                }
                System.out.println("Event logged in millis: "+ (System.currentTimeMillis()-startMillis));
            }
            else
            {
                long startMillis = System.currentTimeMillis();
                limiter.logEvent("test event 2", 10, 15, TimeUnit.SECONDS);
                System.out.println("Event logged in millis: "+ (System.currentTimeMillis()-startMillis));
            }
            Thread.sleep(1000);
        }
    }
}
