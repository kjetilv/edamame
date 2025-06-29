package com.github.kjetilv.eda;

import java.util.Map;

/**
 * Provides access to memoized maps after {@link MapsMemoizer#complete() completion}.
 *
 * @param <I> Id type, used to identify maps
 * @param <K> Key type, used as keys in stored maps
 */

public interface MemoizedMaps<I, K> {

    /**
     * @return The number of maps memoized
     */
    int size();

    /**
     * @param identifier Identifier
     * @return Stored map, or null of the identifier was unknown
     */
    Map<K, ?> get(I identifier);
}
