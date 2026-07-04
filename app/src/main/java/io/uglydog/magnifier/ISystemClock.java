package io.uglydog.magnifier;

/**
 * Interface to abstract time retrieval for unit testing.
 */
public interface ISystemClock {
    long uptimeMillis();
}
