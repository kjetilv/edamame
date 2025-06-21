package com.github.kjetilv.eda.impl;

import com.github.kjetilv.eda.hash.HashBuilder;

enum Type {

    STRING,
    BIG_DECIMAL,
    BIG_INTEGER,
    UUID,
    NUMBER,
    BOOL,
    OBJECT,
    TEMPORAL,
    DOUBLE,
    FLOAT,
    LONG,
    INT,
    SHORT,
    BYTE,
    OTHER_NUMERIC;

    private final byte[] bytes;

    Type() {
        bytes = new byte[] {(byte) this.ordinal()};
    }

    HashBuilder<byte[]> tag(HashBuilder<byte[]> hb) {
        return hb.hash(bytes);
    }
}