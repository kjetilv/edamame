package com.github.kjetilv.eda.impl;

import java.util.List;
import java.util.Map;

/**
 * A canonical value is the result of resolving a {@link HashedTree hashed tree} against shared
 * substructures of other hashed trees, including {@link Collision hash collisions}.
 */
interface CanonicalValue {

    Null NULL = new Null();

    /**
     * @return Canonical value
     */
    default Object value() {
        return null;
    }

    record Node<K>(Map<K, Object> value) implements CanonicalValue {
    }

    record Nodes(List<?> value) implements CanonicalValue {
    }

    record Leaf(Object value) implements CanonicalValue {
    }

    record Collision(Object value) implements CanonicalValue {
    }

    record Null() implements CanonicalValue {
    }
}
