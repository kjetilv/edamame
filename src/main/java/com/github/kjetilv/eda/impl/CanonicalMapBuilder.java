package com.github.kjetilv.eda.impl;

import com.github.kjetilv.eda.MapMemoizer;
import com.github.kjetilv.eda.MapMemoizers;
import com.github.kjetilv.eda.hash.Hash;
import com.github.kjetilv.eda.hash.HashBuilder;
import com.github.kjetilv.eda.hash.Hasher;
import com.github.kjetilv.eda.hash.Hashes;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.github.kjetilv.eda.MapMemoizers.Option.*;
import static java.util.Objects.requireNonNull;

public class CanonicalMapBuilder<I, K> implements MapMemoizer<I, K> {

    private final Map<I, Hash> memoized = new ConcurrentHashMap<>();

    private final Map<Hash, Map<K, Object>> canonicalMaps = new ConcurrentHashMap<>();

    private final Map<Hash, Object> canonicalLeaves = new ConcurrentHashMap<>();

    private final Function<Object, K> keyNormalizer;

    private final Map<Object, K> canonicalKeys = new ConcurrentHashMap<>();

    private final Map<I, Map<K, Object>> overflows = new ConcurrentHashMap<>();

    private final Supplier<HashBuilder<byte[]>> hashBuilder;

    private final Hasher hasher;

    private final boolean cacheLeaves;

    private final boolean keepBlanks;

    private final boolean gcCompleted;

    @SuppressWarnings("unchecked")
    public CanonicalMapBuilder(
        Supplier<HashBuilder<byte[]>> hb,
        Function<?, K> keyNormalizer,
        Hasher hasher,
        MapMemoizers.Option... options
    ) {
        this.hashBuilder = requireNonNull(hb, "hb");
        this.keyNormalizer = requireNonNull((Function<Object, K>) keyNormalizer, "canonicalKey");
        this.hasher = requireNonNull(hasher, "hasher");
        this.cacheLeaves = !is(OMIT_LEAVES, options);
        this.keepBlanks = is(KEEP_BLANKS, options);
        this.gcCompleted = !is(OMIT_GC, options);
    }

    @Override
    public int size() {
        return memoized.size();
    }

    @Override
    public int leafCount() {
        return canonicalLeaves.size();
    }

    @Override
    public boolean overflow() {
        return !overflows.isEmpty();
    }

    @Override
    public void put(I key, Map<?, ?> value) {
        requireNonNull(key, "key");
        requireNonNull(value, "value");
        Map<Object, Object> cleaned = clean(value);
        Map<K, Object> keyed = Maps.normalizeKeys(cleaned, keyNormalizer);
        HashedTree node = hashedMap(keyed);
        if (node instanceof HashedTree.Collision) {
            overflows.put(key, keyed);
        } else {
            memoized.put(key, node.hash());
        }
    }

    @Override
    public Access<I, K> complete() {
        Map<I, Hash> memoized = Map.copyOf(this.memoized);
        Map<Hash, Map<K, Object>> canonical = canonical();
        return overflows.isEmpty()
            ? new CanonicalAccess<>(memoized, canonical)
            : new CanonicalOverflowAccess<>(memoized, canonical, Map.copyOf(overflows));
    }

    @SuppressWarnings("unchecked")
    private Map<Object, Object> clean(Map<?, ?> value) {
        return (Map<Object, Object>) (keepBlanks ? value : Maps.clean(value));
    }

    /**
     * @param map Map
     * @return Hashed node for map, or null if a unique hash could not be obtained
     */
    private HashedTree hashedMap(Map<K, Object> map) {
        Map<K, HashedTree> hashedMap = Maps.toMap(map.entrySet()
            .stream()
            .map(e ->
                Map.entry(canonicalKey(e.getKey()), hashedTree(e.getValue()))
            ));
        Optional<HashedTree> hashedTree = anyCollision(hashedMap);
        return hashedTree.orElseGet(() -> {
            Hash hash = hashMap(hashedMap);
            Map<K, Object> existing = canonicalMaps.computeIfAbsent(
                hash,
                __ -> canonicalMap(hashedMap)
            );
            return existing == null || existing.equals(map)
                ? new HashedTree.Node<>(hash, hashedMap)
                // Collision
                : new HashedTree.Collision(hash, map);
        });
    }

    private HashedTree hashedNodes(Iterable<?> multiple) {
        List<HashedTree> elements = Maps.stream(multiple)
            .map(this::hashedTree)
            .toList();
        return anyCollision(elements).orElseGet(() ->
            new HashedTree.Nodes(hashTrees(elements), elements)
        );
    }

    private HashedTree hashedLeaf(Object object) {
        Hash hash = hashObject(object);
        if (cacheLeaves) {
            Object existing = canonicalLeaves.putIfAbsent(hash, object);
            if (existing != null && !existing.equals(object)) {
                return new HashedTree.Collision(hash, object);
            }
        }
        return new HashedTree.Leaf(hash, object);
    }

    @SuppressWarnings("unchecked")
    private HashedTree hashedTree(Object object) {
        return switch (object) {
            case Map<?, ?> map -> hashedMap((Map<K, Object>) map);
            case Iterable<?> iterable -> hashedNodes(iterable);
            default -> hashedLeaf(object);
        };
    }

    private static <K> Optional<HashedTree> anyCollision(Map<K, HashedTree> hashedMap) {
        return anyCollision(hashedMap.values());
    }

    private static Optional<HashedTree> anyCollision(Collection<HashedTree> values) {
        return values
            .stream()
            .filter(HashedTree::collision)
            .findAny();
    }

    private Map<K, Object> canonicalMap(Map<K, HashedTree> map) {
        return Maps.toMap(map.entrySet()
            .stream()
            .map(entry ->
                Map.entry(
                    canonicalKey(entry.getKey()),
                    canonical(entry.getValue())
                )));
    }

    private Object canonical(HashedTree tree) {
        return switch (tree) {
            case HashedTree.Node<?> node -> canonicalMaps.get(node.hash());
            case HashedTree.Nodes(Hash ignored, List<HashedTree> values) -> values.stream()
                .map(this::canonical)
                .collect(Collectors.toList());
            case HashedTree.Leaf(Hash hash, Object object) -> cacheLeaves
                ? canonicalLeaves.getOrDefault(hash, object)
                : object;
            case HashedTree.Collision collision -> collision;
        };
    }

    private K canonicalKey(K key) {
        return canonicalKeys.computeIfAbsent(key, __ -> keyNormalizer.apply(key));
    }

    private Map<Hash, Map<K, Object>> canonical() {
        if (gcCompleted) {
            return canonicalMaps.keySet()
                .stream()
                .filter(new HashSet<>(memoized.values())::contains)
                .collect(Collectors.toMap(Function.identity(), canonicalMaps::get));
        }
        return Map.copyOf(canonicalMaps);
    }

    private Hash hashObject(Object object) {
        return hasher.hash(this.hashBuilder.get(), object);
    }

    private Hash hashTrees(Collection<HashedTree> trees) {
        HashBuilder<Hash> hb = hashBuilder.get()
            .map(Hash::bytes);
        return hb.hash(trees.stream()
            .map(HashedTree::hash)).get();
    }

    private Hash hashMap(Map<K, HashedTree> tree) {
        HashBuilder<byte[]> hb = hashBuilder.get();
        hb.hash(Hashes.bytes(tree.size()));
        tree.forEach((key, value) -> {
            hb.hash(key.toString().getBytes());
            hb.hash(value.hash().bytes());
        });
        return hb.get();
    }
}
