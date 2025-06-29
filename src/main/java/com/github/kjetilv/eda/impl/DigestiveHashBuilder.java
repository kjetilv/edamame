package com.github.kjetilv.eda.impl;

import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Builds hashes using a {@link ByteDigest}
 *
 * @param <T> Input type
 */
final class DigestiveHashBuilder<T> implements HashBuilder<T> {

    /**
     * Starting point, taking byte arrays
     *
     * @param byteDigest Byte digest
     * @return Hash builder
     */
    static HashBuilder<byte[]> create(ByteDigest byteDigest) {
        return new DigestiveHashBuilder<>(byteDigest, Stream::of);
    }

    private final ByteDigest byteDigest;

    private final Function<T, Stream<byte[]>> toBytes;

    private DigestiveHashBuilder(ByteDigest byteDigest, Function<T, Stream<byte[]>> toBytes) {
        this.byteDigest = Objects.requireNonNull(byteDigest, "byteDigest");
        this.toBytes = Objects.requireNonNull(toBytes, "toBytes");
    }

    @Override
    public HashBuilder<T> hash(T t) {
        toBytes.apply(t).forEach(byteDigest);
        return this;
    }

    @Override
    public Hash get() {
        return byteDigest.hash();
    }

    @Override
    public <R> HashBuilder<R> map(Function<R, T> transform) {
        return new DigestiveHashBuilder<>(byteDigest, transform.andThen(toBytes));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + byteDigest + " <- " + toBytes + "]";
    }
}
