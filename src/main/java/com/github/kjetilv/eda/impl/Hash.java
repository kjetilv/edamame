package com.github.kjetilv.eda.impl;

import java.util.Arrays;

/**
 * A 256-bit hash, exposed as four longs.
 */
@FunctionalInterface
public interface Hash extends Comparable<Hash> {

    String LPAR = "⟨";

    String RPAR = "⟩";

    int DIGEST_LEN = 22;

    static Hash of(long l0, long l1) {
        return new DefaultHash(l0, l1);
    }

    /**
     * @return Unique 43-char string representation
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

    default String toShortString() {
        return LPAR + digest().substring(0, 6) + RPAR;
    }

    @Override
    default int compareTo(Hash o) {
        return Arrays.compare(ls(), o.ls());
    }

    default byte byteAt(int i) {
        return bytes()[i];
    }

    /**
     * The longs,
     *
     * @return Longs
     */
    long[] ls();
}
