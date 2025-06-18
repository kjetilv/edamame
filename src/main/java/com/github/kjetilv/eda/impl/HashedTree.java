package com.github.kjetilv.eda.impl;

import com.github.kjetilv.eda.hash.Hash;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

sealed interface HashedTree extends Comparable<HashedTree> {

    @Override
    default int compareTo(HashedTree o) {
        return hash().compareTo(o.hash());
    }

    default boolean collision() {
        return false;
    }

    Hash hash();

    record Leaf(Hash hash, Object value) implements HashedTree {

        @SuppressWarnings("NullableProblems")
        @Override
        public String toString() {
            return getClass().getSimpleName() + "[" + hash.toString() + ": " + value + "]";
        }
    }

    record Node<K>(Hash hash, Map<K, HashedTree> level) implements HashedTree {

        @SuppressWarnings("NullableProblems")
        @Override
        public String toString() {
            String keys = level.keySet()
                .stream()
                .map(Object::toString)
                .collect(Collectors.joining(", "));
            return getClass().getSimpleName() + "[" + hash.toString() + ": <" + keys + ">]";
        }
    }

    record Nodes(Hash hash, List<HashedTree> elements) implements HashedTree {

        @SuppressWarnings("NullableProblems")
        @Override
        public String toString() {
            return getClass().getSimpleName() + "[" + hash.toString() + ": " + elements.size() + " elements]";
        }
    }

    record Collision(Hash hash) implements HashedTree {

        @Override
        public boolean collision() {
            return true;
        }
    }
}
