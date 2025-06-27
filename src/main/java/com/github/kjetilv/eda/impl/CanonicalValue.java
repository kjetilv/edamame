package com.github.kjetilv.eda.impl;

import java.util.List;
import java.util.Map;

public interface CanonicalValue {

    Object value();

    Null NULL = new Null();

    record Node<K>(Map<K, Object> value) implements CanonicalValue {
    }

    record Nodes(List<?> value) implements CanonicalValue {
    }

    record Leaf(Object value) implements CanonicalValue {
    }

    record Collision<K>(Object value) implements CanonicalValue {
    }

    record Null() implements CanonicalValue {

        @Override
        public Object value() {
            return null;
        }
    }
}
