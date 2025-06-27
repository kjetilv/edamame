package com.github.kjetilv.eda.impl;

import com.github.kjetilv.eda.KeyHandler;

/**
 * Strategy for hashing leaves.  {@link MapMemoizerFactory#create(KeyHandler, LeafHasher)}  Overridable}
 * for testing purposes.
 */
public interface LeafHasher {

    Hash hash(Object leaf);
}
