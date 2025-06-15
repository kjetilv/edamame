package com.github.kjetilv.eda.impl;

import com.github.kjetilv.eda.MapMemoizer;
import com.github.kjetilv.eda.hash.Hash;

import java.util.Map;

import static java.util.Objects.requireNonNull;

abstract class AbstractCanonicalMapAccess<I, K> implements MapMemoizer.Access<I, K> {

    private final Map<Hash, Map<K, Object>> canonicalMaps;

    private final boolean overflow;

    private final Map<?, Hash> memoized;

    AbstractCanonicalMapAccess(Map<I, Hash> memoized, Map<Hash, Map<K, Object>> canonicalMaps, boolean overflow) {
        this.memoized = memoized;
        this.canonicalMaps = canonicalMaps;
        this.overflow = overflow;
    }

    @Override
    public final int size() {
        return memoized.size();
    }

    @Override
    public final boolean overflow() {
        return overflow;
    }

    @Override
    public final Map<K, ?> get(I i) {
        return apply(i);
    }

    @Override
    public final Map<K, ?> apply(I key) {
        Hash hash = hash(key);
        return resolved(key, hash);
    }

    Map<K, Object> canonical(Object key, Hash hash) {
        Map<K, Object> canonical = canonicalMaps.get(hash);
        if (canonical == null) {
            throw new IllegalStateException("No hash found for key: " + key);
        }
        return canonical;
    }

    protected abstract Map<K, ?> resolved(I key, Hash hash);

    private Hash hash(Object key) {
        Hash hash = memoized.get(requireNonNull(key, "key"));
        if (hash == null && !overflow) {
            throw new IllegalArgumentException("Unknown key: " + key);
        }
        return hash;
    }
}
