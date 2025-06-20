package com.github.kjetilv.eda.impl;

import com.github.kjetilv.eda.KeyNormalizer;
import com.github.kjetilv.eda.MapMemoizer;
import com.github.kjetilv.eda.MapMemoizers;
import com.github.kjetilv.eda.Option;
import com.github.kjetilv.eda.hash.*;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
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
public class CanonicalMapBuilder<I, K> implements MapMemoizer<I, K>, MapMemoizer.Access<I, K> {

    private final Map<I, Hash> memoized = new ConcurrentHashMap<>();

    private Map<Hash, Map<K, Object>> canonicalMaps = new ConcurrentHashMap<>();

    private final Map<Hash, Object> canonicalLeaves = new ConcurrentHashMap<>();

    private final Map<Object, K> canonicalKeys = new ConcurrentHashMap<>();

    private final Map<I, Map<K, Object>> overflows = new ConcurrentHashMap<>();

    private final Lock lock = new ReentrantLock();

    private final KeyNormalizer<K> keyNormalizer;

    private final Supplier<HashBuilder<byte[]>> newBuilder;

    private final LeafHasher leafHasher;

    private final boolean cacheLeaves;

    private final boolean keepBlanks;

    private final boolean gcCompleted;

    private final boolean forkComplete;

    private boolean forked;

    private boolean complete;

    /**
     * Use {@link MapMemoizers#create(Option...)} and siblings to create instances of this class.
     *
     * @param newBuilder    Hash builder, not null
     * @param keyNormalizer Key normalizer, not null
     * @param leafHasher    Hasher, not null
     * @param options       Options
     */
    public CanonicalMapBuilder(
        Supplier<HashBuilder<byte[]>> newBuilder,
        KeyNormalizer<K> keyNormalizer,
        LeafHasher leafHasher,
        Option... options
    ) {
        this.newBuilder = requireNonNull(newBuilder, "newBuilder");
        this.keyNormalizer = requireNonNull(keyNormalizer, "keyNormalizer");
        this.leafHasher = requireNonNull(leafHasher, "hasher");
        this.cacheLeaves = !is(OMIT_LEAVES, options);
        this.keepBlanks = is(KEEP_BLANKS, options);
        this.gcCompleted = !is(OMIT_GC, options);
        this.forkComplete = is(FORK_COMPLETE, options);
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
    public void put(I identifier, Map<?, ?> value) {
        Map<K, Object> identified = identify(value);
        lock.lock();
        try {
            doPut(requireNonNull(identifier, "identifier"), identified);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Access<I, K> complete() {
        lock.lock();
        try {
            return doComplete();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Map<K, ?> get(I identifier) {
        lock.lock();
        try {
            return doGet(identifier);
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<K, Object> identify(Map<?, ?> value) {
        requireNonNull(value, "value");
        return Maps.normalizeIdentifiers(
            (Map<Object, Object>) (keepBlanks ? value : Maps.clean(value)),
            keyNormalizer
        );
    }

    private Map<K, Object> doGet(I identifier) {
        Hash hash = memoized.get(requireNonNull(identifier, "identifier"));
        if (hash == null) {
            if (overflows.isEmpty()) {
                throw new IllegalArgumentException("Unknown identifier: " + identifier);
            }
            Map<K, Object> map = overflows.get(identifier);
            if (map == null) {
                throw new IllegalArgumentException("Unknown identifier: " + identifier);
            }
            return map;
        }
        Map<K, Object> map = canonicalMaps.get(hash);
        if (map == null) {
            throw new IllegalStateException("No hash for: " + identifier);
        }
        return map;
    }

    private void doPut(I identifier, Map<K, Object> value) {
        failForked();
        if (complete) {
            throw new IllegalStateException("Already complete, cannot accept identifier " + identifier);
        }
        HashedTree node = hashedMap(value);
        if (node instanceof HashedTree.Collision) {
            overflows.put(identifier, value);
        } else {
            memoized.put(identifier, node.hash());
        }
    }

    private Access<I, K> doComplete() {
        failForked();
        if (complete) {
            return this;
        }
        Map<Hash, Map<K, Object>> canonicalMaps = gcCompleted
            ? prunedCanonical(this.canonicalMaps, memoized.values())
            : Map.copyOf(this.canonicalMaps);
        if (forkComplete) {
            forked = true;
            return AccessBase.create(Map.copyOf(this.memoized), canonicalMaps, overflows);
        }
        this.canonicalMaps = canonicalMaps;
        this.canonicalLeaves.clear();
        this.canonicalKeys.clear();
        this.complete = true;
        return this;
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
        return anyCollision(hashedMap.values()).orElseGet(() -> resolveCanonical(map, hashedMap));
    }

    private HashedTree hashedNodes(Iterable<?> multiple) {
        List<HashedTree> values = Maps.stream(multiple)
            .map(this::hashedTree)
            .toList();
        return anyCollision(values)
            .orElseGet(() -> new HashedTree.Nodes(hashTrees(values), values));
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
            default -> object.getClass().isArray()
                ? hashedNodes(iterable(object))
                : hashedLeaf(object);
        };
    }

    private HashedTree resolveCanonical(Map<K, Object> map, Map<K, HashedTree> hashedMap) {
        Hash hash = hashMap(hashedMap);
        Map<K, Object> existing =
            canonicalMaps.computeIfAbsent(hash, __ -> canonicalMap(hashedMap));
        return existing == null || existing.equals(map)
            ? new HashedTree.Node<>(hash, hashedMap)
            : new HashedTree.Collision(hash);
    }

    private void failForked() {
        if (forkComplete && forked) {
            throw new IllegalStateException("Already forked");
        }
    }

    private K canonicalKey(K key) {
        return canonicalKeys.computeIfAbsent(key, __ -> keyNormalizer.toKey(key));
    }

    private Hash hashObject(Object object) {
        return leafHasher.hash(object);
    }

    private Hash hashTrees(Collection<? extends Hashed> trees) {
        HashBuilder<byte[]> hb = newBuilder.get();
        HashBuilder<Hash> hashHb = hb.map(Hash::bytes);
        hb.<Integer>map(Hashes::bytes).hash(trees.size());
        return hashHb.hash(trees.stream()
            .map(Hashed::hash)).get();
    }

    private Hash hashMap(Map<K, ? extends Hashed> tree) {
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
            case HashedTree.Nodes(Hash __, List<HashedTree> values) -> values.stream()
                .map(this::canonical)
                .collect(Collectors.toList());
            case HashedTree.Leaf(Hash hash, Object value) -> cacheLeaves
                ? canonicalLeaves.getOrDefault(hash, value)
                : value;
            case HashedTree.Collision collision -> collision;
        };
    }

    private static Iterable<?> iterable(Object object) {
        int length = Array.getLength(object);
        List<Object> list = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            list.add(Array.get(object, i));
        }
        return list;
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
