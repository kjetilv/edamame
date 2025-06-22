package com.github.kjetilv.eda;

import java.util.Map;

/**
 * The memoizer! Maps will be stored in canonical form, avoiding memory wasted on identical trees.
 * <p>
 * The memoizer extends {@link Memoized} to provide access to maps ahead of {@link #complete() completion}.
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
