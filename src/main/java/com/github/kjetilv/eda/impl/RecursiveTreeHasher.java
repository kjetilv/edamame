package com.github.kjetilv.eda.impl;

import com.github.kjetilv.eda.KeyHandler;
import com.github.kjetilv.eda.MapsMemoizers;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static com.github.kjetilv.eda.impl.CollectionUtils.*;
import static com.github.kjetilv.eda.impl.HashedTree.*;
import static java.util.Objects.requireNonNull;

/**
 * Builds {@link HashedTree hashed trees}. Stateless and thread-safe.
 *
 * @param <K> Identifier type
 */
final class RecursiveTreeHasher<K> {

    private final KeyHandler<K> keyHandler;

    private final Supplier<HashBuilder<byte[]>> newBuilder;

    private final LeafHasher leafHasher;

    /**
     * @param newBuilder Hash builder, not null
     * @param keyHandler Key normalizer, not null
     * @param leafHasher Hasher, not null
     * @see MapsMemoizers#create(KeyHandler)
     */
    RecursiveTreeHasher(Supplier<HashBuilder<byte[]>> newBuilder, KeyHandler<K> keyHandler, LeafHasher leafHasher) {
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
    Node<K> hashedMap(Map<K, Object> value) {
        Map<K, HashedTree<?>> hashedTrees = mapTree(value, this::hashedTree);
        Hash hash = hashForMap(hashedTrees);
        return new Node<>(hash, hashedTrees);
    }

    Nodes hashedList(List<Object> values) {
        List<? extends HashedTree<?>> hashedList = mapValues(values, this::hashedTree);
        Hash hash = hashForList(hashedList);
        return new Nodes(hash, hashedList);
    }

    @SuppressWarnings("unchecked")
    private HashedTree<?> hashedTree(Object value) {
        return value == null ? NULL : switch (value) {
            case Map<?, ?> map -> {
                Map<K, HashedTree<?>> hashedMap =
                    mapTree((Map<K, Object>) map, this::hashedTree);
                yield new Node<>(hashForMap(hashedMap), hashedMap);
            }
            case Iterable<?> iterable -> hashedValues(iterable);
            default -> value.getClass().isArray() ? hashedValues(iterable(value))
                : hashedLeaf(value);
        };
    }

    private Nodes hashedValues(Iterable<?> values) {
        List<? extends HashedTree<?>> hashedValues = mapValues(values, this::hashedTree);
        return new Nodes(hashForList(hashedValues), hashedValues);
    }

    private Leaf hashedLeaf(Object value) {
        return new Leaf(leafHasher.hash(value), value);
    }

    private Hash hashForList(List<? extends HashedTree<?>> trees) {
        HashBuilder<byte[]> hb = newBuilder.get();
        HashBuilder<Hash> hashHb = hb.map(Hash::bytes);
        hb.<Integer>map(Hashes::bytes).hash(trees.size());
        trees.stream()
            .map(HashedTree::hash)
            .forEach(hashHb);
        return hashHb.get();
    }

    private Hash hashForMap(Map<K, ? extends HashedTree<?>> tree) {
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
}
