package com.github.kjetilv.eda.impl;

import java.util.List;

sealed interface HashedTree {

    Null NULL = new Null();

    Hash hash();

    record Node(Hash hash) implements HashedTree {
    }

    record Nodes(Hash hash, List<HashedTree> values) implements HashedTree {
    }

    record Leaf(Hash hash, Object value) implements HashedTree {
    }

    record Null() implements HashedTree {

        @Override
        public Hash hash() {
            return Hashes.NULL;
        }
    }

    record Collision(Hash hash) implements HashedTree {
    }
}
