package com.github.kjetilv.eda.impl;

/**
 * A 128-bit hash, exposed as two longs.
 */
public record Hash(long l0, long l1) {

    static Hash of(long[] ls) {
        return of(ls[0], ls[1]);
    }

    static Hash of(byte[] bytes) {
        long[] ls = new long[2];
        for (int i = 0; i < 8; i++) {
            ls[0] <<= 8;
            ls[0] |= bytes[i] & 0xFF;
            ls[1] <<= 8;
            ls[1] |= bytes[i + 8] & 0xFF;
        }
        return of(ls);
    }

    public static Hash of(long l0, long l1) {
        return new Hash(l0, l1);
    }

    /**
     * @return Unique string representation
     */
    String digest() {
        return Hashes.digest(this);
    }

    /**
     * @return Byte representation of the id
     */
    byte[] bytes() {
        return Hashes.toBytes(new long[] {l0, l1});
    }

    static final Hash NULL = new Hash(0L, 0L);

    private static final String LPAR = "⟨";

    private static final String RPAR = "⟩";

    @SuppressWarnings("NullableProblems")
    @Override
    public String toString() {
        return LPAR + digest().substring(0, 8) + RPAR;
    }
}
