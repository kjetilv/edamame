package com.github.kjetilv.eda.impl;

import java.util.List;
import java.util.Map;

/**
 * A canonical value is the result of resolving a {@link HashedTree hashed tree} against shared
 * substructures of other hashed trees, including {@link Collision hash collisions}.
 */
sealed interface CanonicalValue {

    Null NULL = new Null();

    Collision COLLISION = new Collision();

    /**
     * @return Canonical value
     */
    default Object value() {
        return null;
    }

    default boolean collision() {
        return false;
    }

    record Node<K>(Map<K, Object> value) implements CanonicalValue {
    }

    record Nodes(List<?> value) implements CanonicalValue {
    }

    record Leaf(Object value) implements CanonicalValue {
    }

    record Collision() implements CanonicalValue {

        @Override
        public boolean collision() {
            return true;
        }
    }

    record Null() implements CanonicalValue {
    }
}
