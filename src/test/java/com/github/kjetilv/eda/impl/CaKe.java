package com.github.kjetilv.eda.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("NullableProblems")
public record CaKe(String key) implements Comparable<CaKe> {

    @Override
    public int compareTo(CaKe o) {
        return key.compareTo(o.key);
    }

    private static final Map<String, CaKe> canon = new ConcurrentHashMap<>();

    public static CaKe get(String key) {
        return canon.computeIfAbsent(key, CaKe::new);
    }

    @Override
    public String toString() {
        return key;
    }
}
