package com.github.kjetilv.eda.hash;

import java.util.function.Consumer;

interface ByteDigest extends Consumer<byte[]> {

    @Override
    void accept(byte[] bytes);

    Hash get();
}
