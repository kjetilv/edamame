package com.github.kjetilv.eda.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

sealed interface HashedTree extends Comparable<HashedTree> {

    @Override
    default int compareTo(HashedTree o) {
        return hash().compareTo(o.hash());
    }

    Hash hash();

    record Leaf(Hash hash, Object value) implements HashedTree {

        @SuppressWarnings("NullableProblems")
        @Override
        public String toString() {
            return getClass().getSimpleName() + "[" + hash.toString() + ": " + value + "]";
        }
    }

    record Node<K>(Hash hash, Map<K, HashedTree> tree) implements HashedTree {

        @SuppressWarnings("NullableProblems")
        @Override
        public String toString() {
            String keys = tree.keySet()
                .stream()
                .map(Object::toString)
                .collect(Collectors.joining(", "));
            return getClass().getSimpleName() + "[" + hash.toString() + ": <" + keys + ">]";
        }
    }

    record Nodes(Hash hash, List<HashedTree> values) implements HashedTree {

        @SuppressWarnings("NullableProblems")
        @Override
        public String toString() {
            return getClass().getSimpleName() + "[" + hash.toString() + ": " + values.size() + " elements]";
        }
    }

    record Collision(Hash hash) implements HashedTree {
    }
}
