package com.github.kjetilv.eda.impl;

import com.github.kjetilv.eda.KeyHandler;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.lang.Math.ceil;
import static java.util.Objects.requireNonNull;

/**
 * Some collection-related nitty-gritty bits.
 */
@SuppressWarnings("unchecked")
final class CollectionUtils {

    static <K, V> Map<K, V> sizedMap(int size) {
        return new HashMap<>(capacity(size));
    }

    /**
     * Clean the map of blanks, i.e. nulls, empty maps, and empty lists
     *
     * @param map Map
     * @return Map with blanks removed
     */
    static Map<Object, Object> clean(Map<?, ?> map) {
        return cleanMap(requireNonNull((Map<Object, Object>) map, "map"));
    }

    static <K, V> Map<K, V> toMap(Stream<Map.Entry<K, V>> entries) {
        return entries.collect(Collectors.toMap(
            Map.Entry::getKey,
            Map.Entry::getValue
        ));
    }

    static <K> Map<K, Object> normalizeKeys(Map<Object, Object> map, KeyHandler<K> keyHandler) {
        return (Map<K, Object>) rewriteMap(keyHandler, requireNonNull(map, "map"));
    }

    static <T> Stream<T> stream(Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    static <K, V, R> Map<K, R> mapTree(Map<K, V> map, Function<V, R> transform) {
        return Collections.unmodifiableMap(map.entrySet()
            .stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> transform.apply(entry.getValue()),
                noMerge(),
                () -> sizedMap(map.size())
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

    static Iterable<?> iterable(Object object) {
        int length = Array.getLength(object);
        List<Object> list = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            list.add(Array.get(object, i));
        }
        return list;
    }

    private CollectionUtils() {
    }

    private static final int MAX_POWER_OF_TWO = 1 << Integer.SIZE - 2;

    private static <R> BinaryOperator<R> noMerge() {
        return (r1, r2) -> {
            throw new IllegalStateException("Duplicate key " + r1 + "/" + r2);
        };
    }

    private static int capacity(int expectedSize) {
        return expectedSize < 3 ? expectedSize + 1
            : expectedSize < MAX_POWER_OF_TWO ? (int) ceil(expectedSize / 0.75)
                : Integer.MAX_VALUE;
    }

    private static <T> Map<T, Object> cleanMap(Map<T, ?> map) {
        return toMap(map.entrySet()
            .stream()
            .filter(CollectionUtils::hasData)
            .map(CollectionUtils::pruneEntry));
    }

    private static Object cleanObject(Object value) {
        return value == null ? null
            : switch (value) {
                case Map<?, ?> map -> cleanMap(map);
                case Iterable<?> iterable -> stream(iterable)
                    .map(CollectionUtils::cleanObject)
                    .toList();
                default -> value;
            };
    }

    private static <T> Map.Entry<T, Object> pruneEntry(Map.Entry<T, ?> entry) {
        return Map.entry(entry.getKey(), cleanObject(entry.getValue()));
    }

    private static boolean hasData(Object value) {
        return switch (value) {
            case Map.Entry<?, ?> entry -> hasData(entry.getValue());
            case Iterable<?> iterable -> iterable.iterator().hasNext();
            case Map<?, ?> map -> !map.isEmpty();
            case null -> false;
            default -> true;
        };
    }

    private static <K> Map<K, ?> rewriteMap(KeyHandler<K> keyHandler, Map<?, ?> map) {
        return toMap(map.entrySet()
            .stream()
            .map(entry ->
                Map.entry(
                    keyHandler.normalize(entry.getKey()),
                    rewriteObject(entry.getValue(), keyHandler)
                )));
    }

    private static <K> Object rewriteObject(Object value, KeyHandler<K> keyHandler) {
        return value == null ? null
            : switch (value) {
                case Map<?, ?> map -> rewriteMap(keyHandler, map);
                case Iterable<?> iterable -> stream(iterable)
                    .map(v ->
                        rewriteObject(v, keyHandler))
                    .toList();
                default -> value;
            };
    }
}
