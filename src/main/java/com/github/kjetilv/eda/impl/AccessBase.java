package com.github.kjetilv.eda.impl;

import com.github.kjetilv.eda.MapMemoizer;
import com.github.kjetilv.eda.hash.Hash;

import java.util.Map;

import static java.util.Objects.requireNonNull;

abstract class AccessBase<I, K> implements MapMemoizer.Access<I, K> {

    static <I, K> MapMemoizer.Access<I, K> create(
        Map<I, Hash> memoized,
        Map<Hash, Map<K, Object>> canonical,
        Map<I, Map<K, Object>> overflows
    ) {
        return overflows.isEmpty()
            ? new CanonicalAccess<>(memoized, canonical)
            : new CanonicalOverflowAccess<>(memoized, canonical, Map.copyOf(overflows));
    }

    private final Map<Hash, Map<K, Object>> canonicalMaps;

    private final int overflow;

    private final Map<?, Hash> memoized;

    AccessBase(Map<I, Hash> memoized, Map<Hash, Map<K, Object>> canonicalMaps, int overflow) {
        this.memoized = memoized;
        this.canonicalMaps = canonicalMaps;
        this.overflow = overflow;
    }

    @Override
    public final int size() {
        return memoized.size();
    }

    @Override
    public int overflow() {
        return overflow;
    }

    @Override
    public final Map<K, ?> get(I identifier) {
        return resolved(identifier, hash(identifier));
    }

    final Map<K, Object> canonical(Object key, Hash hash) {
        Map<K, Object> canonical = canonicalMaps.get(hash);
        if (canonical == null) {
            throw new IllegalStateException("No hash found for key: " + key);
        }
        return canonical;
    }

    protected abstract Map<K, ?> resolved(I key, Hash hash);

    private Hash hash(Object key) {
        Hash hash = memoized.get(requireNonNull(key, "key"));
        if (hash == null && overflow == 0) {
            throw new IllegalArgumentException("Unknown key: " + key);
        }
        return hash;
    }

    @Override
    public final String toString() {
        return getClass().getSimpleName() + "[" + memoized + (overflow > 0 ? " overflow=" + overflow : "") + "]";
    }
}
