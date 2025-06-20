package com.github.kjetilv.eda.impl;

import com.github.kjetilv.eda.hash.Hash;

import java.util.Map;

final class CanonicalOverflowAccess<I, K> extends AccessBase<I, K> {

    private final Map<I, Map<K, Object>> overflows;

    CanonicalOverflowAccess(
        Map<I, Hash> memoized,
        Map<Hash, Map<K, Object>> canonical,
        Map<I, Map<K, Object>> overflows
    ) {
        super(memoized, canonical, overflows.size());
        this.overflows = overflows;
    }

    @Override
    protected Map<K, ?> resolved(I identifier, Hash hash) {
        return hash == null ? overflow(identifier) : canonical(identifier, hash);
    }

    private Map<K, ?> overflow(I identifier) {
        Map<K, Object> overflow = overflows.get(identifier);
        if (overflow == null) {
            throw new IllegalArgumentException("Unknown identifier: " + identifier);
        }
        return overflow;
    }
}
