package com.github.kjetilv.eda.impl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import static com.github.kjetilv.eda.impl.CollectionUtils.transformValues;
import static com.github.kjetilv.eda.impl.CollectionUtils.transform;

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
    public CanonicalValue toCanonical(HashedTree<?> hashedTree) {
        return switch (hashedTree) {
            case HashedTree.Node<?>(Hash hash, Map<?, ? extends HashedTree<?>> valueMap) -> {
                Map<K, CanonicalValue> canonicalTrees = recurseMap((Map<K, HashedTree<?>>) valueMap);
                yield collision(canonicalTrees).orElseGet(() -> {
                    Map<K, Object> map = mapValue(canonicalTrees);
                    return resolve(
                        cataloguedMap(hash, map),
                        map,
                        CanonicalValue.Node::new
                    );
                });
            }
            case HashedTree.Nodes(Hash hash, List<? extends HashedTree<?>> values) -> {
                List<CanonicalValue> canonicalValues = recurseList(values);
                yield collision(canonicalValues).orElseGet(() -> {
                    List<Object> list = listValue(canonicalValues);
                    return resolve(
                        cataloguedList(hash, list),
                        list,
                        CanonicalValue.Nodes::new
                    );
                });
            }
            case HashedTree.Leaf(Hash hash, Object value) -> resolve(
                cataloguedLeaf(hash, value),
                value,
                CanonicalValue.Leaf::new
            );
            case HashedTree.Null ignored -> CanonicalValue.NULL;
        };
    }

    private Map<K, CanonicalValue> recurseMap(Map<K, HashedTree<?>> hashedTrees) {
        return transformValues(hashedTrees, this::toCanonical);
    }

    private List<CanonicalValue> recurseList(List<? extends HashedTree<?>> values) {
        return transform(values, this::toCanonical);
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

    private static <T> CanonicalValue resolve(T existing, T value, Function<T, CanonicalValue> wrap) {
        return existing == null ? wrap.apply(value)
            : existing.equals(value) ? wrap.apply(existing)
                : CanonicalValue.COLLISION;
    }

    private static List<Object> listValue(List<CanonicalValue> canonicalValues) {
        return transform(canonicalValues, CanonicalValue::value);
    }

    private static <K> Map<K, Object> mapValue(Map<K, CanonicalValue> canonicalTrees) {
        return transformValues(canonicalTrees, CanonicalValue::value);
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
