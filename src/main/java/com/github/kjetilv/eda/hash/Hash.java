package com.github.kjetilv.eda.hash;

import java.util.Arrays;

/**
 * A 256-bit hash, exposed as four longs.
 */
@SuppressWarnings({"SpellCheckingInspection", "unused"})
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

    default String toLongString() {
        return LPAR + digest() + RPAR;
    }

    @Override
    default int compareTo(Hash o) {
        return Arrays.compare(ls(), o.ls());
    }

    default boolean isBlank() {
        return this == Hashes.BLANK || l0() == 0 && l1() == 0;
    }

    default String toStringCustom(int length) {
        if (length < 3) {
            throw new IllegalArgumentException(this + ": Invalid length: " + length + ", should be >= 2");
        }
        if (length > DIGEST_LEN + 2) {
            throw new IllegalArgumentException(this + ": Invalid length: " + length + ", should <= " + DIGEST_LEN + 2);
        }
        return LPAR + digest().substring(0, length - 2) + RPAR;
    }

    default long l0() {
        return ls()[0];
    }

    default long l1() {
        return ls()[1];
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
