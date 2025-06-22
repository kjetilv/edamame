package com.github.kjetilv.eda;

import java.util.Map;

/**
 * Provides access to memoized maps.
 *
 * @param <I> Id type, used to identify maps
 * @param <K> Key type, used as keys in stored maps
 */
public interface Memoized<I, K> {

    /**
     * @param identifier Identifier
     * @return Stored map
     * @throws IllegalArgumentException If the identifier was unknown
     */
    Map<K, ?> get(I identifier);
}
