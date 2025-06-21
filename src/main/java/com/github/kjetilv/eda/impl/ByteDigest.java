package com.github.kjetilv.eda.impl;

import java.util.function.Consumer;

interface ByteDigest extends Consumer<byte[]> {

    @Override
    void accept(byte[] bytes);

    Hash get();
}
