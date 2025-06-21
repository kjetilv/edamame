package com.github.kjetilv.eda.impl;

import java.util.Arrays;

/**
 * A 256-bit hash, exposed as four longs.
 */
@FunctionalInterface
public interface Hash extends Comparable<Hash> {

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

    @Override
    default int compareTo(Hash o) {
        return Arrays.compare(ls(), o.ls());
    }

    /**
     * The longs
     *
     * @return Longs
     */
    long[] ls();
}
