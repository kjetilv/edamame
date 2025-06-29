package com.github.kjetilv.eda.impl;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Stateful interface for building hashes.  Bytes may be {@link #hash(Object) added} to the hash, before the value is
 * {@link #get() fetched} at the end.
 *
 * @param <T>
 */
interface HashBuilder<T> extends Consumer<T>, Function<T, HashBuilder<T>>, Supplier<Hash> {

    @Override
    default void accept(T t) {
        if (t != null) {
            hash(t);
        }
    }

    @Override
    default HashBuilder<T> apply(T t) {
        accept(t);
        return this;
    }

    HashBuilder<T> hash(T t);

    /**
     * Get the hash and reset the underlying hasher
     *
     * @return Hash
     */
    @Override
    Hash get();

    /**
     * Return a hasher that still updates this hash builder, but accepts {@link R} data and transforms its input
     * to {@link T}
     *
     * @param transform Transformer for R to T
     * @param <R>       Input type to new hasher
     * @return New hasher that updates this hasher with {@link T} instances
     */
    <R> HashBuilder<R> map(Function<R, T> transform);
}
