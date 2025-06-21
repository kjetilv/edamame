package com.github.kjetilv.eda.impl;

/**
 * A 128-bit hash, exposed as two longs.
 */
@FunctionalInterface
public interface Hash {

    /**
     * @return Unique string representation
     */
    default String digest() {
        return Hashes.digest(this);
    }

    /**
     * @return Byte representation of the id
     */
    default byte[] bytes() {
        return Hashes.bytes(this);
    }

    /**
     * The longs
     *
     * @return Longs
     */
    long[] ls();
}
