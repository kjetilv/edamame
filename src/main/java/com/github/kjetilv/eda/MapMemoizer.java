package com.github.kjetilv.eda;

import java.util.Map;
import java.util.function.Function;

/**
 * The Map memoizer! Maps will be stored in canonical form, avoiding memory wasted on identical instances.
 * <p>
 * Use {@link MapMemoizers#create(Option...)} and siblings to create instances of this interface.
 * <p>
 * Recommended usage is to {@link #complete()} the memoizer and discard it before usage. This lets the heap shed all
 * working data required to keep the maps canonical, and only the resulting canonical maps will be left. However,
 * a map can also serve as {@link #get(Object) lookup} during the building phase – and beyond, if one never gets
 * to a natural {@link #complete() completion}.
 * <p>
 * Note that  memoizers/accessors works fine even if they report {@link #overflow() overflow}. They will just use
 * slightly more memory because one or more troublesome maps have been stored separately.
 *
 * @param <I> Id type, used to identify maps
 * @param <K> Key type, used as keys in stored maps
 */
@SuppressWarnings("unused")
public interface MapMemoizer<I, K> extends Function<I, Map<K, ?>> {

    /**
     * @param identifier Identifier
     * @return Stored map
     * @throws IllegalArgumentException If the identifier was unknown
     */
    default Map<K, ?> get(I identifier) {
        return apply(identifier);
    }

    /**
     * @param identifier Identifier
     * @return Stored map
     * @throws IllegalArgumentException If the identifier was unknown
     */
    @Override
    Map<K, ?> apply(I identifier);

    /**
     * @param values Maps
     * @see #put(Object, Map)
     */
    default void putAll(Map<I, Map<?, ?>> values) {
        values.forEach(this::put);
    }

    /**
     * Store one map
     *
     * @param key   Identifier
     * @param value Map
     */
    void put(I key, Map<?, ?> value);

    /**
     * @return Number of maps stored
     */
    int size();

    /**
     * @return Number of unique leaves stored
     */
    int leafCount();

    /**
     * @return Number of maps stored separately because of hash collisions
     */
    int overflow();

    /**
     * Complete the building of the maps and and throw away working data.  If this MapMemoizer is
     * subsequently discarded, memory usage and should go down, and we will prohibit further
     * {@link #put(Object, Map) puts}.
     *
     * @return Access to stored maps
     */
    Access<I, K> complete();

    interface Access<I, K> extends Function<I, Map<K, ?>> {

        /**
         * @param identifier Identifier
         * @return Stored map
         * @throws IllegalArgumentException If the identifier was unknown
         */
        default Map<K, ?> get(I identifier) {
            return apply(identifier);
        }

        /**
         * @param identifier Identifier
         * @return Stored map
         * @throws IllegalArgumentException If the identifier was unknown
         */
        @Override
        Map<K, ?> apply(I identifier);

        /**
         * @return Number of maps stored
         */
        int size();

        /**
         * @return Number of maps stored separately because of hash collisions
         */
        int overflow();
    }
}
