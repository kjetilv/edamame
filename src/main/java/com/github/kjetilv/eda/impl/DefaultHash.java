package com.github.kjetilv.eda.impl;

record DefaultHash(long l0, long l1) implements Hash {

    @Override
    public long[] ls() {
        return new long[] {l0, l1};
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public String toString() {
        return LPAR + digest().substring(0, 8) + RPAR;
    }
}
