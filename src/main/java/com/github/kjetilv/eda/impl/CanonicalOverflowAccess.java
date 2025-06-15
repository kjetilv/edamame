package com.github.kjetilv.eda.impl;

import com.github.kjetilv.eda.hash.Hash;

import java.util.Map;

class CanonicalOverflowAccess<I, K> extends AbstractCanonicalMapAccess<I, K> {

    private final Map<I, Map<K, Object>> overflows;

    CanonicalOverflowAccess(
        Map<I, Hash> memoized,
        Map<Hash, Map<K, Object>> canonical,
        Map<I, Map<K, Object>> overflows
    ) {
        super(memoized, canonical, true);
        this.overflows = overflows;
    }

    @Override
    protected Map<K, ?> resolved(I key, Hash hash) {
        return hash == null ? overflow(key) : canonical(key, hash);
    }

    private Map<K, ?> overflow(I key) {
        Map<K, Object> overflow = overflows.get(key);
        if (overflow == null) {
            throw new IllegalArgumentException("Unknown key: " + key);
        }
        return overflow;
    }
}
