package com.github.kjetilv.eda;

import java.util.Map;
import java.util.function.Function;

/**
 * @param <I> Id type, used to identify objects
 * @param <K> Key type, used as keys in stored maps
 */
@SuppressWarnings("unused")
public interface MapMemoizer<I, K> {

    default void putAll(Map<I, Map<?, ?>> values) {
        values.forEach(this::put);
    }

    void put(I key, Map<?, ?> value);

    int size();

    int leafCount();

    boolean overflow();

    Access<I, K> complete();

    interface Access<I, K> extends Function<I, Map<K, ?>> {

        default Map<K, ?> get(I i) {
            return apply(i);
        }

        @Override
        Map<K, ?> apply(I i);

        int size();

        boolean overflow();
    }
}
