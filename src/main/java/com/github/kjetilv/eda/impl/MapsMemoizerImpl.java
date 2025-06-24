package com.github.kjetilv.eda.impl;

import com.github.kjetilv.eda.KeyNormalizer;
import com.github.kjetilv.eda.MapsMemoizer;
import com.github.kjetilv.eda.MapsMemoizers;
import com.github.kjetilv.eda.MemoizedMaps;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.kjetilv.eda.impl.HashedTree.*;
import static java.util.Objects.requireNonNull;

/**
 * Works by hashing nodes and leaves and storing them under their hashes. When structures and/or values
 * re-occur, they are replaced by the already registered, canonical instances.
 * <p>
 * MD5 (128-bit) hashes are used. If an incoming value provokes a hash collision, it is stored as-is,
 * separately from the canonical trees.  This should be rare.
 * <p>
 * Use {@link MapsMemoizers#create()} and siblings to create instances of this class.
 *
 * @param <I> Identifier type.  An identifier identifies exactly one of the cached maps
 * @param <K> Key type for the maps. All maps (and their submaps) will be stored with keys of this type
 */
class MapsMemoizerImpl<I, K> implements MapsMemoizer<I, K>, MemoizedMaps<I, K> {

    private final KeyNormalizer<K> keyNormalizer;

    private final Supplier<HashBuilder<byte[]>> newBuilder;

    private final LeafHasher leafHasher;

    private Map<I, Hash> memoized = new HashMap<>();

    private Map<Hash, Map<K, Object>> canonicalMaps = new HashMap<>();

    private Map<I, Map<K, Object>> overflows = new HashMap<>();

    private final AtomicBoolean complete = new AtomicBoolean();

    private Set<Hash> memoizedHashes = new HashSet<>();

    private Map<Hash, Object> canonicalLeaves = new HashMap<>();

    private Map<Object, K> canonicalKeys = new HashMap<>();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * @param newBuilder    Hash builder, not null
     * @param keyNormalizer Key normalizer, not null
     * @param leafHasher    Hasher, not null
     * @see MapsMemoizers#create(KeyNormalizer)
     */
    MapsMemoizerImpl(
        Supplier<HashBuilder<byte[]>> newBuilder,
        KeyNormalizer<K> keyNormalizer,
        LeafHasher leafHasher
    ) {
        this.newBuilder = requireNonNull(newBuilder, "newBuilder");
        this.keyNormalizer = requireNonNull(keyNormalizer, "keyNormalizer");
        this.leafHasher = requireNonNull(leafHasher, "leafHasher");
    }

    @Override
    public void put(I identifier, Map<?, ?> value) {
        put(identifier, value, true);
    }

    @Override
    public boolean putIfAbsent(I identifier, Map<?, ?> value) {
        return put(identifier, value, false);
    }

    @Override
    public Map<K, ?> get(I identifier) {
        requireNonNull(identifier, "identifier");
        return withLock(lock.readLock(), () -> doGet(identifier));
    }

    @Override
    public MemoizedMaps<I, K> complete() {
        return complete.compareAndSet(false, true)
            ? withLock(lock.writeLock(), this::doComplete)
            : this;
    }

    private boolean put(I identifier, Map<?, ?> value, boolean failOnConflict) {
        Map<K, Object> normalized = normalized(
            requireNonNull(identifier, "identifier"),
            requireNonNull(value, "value")
        );
        try {
            return withLock(
                lock.writeLock(),
                () -> doPut(identifier, normalized, failOnConflict)
            );
        } catch (Exception e) {
            throw new IllegalStateException(this + " failed to insert " + identifier, e);
        }
    }

    private Map<K, Object> doGet(I identifier) {
        Hash hash = memoized.get(requireNonNull(identifier, "identifier"));
        return hash != null ? canonicalMaps.get(hash)
            : !overflows.isEmpty() ? overflows.get(identifier)
                : null;
    }

    private boolean doPut(I identifier, Map<K, Object> value, boolean failOnConflict) {
        if (memoized.containsKey(identifier)) {
            if (failOnConflict && !sameValue(identifier, value)) {
                throw new IllegalArgumentException("Identifier " + identifier + " was:" + get(identifier));
            }
            return false;
        }
        switch (hashedMap(value)) {
            case Collision __ -> overflows.put(identifier, value);
            case HashedTree node -> {
                Hash hash = node.hash();
                memoized.put(identifier, hash);
                memoizedHashes.add(hash);
            }
        }
        return true;
    }

    private boolean sameValue(I identifier, Map<K, Object> value) {
        Map<K, ?> existing = get(identifier);
        return existing == null || existing.equals(value);
    }

    private MapsMemoizerImpl<I, K> doComplete() {
        // Lock down lookupable maps
        this.canonicalMaps =
            Collections.unmodifiableMap(canonicalMaps.keySet()
                .stream()
                .filter(memoizedHashes::contains)
                .collect(Collectors.toMap(
                    Function.identity(),
                    canonicalMaps::get,
                    noMerge(),
                    () -> Maps.sizedMap(canonicalMaps.size())
                )));
        this.memoized =
            Collections.unmodifiableMap(this.memoized);
        this.overflows = overflows.isEmpty()
            ? Collections.emptyMap()
            : Collections.unmodifiableMap(overflows);
        // Free memory used by other maps
        this.canonicalLeaves = null;
        this.canonicalKeys = null;
        this.memoizedHashes = null;
        return this;
    }

    private String doDescribe() {
        int count = memoized.size();
        int overflowsCount = overflows.size();
        return (count + overflowsCount) +
               " items" +
               (overflowsCount == 0 ? ", " : " (" + overflowsCount + " collisions), ") +
               (complete.get() ? "completed" : "working maps:" + canonicalMaps.size());
    }

    @SuppressWarnings("unchecked")
    private Map<K, Object> normalized(I identifier, Map<?, ?> value) {
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
    private HashedTree hashedTree(Object value) {
        return value == null
            ? NULL
            : switch (value) {
                case Map<?, ?> map -> hashedMap((Map<K, Object>) map);
                case Iterable<?> iterable -> hashedNodes(iterable);
                default -> value.getClass().isArray()
                    ? hashedNodes(iterable(value))
                    : hashedLeaf(value);
            };
    }

    private HashedTree hashedLeaf(Object value) {
        Hash hash = leafHasher.hash(value);
        Object existing = canonicalLeaves.putIfAbsent(hash, value);
        return sameOrNull(existing, value)
            ? new Leaf(hash, value)
            : new Collision(hash, existing, value);
    }

    private HashedTree resolveCanonical(Map<K, Object> map, Map<K, HashedTree> hashedMap) {
        Hash hash = hashMap(hashedMap);
        Map<K, Object> existing = canonicalMaps.computeIfAbsent(
            hash,
            __ ->
                transformMap(hashedMap, this::canonicalMap)
        );
        return sameOrNull(existing, map)
            ? new Node(hash)
            : new Collision(hash, existing, map);
    }

    private K canonicalKey(K key) {
        return canonicalKeys.computeIfAbsent(key, __ -> keyNormalizer.toKey(key));
    }

    private Hash hashTrees(Collection<? extends HashedTree> trees) {
        HashBuilder<byte[]> hb = newBuilder.get();
        HashBuilder<Hash> hashHb = hb.map(Hash::bytes);
        hb.<Integer>map(Hashes::bytes).hash(trees.size());
        Stream<Hash> hashes = trees.stream()
            .filter(Objects::nonNull)
            .map(HashedTree::hash);
        return hashHb.hash(hashes).get();
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
        return tree == null
            ? NULL
            : switch (tree) {
                case Node node -> canonicalMaps.get(node.hash());
                case Nodes(Hash ignored, List<HashedTree> values) -> values.stream()
                    .map(this::canonicalMap)
                    .collect(Collectors.toList());
                case Leaf(Hash hash, Object value) -> canonicalLeaves.getOrDefault(hash, value);
                case Null __ -> null;
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

    private static <T> T withLock(Lock lock, Supplier<T> action) {
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }

    private static Optional<HashedTree> anyCollision(Collection<HashedTree> values) {
        return values.stream()
            .filter(Collision.class::isInstance)
            .findAny();
    }

    private static Iterable<?> iterable(Object object) {
        int length = Array.getLength(object);
        List<Object> list = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            list.add(Array.get(object, i));
        }
        return list;
    }

    private static boolean sameOrNull(Object existing, Object value) {
        return existing == null || existing.equals(value);
    }

    private static <K> BinaryOperator<Map<K, Object>> noMerge() {
        return (k1, k2) -> {
            throw new IllegalStateException("Duplicate key " + k1 + "/" + k2);
        };
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + withLock(lock.readLock(), this::doDescribe) + "]";
    }
}
