package com.github.kjetilv.eda;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public record CaKe(String key)  {

    private static final Map<String, CaKe> canon = new ConcurrentHashMap<>();

    public static CaKe get(String key) {
        return canon.computeIfAbsent(key, CaKe::new);
    }

    @Override
    public String toString() {
        return key;
    }
}
