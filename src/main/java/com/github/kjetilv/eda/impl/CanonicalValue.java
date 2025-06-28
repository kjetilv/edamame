package com.github.kjetilv.eda.impl;

import java.util.List;
import java.util.Map;

interface CanonicalValue {

    Null NULL = new Null();

    Object value();

    record Node<K>(Map<K, Object> value) implements CanonicalValue {
    }

    record Nodes(List<?> value) implements CanonicalValue {
    }

    record Leaf(Object value) implements CanonicalValue {
    }

    record Collision(Object value) implements CanonicalValue {
    }

    record Null() implements CanonicalValue {

        @Override
        public Object value() {
            return null;
        }
    }
}
