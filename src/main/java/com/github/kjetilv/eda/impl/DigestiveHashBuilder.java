package com.github.kjetilv.eda.impl;

import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

final class DigestiveHashBuilder<T> implements HashBuilder<T> {

    private final ByteDigest byteDigest;

    private final Function<T, Stream<byte[]>> toBytes;

    DigestiveHashBuilder(ByteDigest byteDigest) {
        this(byteDigest, null);
    }

    @SuppressWarnings("unchecked")
    private DigestiveHashBuilder(ByteDigest byteDigest, Function<T, Stream<byte[]>> toBytes) {
        this.byteDigest = Objects.requireNonNull(byteDigest, "byteDigest");
        this.toBytes = toBytes == null ? (Function<T, Stream<byte[]>>) IDENTITY : toBytes;
    }

    @Override
    public HashBuilder<T> hash(T t) {
        toBytes.apply(t).forEach(byteDigest);
        return this;
    }

    @Override
    public Hash get() {
        return byteDigest.get();
    }

    @Override
    public <R> HashBuilder<R> map(Function<R, T> transform) {
        return new DigestiveHashBuilder<>(byteDigest, transform.andThen(toBytes));
    }

    private static final Function<byte[], Stream<byte[]>> IDENTITY = Stream::of;

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + byteDigest + " <- " + toBytes + "]";
    }
}
