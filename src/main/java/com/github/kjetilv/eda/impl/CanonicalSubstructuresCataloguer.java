package com.github.kjetilv.eda.impl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import static com.github.kjetilv.eda.impl.CollectionUtils.mapTree;
import static com.github.kjetilv.eda.impl.CollectionUtils.mapValues;

/**
 * Canonicalizes {@link HashedTree hashed trees}, progressively storing and resolving shared substructures
 * as they appear.
 * <p>
 * This class ought to be thread-safe, as it only appends to {@link ConcurrentMap concurrent maps}.
 *
 * @param <K>
 */
final class CanonicalSubstructuresCataloguer<K> {

    private final Map<Hash, Map<K, Object>> maps = new ConcurrentHashMap<>();

    private final Map<Hash, List<Object>> lists = new ConcurrentHashMap<>();

    private final Map<Hash, Object> leaves = new ConcurrentHashMap<>();

    /**
     * Accepts a {@link HashedTree hashed tree} and returns the {@link CanonicalValue canonical value}.
     * Traverses the {@link HashedTree hashed tree} and re-builds it.  New substructures found in incoming
     * structures are recorded under their respective {@link HashedTree#hash() hashes}.  If the hash is
     * recorded already, that occurrence is retrieved and used to replace the incoming one.
     * <p>
     * This method tries to show the recursive flow with a minimum of fuss, which is why it calls out
     * to a lot of one-liners, which would otherwise add up to a lot of clutter.
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
                : CanonicalValue.COLLISION;
    }

    /**
     * Collision, of any
     *
     * @param map Map
     * @return Any collision
     */
    private static Optional<CanonicalValue> collision(Map<?, CanonicalValue> map) {
        return collision(map.values());
    }

    /**
     * Collision, of any
     *
     * @param values List
     * @return Any collision
     */
    private static Optional<CanonicalValue> collision(Collection<CanonicalValue> values) {
        return values.stream()
            .filter(CanonicalValue::collision)
            .findFirst();
    }
}
