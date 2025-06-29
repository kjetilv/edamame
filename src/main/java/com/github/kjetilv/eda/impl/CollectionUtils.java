package com.github.kjetilv.eda.impl;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.lang.Math.ceil;

/**
 * Some collection-related nitty-gritty bits.
 */
final class CollectionUtils {

    static <K, V> Supplier<Map<K, V>> sizedMap(int size) {
        return () -> new HashMap<>(capacity(size));
    }

    static <K, V, R> Map<K, R> mapTree(Map<K, V> map, Function<V, R> transform) {
        return Collections.unmodifiableMap(map.entrySet()
            .stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> transform.apply(entry.getValue()),
                noMerge(),
                sizedMap(map.size())
            )));
    }

    static <K, T, V, R> Map<K, R> mapTree(Map<T, V> map, Function<T, K> keyNormalizer, Function<V, R> transform) {
        return Collections.unmodifiableMap(map.entrySet()
            .stream()
            .collect(Collectors.toMap(
                entry -> keyNormalizer.apply(entry.getKey()),
                entry -> transform.apply(entry.getValue()),
                noMerge(),
                sizedMap(map.size())
            )));
    }

    static <T, R> List<R> mapValues(List<? extends T> list, Function<T, R> transform) {
        return list.stream()
            .map(transform)
            .toList();
    }

    static <T, R> List<R> mapValues(Iterable<? extends T> list, Function<T, R> transform) {
        return stream(list)
            .map(transform)
            .toList();
    }

    static Iterable<?> iterable(Object array) {
        return () -> new Iterator<>() {

            private final int length = Array.getLength(array);

            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < length;
            }

            @Override
            public Object next() {
                try {
                    return Array.get(array, index);
                } finally {
                    index++;
                }
            }
        };
    }

    static <R> BinaryOperator<R> noMerge() {
        return (r1, r2) -> {
            throw new IllegalStateException("Duplicate key " + r1 + "/" + r2);
        };
    }

    static <T> Stream<T> stream(Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    private CollectionUtils() {
    }

    private static final int MAX_POWER_OF_TWO = 1 << Integer.SIZE - 2;

    private static int capacity(int expectedSize) {
        return expectedSize < 3 ? expectedSize + 1
            : expectedSize < MAX_POWER_OF_TWO ? (int) ceil(expectedSize / 0.75)
                : Integer.MAX_VALUE;
    }
}
