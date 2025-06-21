package com.github.kjetilv.eda;

import java.util.Map;

/**
 * The Map memoizer! Maps will be stored in canonical form, avoiding memory wasted on identical instances.
 *
 * @param <I> Id type, used to identify maps
 * @param <K> Key type, used as keys in stored maps
 */
public interface MapMemoizer<I, K>  {

    /**
     * @param identifier Identifier
     * @return Stored map
     * @throws IllegalArgumentException If the identifier was unknown
     */
    Map<K, ?> get(I identifier);

    /**
     * Store one map
     *
     * @param identifier   Identifier
     * @param value Map
     */
    void put(I identifier, Map<?, ?> value);

    /**
     * @return Number of maps stored
     */
    int size();

    /**
     * Complete the building of the maps and and throw away working data.  If this MapMemoizer is
     * subsequently discarded, memory usage and should go down, and we will prohibit further
     * {@link #put(Object, Map) puts}.
     *
     * @return Access to stored maps
     */
    Access<I, K> complete();

    interface Access<I, K>  {

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
