package com.github.kjetilv.eda;

import java.util.Map;

/**
 * Behold the memoizer! Maps will be stored in canonical form, avoiding memory wasted on identical
 * trees.
 * <p>
 * In cases where the set of maps is known (and finite), the {@link #complete() complete} method can
 * be invoked after all data are inserted. This allows further savings by throwing away internal
 * book-keeping state and locking down the memoizer for further puts.
 * <p>
 * Extends {@link MemoizedMaps} to enable lookup of stored maps even before {@link #complete() completion}.
 *
 * @param <I> Id type, used to identify maps
 * @param <K> Key type, used as keys in stored maps
 */
@SuppressWarnings("unused")
public interface MapsMemoizer<I, K> extends MemoizedMaps<I, K> {

    /**
     * Store one map
     *
     * @param identifier Identifier
     * @param value      Map
     * @throws IllegalStateException    If this instance is {@link #complete()}
     * @throws IllegalArgumentException If the identifier is already stored
     */
    void put(I identifier, Map<?, ?> value);

    /**
     * Store one map, unless it's already stored
     *
     * @param identifier Identifier
     * @param value      Map
     * @return true iff the map was added.  If false, the memoizer was unchanged
     * @throws IllegalStateException    If this instance is {@link #complete()}
     */
    boolean putIfAbsent(I identifier, Map<?, ?> value);

    /**
     * Signals the end of {@link #put(Object, Map) putting} activities.  Locks down this instance
     * for further calls to {@link #put}, allowing it to free up memory used for working data.
     *
     * @return This instance, typed to {@link MemoizedMaps} in order to discourage futher putting
     */
    MemoizedMaps<I, K> complete();
}
