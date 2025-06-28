package com.github.kjetilv.eda.impl;

import com.github.kjetilv.eda.KeyHandler;
import com.github.kjetilv.eda.PojoBytes;

/**
 * Strategy for hashing leaves.  {@link MapMemoizerFactory#create(KeyHandler, PojoBytes, LeafHasher) Overridable}
 * for testing purposes.
 */
public interface LeafHasher {

    Hash hash(Object leaf);
}
