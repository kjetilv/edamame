package com.github.kjetilv.eda.impl;

import com.github.kjetilv.eda.KeyHandler;
import com.github.kjetilv.eda.MapsMemoizer;
import com.github.kjetilv.eda.MapsMemoizers;
import com.github.kjetilv.eda.MemoizedMaps;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

import static com.github.kjetilv.eda.impl.HashedTree.Node;
import static java.util.Objects.requireNonNull;

/**
 * Works by hashing nodes and leaves and storing them under their hashes. When structures and/or values
 * re-occur, they are replaced by the already registered, canonical instances.
 * <p>
 * MD5 (128-bit) hashes are used. If an incoming value provokes a hash collision, it will be stored as-is and
 * separately from the canonical trees.  This should be rare.
 * <p>
 * Use {@link MapsMemoizers#create()} and siblings to create instances of this class.
 *
 * @param <I> Identifier type.  An identifier identifies exactly one of the cached maps
 * @param <K> Key type for the maps. All maps (and their submaps) will be stored with keys of this type
 */
class MapsMemoizerImpl<I, K> implements MapsMemoizer<I, K>, MemoizedMaps<I, K>, KeyHandler<K> {

    private final Map<I, Hash> memoizedHashes = new HashMap<>();

    private final Map<Hash, Map<K, Object>> canonicalObjects = new HashMap<>();

    private final Map<I, Map<K, Object>> overflowObjects = new HashMap<>();

    private final AtomicBoolean complete = new AtomicBoolean();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private Map<Object, K> canonicalKeys = new ConcurrentHashMap<>();

    private Map<K, byte[]> canonicalBytes = new ConcurrentHashMap<>();

    private RecursiveTreeHasher<K> recursiveTreeHasher;

    private CanonicalSubstructuresCataloguer<K> canonicalSubstructuresCataloguer;

    private final KeyHandler<K> keyHandler;

    /**
     * @param newBuilder Hash builder, not null
     * @param keyHandler Key handler, not null
     * @param leafHasher Hasher, not null
     * @see MapsMemoizers#create(KeyHandler)
     */
    MapsMemoizerImpl(
        Supplier<HashBuilder<byte[]>> newBuilder,
        KeyHandler<K> keyHandler,
        LeafHasher leafHasher
    ) {
        requireNonNull(keyHandler, "key handler");
        this.keyHandler = keyHandler;
        this.recursiveTreeHasher = new RecursiveTreeHasher<>(
            requireNonNull(newBuilder, "newBuilder"),
            this,
            requireNonNull(leafHasher, "leafHasher")
        );
        this.canonicalSubstructuresCataloguer = new CanonicalSubstructuresCataloguer<>();
    }

    @Override
    public void put(I identifier, Map<?, ?> value) {
        put(
            requireNonNull(identifier, "identifier"),
            requireNonNull(value, "value"),
            true
        );
    }

    @Override
    public boolean putIfAbsent(I identifier, Map<?, ?> value) {
        return put(
            requireNonNull(identifier, "identifier"),
            requireNonNull(value, "value"),
            false
        );
    }

    @Override
    public int size() {
        return memoizedHashes.size() + overflowObjects.size();
    }

    @Override
    public Map<K, ?> get(I identifier) {
        requireNonNull(identifier, "identifier");
        return withReadLock(() -> {
            Hash hash = memoizedHashes.get(requireNonNull(identifier, "identifier"));
            return hash != null ? canonicalObjects.get(hash)
                : !overflowObjects.isEmpty() ? overflowObjects.get(identifier)
                    : null;
        });
    }

    @Override
    public MemoizedMaps<I, K> complete() {
        return complete.compareAndSet(false, true)
            ? withWriteLock(this::doComplete)
            : this;
    }

    @Override
    public K normalize(Object key) {
        return canonicalKeys.computeIfAbsent(key, keyHandler::normalize);
    }

    @Override
    public byte[] bytes(K key) {
        return canonicalBytes.computeIfAbsent(key, keyHandler::bytes);
    }

    @SuppressWarnings("unchecked")
    private boolean put(I identifier, Map<?, ?> value, boolean failOnConflict) {
        if (complete.get()) {
            throw new IllegalStateException(this + " is complete, cannot put " + identifier);
        }
        return switch (recursiveTreeHasher.hashedTree(value)) {
            case Node<?> node -> {
                CanonicalValue canonical = canonicalSubstructuresCataloguer.canonical((Node<K>) node);
                yield update(identifier, canonical, (Node<K>) node, failOnConflict);
            }
            case HashedTree<?> other -> throw new IllegalArgumentException(
                "Unexpected hashed tree " + other
            );
        };
    }

    private boolean update(I identifier, CanonicalValue canonicalValue, Node<K> node, boolean failOnConflict) {
        return withWriteLock(() -> {
            if (shouldPut(identifier, failOnConflict)) {
                store(identifier, node, canonicalValue);
                return true;
            }
            return false;
        });
    }

    @SuppressWarnings({"unchecked", "unused"})
    private void store(I identifier, Node<K> node, CanonicalValue canonicalValue) {
        switch (canonicalValue) {
            case CanonicalValue.Node<?>(Map<?, Object> map) -> {
                memoizedHashes.put(identifier, node.hash());
                canonicalObjects.put(node.hash(), (Map<K, Object>) map);
            }
            case CanonicalValue.Collision __ -> overflowObjects.put(
                identifier,
                node.unwrap()
            );
            default -> throw new IllegalStateException(
                "Unexpected canonical value for node " + node + ": " + canonicalValue
            );
        }
    }

    private MapsMemoizerImpl<I, K> doComplete() {
        // Shed working data
        this.recursiveTreeHasher = null;
        this.canonicalSubstructuresCataloguer = null;
        this.canonicalKeys = null;
        this.canonicalBytes = null;
        return this;
    }

    private boolean shouldPut(I identifier, boolean failOnConflict) {
        if (!memoizedHashes.containsKey(identifier)) {
            return true;
        }
        if (failOnConflict) {
            throw new IllegalArgumentException("Identifier " + identifier + " was:" + get(identifier));
        }
        return false;
    }

    private String doDescribe() {
        int count = memoizedHashes.size();
        int overflowsCount = overflowObjects.size();
        return (count + overflowsCount) +
               " items" +
               (overflowsCount == 0 ? ", " : " (" + overflowsCount + " collisions), ") +
               (complete.get() ? "completed" : "working maps:" + canonicalObjects.size());
    }

    private <T> T withReadLock(Supplier<T> action) {
        return withLock(lock.readLock(), action);
    }

    private <T> T withWriteLock(Supplier<T> action) {
        return withLock(lock.writeLock(), action);
    }

    private static <T> T withLock(Lock lock, Supplier<T> action) {
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + withReadLock(this::doDescribe) + "]";
    }
}
