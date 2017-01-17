package simpleeventratelimiter.couchbase;

/**
 * Created by klemen on 20.12.2016.
 */
public interface CouchbaseLogbook {
    Long getShorttermCounter(String eventKey);
    void increaseShorttermCounter(String eventKey);
}
