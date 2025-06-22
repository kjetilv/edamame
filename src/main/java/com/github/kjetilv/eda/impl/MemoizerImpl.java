package com.github.kjetilv.eda.impl;

import com.github.kjetilv.eda.KeyNormalizer;
import com.github.kjetilv.eda.Memoized;
import com.github.kjetilv.eda.Memoizer;
import com.github.kjetilv.eda.Memoizers;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static com.github.kjetilv.eda.impl.HashedTree.*;
import static java.util.Objects.requireNonNull;

/**
 * Use {@link Memoizers#create()} and siblings to create instances of this class.
 *
 * @param <I> Identifier type.  An identifier identifies exactly one of the cached maps
 * @param <K> Key type for the maps. All maps (and their submaps) will be stored with keys of this type
 */
class MemoizerImpl<I, K> implements Memoizer<I, K>, Memoized<I, K> {

    private final Map<I, Hash> memoized = new HashMap<>();

    private Map<Hash, Map<K, Object>> canonicalMaps = new HashMap<>();

    private Map<I, Map<K, Object>> overflows = new HashMap<>();

    private final AtomicBoolean complete = new AtomicBoolean();

    private Set<Hash> memoizedHashes = new HashSet<>();

    private Map<Hash, Object> canonicalLeaves = new HashMap<>();

    private Map<Object, K> canonicalKeys = new HashMap<>();

    private final Lock lock = new ReentrantLock();

    private final KeyNormalizer<K> keyNormalizer;

    private final Supplier<HashBuilder<byte[]>> newBuilder;

    private final LeafHasher leafHasher;

    /**
     * Use {@link Memoizers#create()} and siblings to create instances of this class.
     *
     * @param newBuilder    Hash builder, not null
     * @param keyNormalizer Key normalizer, not null
     * @param leafHasher    Hasher, not null
     */
    MemoizerImpl(
        Supplier<HashBuilder<byte[]>> newBuilder,
        KeyNormalizer<K> keyNormalizer,
        LeafHasher leafHasher
    ) {
        this.newBuilder = requireNonNull(newBuilder, "newBuilder");
        this.keyNormalizer = requireNonNull(keyNormalizer, "keyNormalizer");
        this.leafHasher = requireNonNull(leafHasher, "hasher");
    }

    @Override
    public void put(I identifier, Map<?, ?> value) {
        Map<K, Object> normalized = normalized(identifier, value);
        withLock(() -> doPut(identifier, normalized));
    }

    @Override
    public Memoized<I, K> complete() {
        return withLock(this::doComplete);
    }

    @Override
    public Map<K, ?> get(I identifier) {
        return withLock(() -> doGet(identifier));
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

    private Object doPut(I identifier, Map<K, Object> value) {
        if (complete.get()) {
            throw new IllegalStateException("Already complete, cannot accept identifier " + identifier);
        }
        return switch (hashedMap(value)) {
            case Collision __ -> overflows.put(identifier, value);
            case HashedTree node -> memoized.put(identifier, node.hash());
        };
    }

    private Memoized<I, K> doComplete() {
        if (complete.compareAndSet(false, true)) {
            this.canonicalMaps = Collections.unmodifiableMap(reachableMaps());
            this.overflows = overflows.isEmpty() ? Collections.emptyMap() : Map.copyOf(overflows);
            // Free memory used for working data
            this.canonicalLeaves = null;
            this.canonicalKeys = null;
            this.memoizedHashes = null;
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    private Map<K, Object> normalized(I identifier, Map<?, ?> value) {
        requireNonNull(identifier, "identifier");
        requireNonNull(value, "value");
        return Maps.normalizeIdentifiers((Map<Object, Object>) Maps.clean(value), keyNormalizer);
    }

    /**
     * @param map Map
     * @return Hashed node for map, or null if a unique hash could not be obtained
     */
    private HashedTree hashedMap(Map<K, Object> map) {
        Map<K, HashedTree> hashedMap = transformMap(map, this::hashedTree);
        return anyCollision(hashedMap.values()).orElseGet(() ->
            resolveCanonical(map, hashedMap));
    }

    private HashedTree hashedNodes(Iterable<?> multiple) {
        List<HashedTree> values = Maps.stream(multiple)
            .map(this::hashedTree)
            .toList();
        return anyCollision(values).orElseGet(() ->
            new Nodes(hashTrees(values), values));
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

    private HashedTree hashedLeaf(Object object) {
        Hash hash = leafHasher.hash(object);
        Object existing = canonicalLeaves.putIfAbsent(hash, object);
        if (existing != null && !existing.equals(object)) {
            return new Collision(hash);
        }
        return new Leaf(hash, object);
    }

    private HashedTree resolveCanonical(Map<K, Object> map, Map<K, HashedTree> hashedMap) {
        Hash hash = hashMap(hashedMap);
        Map<K, Object> existing = canonicalMaps.computeIfAbsent(
            hash,
            __ ->
                transformMap(hashedMap, this::canonicalMap)
        );
        if (existing == null || existing.equals(map)) {
            memoizedHashes.add(hash);
            return new Node(hash);
        }
        return new Collision(hash);
    }

    private K canonicalKey(K key) {
        return canonicalKeys.computeIfAbsent(key, __ -> keyNormalizer.toKey(key));
    }

    private Hash hashTrees(Collection<? extends HashedTree> trees) {
        HashBuilder<byte[]> hb = newBuilder.get();
        HashBuilder<Hash> hashHb = hb.map(Hash::bytes);
        hb.<Integer>map(Hashes::bytes).hash(trees.size());
        return hashHb.hash(trees.stream()
            .map(HashedTree::hash)).get();
    }

    private Hash hashMap(Map<K, ? extends HashedTree> tree) {
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

    private Object canonicalMap(HashedTree tree) {
        return switch (tree) {
            case Node node -> canonicalMaps.get(node.hash());
            case Nodes(Hash ignored, List<HashedTree> values) -> values.stream()
                .map(this::canonicalMap)
                .collect(Collectors.toList());
            case Leaf(Hash hash, Object value) -> canonicalLeaves.getOrDefault(hash, value);
            case Collision collision -> collision;
        };
    }

    private <T, R> Map<K, R> transformMap(Map<K, T> map, Function<T, R> transform) {
        return Maps.toMap(map.entrySet()
            .stream()
            .map(entry ->
                Map.entry(
                    canonicalKey(entry.getKey()),
                    transform.apply(entry.getValue())
                )));
    }

    private Map<Hash, Map<K, Object>> reachableMaps() {
        return canonicalMaps.keySet()
            .stream()
            .filter(memoizedHashes::contains)
            .collect(toSizedImmutableMap());
    }

    private Collector<Hash, ?, Map<Hash, Map<K, Object>>> toSizedImmutableMap() {
        return Collectors.toMap(
            Function.identity(),
            canonicalMaps::get,
            (a, b) -> {
                throw new IllegalStateException("Duplicate key " + a);
            },
            () ->
                Maps.sizedMap(canonicalMaps.size())
        );
    }

    private <T> T withLock(Supplier<T> action) {
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }

    private static Optional<HashedTree> anyCollision(Collection<HashedTree> values) {
        return values.stream()
            .filter(Collision.class::isInstance).findAny();
    }

    private static Iterable<?> iterable(Object object) {
        int length = Array.getLength(object);
        List<Object> list = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            list.add(Array.get(object, i));
        }
        return list;
    }
}
