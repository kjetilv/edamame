package com.github.kjetilv.eda.impl;

import java.util.List;

sealed interface HashedTree {

    Hash hash();

    record Leaf(Hash hash, Object value) implements HashedTree {
    }

    record Node(Hash hash) implements HashedTree {
    }

    record Nodes(Hash hash, List<HashedTree> values) implements HashedTree {
    }

    record Collision(Hash hash) implements HashedTree {
    }
}
