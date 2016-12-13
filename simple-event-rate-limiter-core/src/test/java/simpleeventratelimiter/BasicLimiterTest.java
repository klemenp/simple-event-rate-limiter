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
        for (int eventCnt = 0; eventCnt<20;eventCnt++)
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
