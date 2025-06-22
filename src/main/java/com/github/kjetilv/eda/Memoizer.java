package com.github.kjetilv.eda;

import java.util.Map;

/**
 * The memoizer! Maps will be stored in canonical form, avoiding memory wasted on identical trees.
 * Extends {@link Memoized} to provide lookup of stored maps.
 * <p>
 * In cases where the set of maps is known and finite, the {@link #complete()} method can be invoked
 * after all data are inserted. This allows further savings by throwing away internal book-keeping
 * state and locking down the memoizer for further puts.
 * <p>
 *
 * @param <I> Id type, used to identify maps
 * @param <K> Key type, used as keys in stored maps
 */
public interface Memoizer<I, K> extends Memoized<I, K> {

    /**
     * Store one map.
     *
     * @param identifier Identifier
     * @param value      Map
     * @throws IllegalStateException If this instance is {@link #complete()}
     */
    void put(I identifier, Map<?, ?> value);

    /**
     * Signals the end of {@link #put(Object, Map) putting} activities.  Locks down this instance
     * for further calls to {@link #put}, allowing it to free up memory used for working data.
     *
     * @return This instance
     */
    Memoized<I, K> complete();
}
