package com.github.kjetilv.eda.impl;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Stateful interface for building ids.
 *
 * @param <T>
 */
interface HashBuilder<T> extends Consumer<T>, Function<T, HashBuilder<T>>, Supplier<Hash> {

    @Override
    default void accept(T t) {
        hash(t);
    }

    @Override
    default HashBuilder<T> apply(T t) {
        accept(t);
        return this;
    }

    default HashBuilder<T> hash(List<T> ts) {
        for (T t : ts) {
            accept(t);
        }
        return this;
    }

    default HashBuilder<T> hash(Stream<T> ts) {
        ts.forEach(this);
        return this;
    }

    HashBuilder<T> hash(T t);

    /**
     * Get the id, reset the underlying hasher
     *
     * @return Hash
     */
    @Override
    Hash get();

    /**
     * @param transform Transformer for R to T
     * @param <R>       Input type to new hasher
     * @return New hasher that accepts and transforms its input to T
     */
    <R> HashBuilder<R> map(Function<R, T> transform);
}
