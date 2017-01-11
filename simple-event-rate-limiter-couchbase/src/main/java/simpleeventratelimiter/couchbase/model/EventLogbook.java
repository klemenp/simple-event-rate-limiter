package simpleeventratelimiter.couchbase.model;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * Created by klemen on 11.1.2017.
 */
public class EventLogbook implements Serializable {
    public String type = "EventLogbook";
    public String eventKey;
    public int limit;
    public long milllisInterval;
    public boolean atLeastOnceHandled;

    public EventLogbook(String eventKey, int limit, long interval, TimeUnit timeUnit) {
        this.eventKey = eventKey;
        this.limit = limit;
        this.milllisInterval = TimeUnit.MILLISECONDS.convert(interval, timeUnit);
        atLeastOnceHandled = false;
    }
}
