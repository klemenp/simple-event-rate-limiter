# Simple Event Rate Limiter

Simple library for event rate limiting.

Usage:

```java
    Limiter limiter = BasicLimiter.getInstance();

    try {
        limiter.logEvent("sample event", 5, 1, TimeUnit.MINUTES);
    } catch (EventLimitException e) {
        System.out.println("Event rate limit reached");
    }

```

Licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).
