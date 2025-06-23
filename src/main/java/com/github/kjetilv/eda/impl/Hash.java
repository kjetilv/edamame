package com.github.kjetilv.eda.impl;

/**
 * A 128-bit hash, exposed as two longs.
 */
public record Hash(long l0, long l1) {

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

    boolean isNull() {
        return this == NULL || this.l0 == 0L && this.l1 == 0L;
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
