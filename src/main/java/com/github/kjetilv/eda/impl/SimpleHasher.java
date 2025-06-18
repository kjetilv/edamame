package com.github.kjetilv.eda.impl;

import com.github.kjetilv.eda.hash.Hash;
import com.github.kjetilv.eda.hash.Hasher;

public final class SimpleHasher implements Hasher {

    @Override
    public Hash hash(Object object) {
        return Hash.of(0L, object.hashCode());
    }
}
