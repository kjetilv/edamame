package com.github.kjetilv.eda.impl;

import com.github.kjetilv.eda.KeyNormalizer;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Objects.requireNonNull;

final class Maps {

    /**
     * Clean the map of blanks, i.e. nulls, empty maps, and empty lists
     *
     * @param map Map
     * @return Map with blanks removed
     */
    static Map<?, ?> clean(Map<?, ?> map) {
        return cleanMap(requireNonNull(map, "map"));
    }

    @SuppressWarnings("unchecked")
    static <K, T> Map<K, Object> normalizeIdentifiers(Map<T, Object> map, KeyNormalizer<K> keyNormalizer) {
        return (Map<K, Object>) rewriteMap(keyNormalizer, requireNonNull(map, "map"));
    }

    static <T> Stream<T> stream(Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    static <K, V> Map<K, V> toMap(Stream<Map.Entry<K, V>> entries) {
        return entries.collect(Collectors.toMap(
            Map.Entry::getKey,
            Map.Entry::getValue
        ));
    }

    private Maps() {
    }

    private static <T> Map<T, Object> cleanMap(Map<T, ?> map) {
        return toMap(map.entrySet()
            .stream()
            .filter(Maps::hasData)
            .map(Maps::pruneEntry)
        );
    }

    private static Object cleanObject(Object value) {
        return switch (value) {
            case Map<?, ?> map -> clean(map);
            case Iterable<?> iterable -> stream(iterable)
                .map(Maps::cleanObject)
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

    private static <K> Map<K, ?> rewriteMap(
        KeyNormalizer<K> keyNormalizer,
        Map<?, ?> map
    ) {
        return toMap(map.entrySet()
            .stream()
            .map(entry ->
                Map.entry(
                    keyNormalizer.toKey(entry.getKey()),
                    rewriteObject(entry.getValue(), keyNormalizer)
                )));
    }

    private static <K> Object rewriteObject(
        Object value,
        KeyNormalizer<K> keyNormalizer
    ) {
        return switch (value) {
            case Map<?, ?> map -> rewriteMap(keyNormalizer, map);
            case Iterable<?> iterable -> stream(iterable)
                .map(v ->
                    rewriteObject(v, keyNormalizer))
                .toList();
            default -> value;
        };
    }
}
