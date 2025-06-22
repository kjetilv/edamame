package com.github.kjetilv.eda.impl;

import com.github.kjetilv.eda.KeyNormalizer;

/**
 * Strategy for hashing leaves.  {@link MapMemoizerFactory#create(KeyNormalizer, LeafHasher)}  Overridable}
 * for testing purposes.
 */
public interface LeafHasher {

    Hash hash(Object leaf);
}
