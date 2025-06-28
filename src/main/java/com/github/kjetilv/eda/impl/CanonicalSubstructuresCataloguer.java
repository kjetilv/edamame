package com.github.kjetilv.eda.impl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static com.github.kjetilv.eda.impl.CollectionUtils.mapTree;
import static com.github.kjetilv.eda.impl.CollectionUtils.mapValues;

/**
 * This class should be thread-safe, as it only appends to concurrent maps.
 *
 * @param <K>
 */
final class CanonicalSubstructuresCataloguer<K> {

    private final Map<Hash, Map<K, Object>> maps = new ConcurrentHashMap<>();

    private final Map<Hash, List<Object>> lists = new ConcurrentHashMap<>();

    private final Map<Hash, Object> leaves = new ConcurrentHashMap<>();

    /**
     * Returns the canonical value. Traverses the {@link HashedTree hashed tree} and re-builds it.  New
     * substructures found are stored under their respective {@link HashedTree#hash() hashes}.  If the
     * hash is recorded from before, the first occurrence is retrieved to replaces the incoming one.
     *
     * @param hashedTree Hashed tree
     * @return A {@link CanonicalValue value} which may be either a {@link CanonicalValue.Collision collision},
     * or a holder for the canonical value
     */
    @SuppressWarnings("unchecked")
    public CanonicalValue canonical(HashedTree<?> hashedTree) {
        return switch (hashedTree) {
            case HashedTree.Node<?>(Hash hash, Map<?, ? extends HashedTree<?>> valueMap) -> {
                Map<K, CanonicalValue> canonicalTrees = recurseTrees((Map<K, HashedTree<?>>) valueMap);
                yield collision(canonicalTrees).orElseGet(() -> {
                    Map<K, Object> computed = mapTree(canonicalTrees, CanonicalValue::value);
                    return canonical(
                        cataloguedMap(hash, computed),
                        computed,
                        CanonicalValue.Node::new
                    );
                });
            }
            case HashedTree.Nodes(Hash hash, List<? extends HashedTree<?>> values) -> {
                List<CanonicalValue> canonicalValues = recurseValues(values);
                yield collision(canonicalValues).orElseGet(() -> {
                    List<Object> computed = mapValues(canonicalValues, CanonicalValue::value);
                    return canonical(
                        cataloguedList(hash, computed),
                        computed,
                        CanonicalValue.Nodes::new
                    );
                });
            }
            case HashedTree.Leaf(Hash hash, Object value) -> canonical(
                cataloguedLeaf(hash, value),
                value,
                CanonicalValue.Leaf::new
            );
            case HashedTree.Null ignored -> CanonicalValue.NULL;
        };
    }

    private Map<K, CanonicalValue> recurseTrees(Map<K, HashedTree<?>> hashedTrees) {
        return mapTree(hashedTrees, this::canonical);
    }

    private List<CanonicalValue> recurseValues(List<? extends HashedTree<?>> values) {
        return mapValues(values, this::canonical);
    }

    private Map<K, Object> cataloguedMap(Hash hash, Map<K, Object> computed) {
        return maps.putIfAbsent(hash, computed);
    }

    private List<Object> cataloguedList(Hash hash, List<Object> computed) {
        return lists.putIfAbsent(hash, computed);
    }

    private Object cataloguedLeaf(Hash hash, Object value) {
        return leaves.putIfAbsent(hash, value);
    }

    private static <T> CanonicalValue canonical(T existing, T value, Function<T, CanonicalValue> wrap) {
        return existing == null ? wrap.apply(value)
            : existing.equals(value) ? wrap.apply(existing)
                : new CanonicalValue.Collision(value);
    }

    private static <K> Optional<CanonicalValue> collision(Map<K, CanonicalValue> canonicalValues) {
        return collision(canonicalValues.values());
    }

    private static Optional<CanonicalValue> collision(Collection<CanonicalValue> values) {
        return values.stream().filter(CanonicalValue.Collision.class::isInstance).findAny();
    }
}
