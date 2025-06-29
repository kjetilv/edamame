package com.github.kjetilv.eda.impl;

import com.github.kjetilv.eda.KeyHandler;
import com.github.kjetilv.eda.MapsMemoizers;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.github.kjetilv.eda.impl.CollectionUtils.*;
import static com.github.kjetilv.eda.impl.HashedTree.*;
import static java.util.Objects.requireNonNull;

/**
 * Normalizes input trees and builds {@link HashedTree hashed trees}. Stateless and thread-safe.
 * <p>
 *
 * @param <K> Identifier type
 */
final class RecursiveTreeHasher<K> {

    private final KeyHandler<K> keyHandler;

    private final Supplier<HashBuilder<byte[]>> newBuilder;

    private final LeafHasher leafHasher;

    /**
     * @param newBuilder Hash builder, not null
     * @param keyHandler Key handler, not null
     * @param leafHasher Hasher, not null
     * @see MapsMemoizers#create(KeyHandler)
     */
    RecursiveTreeHasher(
        Supplier<HashBuilder<byte[]>> newBuilder,
        KeyHandler<K> keyHandler,
        LeafHasher leafHasher
    ) {
        this.newBuilder = requireNonNull(newBuilder, "newBuilder");
        this.keyHandler = requireNonNull(keyHandler, "keyHandler");
        this.leafHasher = requireNonNull(leafHasher, "leafHasher");
    }

    /**
     * Returns a {@link Node} holding the hashed map value and recursively hashed sub-trees/lists/leaves.
     *
     * @param value Map
     * @return Hashed tree
     */
    @SuppressWarnings("unchecked")
    Node<K> hashedMap(Map<?, ?> value) {
        return (Node<K>) hashedTree(value);
    }

    @SuppressWarnings("unchecked")
    HashedTree<?> hashedTree(Object value) {
        return value == null
            ? NULL
            : switch (value) {
                case Map<?, ?> map -> nodeForMap((Map<K, Object>) map);
                case Iterable<?> iterable -> nodesForIterable(iterable);
                default -> value.getClass().isArray()
                    ? nodesForIterable(iterable(value))
                    : leafFor(value);
            };
    }

    private Nodes nodesForIterable(Iterable<?> iterable) {
        List<? extends HashedTree<?>> hashedValues = mapValues(iterable, this::hashedTree);
        return new Nodes(listHash(hashedValues), hashedValues);
    }

    private Node<K> nodeForMap(Map<K, Object> map) {
        Map<K, HashedTree<?>> hashedMap = normalized(map);
        return new Node<>(mapHash(hashedMap), hashedMap);
    }

    private Leaf leafFor(Object value) {
        return new Leaf(leafHash(value), value);
    }

    private Hash listHash(List<? extends HashedTree<?>> trees) {
        HashBuilder<byte[]> hb = newBuilder.get();
        HashBuilder<Hash> hashHb = hb.map(Hash::bytes);
        hb.<Integer>map(Hashes::bytes).hash(trees.size());
        trees.stream()
            .map(HashedTree::hash)
            .forEach(hashHb);
        return hashHb.get();
    }

    private Hash mapHash(Map<K, ? extends HashedTree<?>> tree) {
        HashBuilder<byte[]> hb = newBuilder.get();
        HashBuilder<Hash> hashHb = hb.map(Hash::bytes);
        HashBuilder<K> keyHb = hb.map(keyHandler::bytes);
        hb.<Integer>map(Hashes::bytes).hash(tree.size());
        tree.forEach((key, value) -> {
            keyHb.hash(key);
            hashHb.apply(value.hash());
        });
        return hb.get();
    }

    private Hash leafHash(Object value) {
        return leafHasher.hash(value);
    }

    private Map<K, HashedTree<?>> normalized(Map<?, ?> value) {
        return Collections.unmodifiableMap(value.entrySet()
            .stream()
            .filter(hasData())
            .collect(Collectors.toMap(
                entry -> keyHandler.normalize(entry.getKey()),
                entry -> hashedTree(entry.getValue()),
                noMerge(),
                sizedMap(value.size())
            )));
    }

    private static Predicate<Map.Entry<?, ?>> hasData() {
        return entry -> !isEmpty(entry.getValue());
    }

    private static boolean isEmpty(Object value) {
        return value != null && switch (value) {
            case Map<?, ?> map -> map.isEmpty();
            case Iterable<?> iterable -> !iterable.iterator().hasNext();
            case Object object -> object.getClass().isArray() && isEmpty(iterable(object));
        };
    }
}
