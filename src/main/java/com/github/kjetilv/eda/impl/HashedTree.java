package com.github.kjetilv.eda.impl;

import java.util.List;
import java.util.Map;

import static com.github.kjetilv.eda.impl.CollectionUtils.mapTree;
import static com.github.kjetilv.eda.impl.CollectionUtils.mapValues;

/**
 * A hashed tree mirrors a structure we want to store, decorating each part of the tree with a unique
 * {@link #hash() hash}.
 *
 * @param <T> Type of contents
 */
sealed interface HashedTree<T> {

    Null NULL = new Null();

    Hash hash();

    T unwrap();

    /**
     * A node in the tree
     *
     * @param hash     Hash
     * @param valueMap Map
     * @param <K>      Type of key in the map
     */
    record Node<K>(Hash hash, Map<K, ? extends HashedTree<?>> valueMap) implements HashedTree<Map<K, Object>> {
        @Override

        public Map<K, Object> unwrap() {
            return mapTree(valueMap, HashedTree::unwrap);
        }
    }

    /**
     * A list in the tree
     *
     * @param hash   Hash
     * @param values List
     */
    record Nodes(Hash hash, List<? extends HashedTree<?>> values) implements HashedTree<List<? extends HashedTree<?>>> {

        @SuppressWarnings({"unchecked", "ClassEscapesDefinedScope"})
        @Override
        public List<? extends HashedTree<?>> unwrap() {
            return (List<? extends HashedTree<?>>) mapValues(values, HashedTree::unwrap);
        }
    }

    /**
     * A leaf in the tree
     *
     * @param hash  Hash
     * @param value Leaf
     */
    record Leaf(Hash hash, Object value) implements HashedTree<Object> {

        @Override
        public Object unwrap() {
            return value;
        }
    }

    /**
     * Null value, which may occur in a list. Has the {@link Hash#NULL null} hash.
     */
    record Null() implements HashedTree<Void> {

        @Override
        public Hash hash() {
            return Hash.NULL;
        }

        @Override
        public Void unwrap() {
            return null;
        }
    }
}
