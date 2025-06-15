package com.github.kjetilv.eda.hash;

import java.util.function.Consumer;

interface ByteDigest extends Consumer<byte[]> {

    default void digest(byte[] bytes) {
        accept(bytes);
    }

    @Override
    void accept(byte[] bytes);

    Hash get();
}
