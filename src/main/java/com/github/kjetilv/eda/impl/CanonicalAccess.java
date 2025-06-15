package com.github.kjetilv.eda.impl;

import com.github.kjetilv.eda.MapMemoizer;
import com.github.kjetilv.eda.hash.Hash;

import java.util.Map;

class CanonicalAccess<I, K> extends AbstractCanonicalMapAccess<I, K> implements MapMemoizer.Access<I, K> {

    CanonicalAccess(Map<I, Hash> memoized, Map<Hash, Map<K, Object>> canonicalMaps) {
        super(memoized, canonicalMaps, false);
    }

    @Override
    protected Map<K, ?> resolved(I key, Hash hash) {
        return canonical(key, hash);
    }
}
