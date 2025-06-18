package com.github.kjetilv.eda.impl;

import com.github.kjetilv.eda.KeyNormalizer;
import com.github.kjetilv.eda.MapMemoizer;
import com.github.kjetilv.eda.MapMemoizers;
import com.github.kjetilv.eda.Option;
import com.github.kjetilv.eda.hash.Hash;
import com.github.kjetilv.eda.hash.HashBuilder;
import com.github.kjetilv.eda.hash.Hasher;
import com.github.kjetilv.eda.hash.Hashes;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.github.kjetilv.eda.Option.*;
import static java.util.Objects.requireNonNull;

/**
 * Use {@link MapMemoizers#create(Option...)} and siblings to create instances of this class.
 *
 * @param <I> Identifier type.  An identifier identifies exactly one of the cached maps
 * @param <K> Key type for the maps. All maps (and their submaps) will be stored with keys of this type
 */
public class CanonicalMapBuilder<I, K> implements MapMemoizer<I, K> {

    static <I, K> Access<I, K> create(
        Map<I, Hash> memoized,
        Map<Hash, Map<K, Object>> canonical,
        Map<I, Map<K, Object>> overflows
    ) {
        return overflows.isEmpty()
            ? new CanonicalAccess<>(memoized, canonical)
            : new CanonicalOverflowAccess<>(memoized, canonical, Map.copyOf(overflows));
    }

    private final Map<I, Hash> memoized = new ConcurrentHashMap<>();

    private final Map<Hash, Map<K, Object>> canonicalMaps = new ConcurrentHashMap<>();

    private final Map<Hash, Object> canonicalLeaves = new ConcurrentHashMap<>();

    private final KeyNormalizer<K> keyNormalizer;

    private final Map<Object, K> canonicalKeys = new ConcurrentHashMap<>();

    private final Map<I, Map<K, Object>> overflows = new ConcurrentHashMap<>();

    private final Supplier<HashBuilder<byte[]>> newBuilder;

    private final Hasher hasher;

    private final boolean cacheLeaves;

    private final boolean keepBlanks;

    private final boolean gcCompleted;

    /**
     * Use {@link MapMemoizers#create(Option...)} and siblings to create instances of this class.
     *
     * @param newBuilder    Hash builder, not null
     * @param keyNormalizer Key normalizer, not null
     * @param hasher        Hasher, not null
     * @param options       Options
     */
    public CanonicalMapBuilder(
        Supplier<HashBuilder<byte[]>> newBuilder,
        KeyNormalizer<K> keyNormalizer,
        Hasher hasher,
        Option... options
    ) {
        this.newBuilder = requireNonNull(newBuilder, "newBuilder");
        this.keyNormalizer = requireNonNull(keyNormalizer, "keyNormalizer");
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
    public int overflow() {
        return overflows.size();
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
        return create(
            Map.copyOf(this.memoized),
            gcCompleted
                ? prunedCanonical(canonicalMaps, memoized.values())
                : Map.copyOf(canonicalMaps),
            overflows
        );
    }

    @Override
    public Map<K, ?> apply(I identifier) {
        return create(memoized, canonicalMaps, overflows).get(identifier);
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
        return anyCollision(hashedMap.values())
            .orElseGet(() -> {
                Hash hash = hashMap(hashedMap);
                Map<K, Object> existing =
                    canonicalMaps.computeIfAbsent(
                        hash,
                        __ -> canonicalMap(hashedMap)
                    );
                return existing == null || existing.equals(map)
                    ? new HashedTree.Node<>(hash, hashedMap)
                    : new HashedTree.Collision(hash);
            });
    }

    private HashedTree hashedNodes(Iterable<?> multiple) {
        List<HashedTree> elements = Maps.stream(multiple)
            .map(this::hashedTree)
            .toList();
        return anyCollision(elements)
            .orElseGet(() ->
                new HashedTree.Nodes(hashTrees(elements), elements));
    }

    private HashedTree hashedLeaf(Object object) {
        Hash hash = hashObject(object);
        if (cacheLeaves) {
            Object existing = canonicalLeaves.putIfAbsent(hash, object);
            if (existing != null && !existing.equals(object)) {
                return new HashedTree.Collision(hash);
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

    private K canonicalKey(K key) {
        return canonicalKeys.computeIfAbsent(key, __ -> keyNormalizer.toKey(key));
    }

    private Hash hashObject(Object object) {
        return hasher.hash(object);
    }

    private Hash hashTrees(Collection<HashedTree> trees) {
        HashBuilder<byte[]> hb = newBuilder.get();
        HashBuilder<Hash> hashHb = hb.map(Hash::bytes);
        hb.<Integer>map(Hashes::bytes).hash(trees.size());
        return hashHb.hash(trees.stream()
            .map(HashedTree::hash)).get();
    }

    private Hash hashMap(Map<K, HashedTree> tree) {
        HashBuilder<byte[]> hb = newBuilder.get();
        HashBuilder<Hash> hashHb = hb.map(Hash::bytes);
        HashBuilder<K> keyHb = hb.map(keyNormalizer::bytes);
        hb.<Integer>map(Hashes::bytes).hash(tree.size());
        tree.forEach((key, value) -> {
            keyHb.hash(key);
            hashHb.apply(value.hash());
        });
        return hb.get();
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

    private static <K> Map<Hash, Map<K, Object>> prunedCanonical(
        Map<Hash, Map<K, Object>> maps,
        Collection<Hash> overflow
    ) {
        return maps.keySet()
            .stream()
            .filter(new HashSet<>(overflow)::contains)
            .collect(Collectors.toMap(Function.identity(), maps::get));
    }

    private static Optional<HashedTree> anyCollision(Collection<HashedTree> values) {
        return values.stream()
            .filter(HashedTree::collision).findAny();
    }
}
