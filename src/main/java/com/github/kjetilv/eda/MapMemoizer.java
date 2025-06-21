package com.github.kjetilv.eda;

import java.util.Map;

/**
 * The Map memoizer! Maps will be stored in canonical form, avoiding memory wasted on identical trees.
 *
 * @param <I> Id type, used to identify maps
 * @param <K> Key type, used as keys in stored maps
 */
public interface MapMemoizer<I, K> {

    /**
     * @param identifier Identifier
     * @return Stored map
     * @throws IllegalArgumentException If the identifier was unknown
     */
    Map<K, ?> get(I identifier);

    /**
     * Store one map.
     *
     * @param identifier Identifier
     * @param value      Map
     * @throws IllegalStateException If this instance is {@link #complete()}
     */
    void put(I identifier, Map<?, ?> value);

    /**
     * @return Number of maps stored
     */
    int size();

    /**
     * Signals the end of {@link #put(Object, Map) putting} activities.  This instance will throw out
     * working data and prohibit further {@link #put(Object, Map) puts}.
     *
     * @return Access to stored maps
     */
    Access<I, K> complete();

    interface Access<I, K> {

        /**
         * @param identifier Identifier
         * @return Stored map
         * @throws IllegalArgumentException If the identifier was unknown
         */
        Map<K, ?> get(I identifier);

        /**
         * @return Number of maps stored
         */
        int size();
    }
}
