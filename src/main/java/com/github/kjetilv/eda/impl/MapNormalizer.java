package com.github.kjetilv.eda.impl;

import com.github.kjetilv.eda.KeyHandler;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.github.kjetilv.eda.impl.CollectionUtils.*;
import static java.util.Objects.requireNonNull;

final class MapNormalizer<K> {

    private final KeyHandler<K> keyHandler;

    MapNormalizer(KeyHandler<K> keyHandler) {
        this.keyHandler = requireNonNull(keyHandler, "keyHandler");
    }

    Map<K, Object> normalize(Map<?, ?> map) {
        return requireNonNull(map, "map")
            .entrySet()
            .stream()
            .filter(MapNormalizer::hasData)
            .collect(
                Collectors.toMap(
                    entry ->
                        keyHandler.normalize(entry.getKey()),
                    entry ->
                        rewriteObject(entry.getValue()),
                    noMerge(),
                    () ->
                        sizedMap(map.size(), IdentityHashMap::new)
                ));
    }

    private Object rewriteObject(Object value) {
        return value == null ? null
            : switch (value) {
                case Map<?, ?> map -> rewriteMap(map);
                case Iterable<?> iterable -> rewriteIterable(iterable);
                case Object object -> object.getClass().isArray()
                    ? rewriteIterable(iterable(object))
                    : value;
            };
    }

    private Map<K, ?> rewriteMap(Map<?, ?> map) {
        return map.entrySet()
            .stream()
            .filter(MapNormalizer::hasData)
            .collect(
                Collectors.toMap(
                    entry ->
                        keyHandler.normalize(entry.getKey()),
                    entry ->
                        rewriteObject(entry.getValue()),
                    noMerge(),
                    () ->
                        sizedMap(map.size(), IdentityHashMap::new)
                ));
    }

    private List<Object> rewriteIterable(Iterable<?> iterable) {
        return stream(iterable)
            .map(this::rewriteObject)
            .toList();
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
}

