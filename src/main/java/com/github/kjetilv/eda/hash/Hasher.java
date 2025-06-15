package com.github.kjetilv.eda.hash;

public interface Hasher {

    Hash hash(HashBuilder<byte[]> hashBuilder, Object object);
}
