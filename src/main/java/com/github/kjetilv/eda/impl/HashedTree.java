package com.github.kjetilv.eda.impl;

import java.util.List;
import java.util.Map;

sealed interface HashedTree {

    Hash hash();

    record Leaf(Hash hash, Object value) implements HashedTree {
    }

    record Node<K>(Hash hash, Map<K, HashedTree> tree) implements HashedTree {
    }

    record Nodes(Hash hash, List<HashedTree> values) implements HashedTree {
    }

    record Collision(Hash hash) implements HashedTree {
    }
}
