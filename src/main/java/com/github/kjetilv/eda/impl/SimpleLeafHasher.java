package com.github.kjetilv.eda.impl;

import com.github.kjetilv.eda.hash.Hash;
import com.github.kjetilv.eda.hash.LeafHasher;

public final class SimpleLeafHasher implements LeafHasher {

    @Override
    public Hash hash(Object leaf) {
        return Hash.of(0L, leaf.hashCode());
    }
}
