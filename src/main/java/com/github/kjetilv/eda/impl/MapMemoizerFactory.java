package com.github.kjetilv.eda.impl;

import com.github.kjetilv.eda.KeyNormalizer;
import com.github.kjetilv.eda.MapMemoizer;

import java.util.function.Supplier;

public final class MapMemoizerFactory {

    /**
     * @param <I>           Id type
     * @param <K>           Key type
     * @param keyNormalizer Key normalizer, null is default behaviour
     * @param leafHasher    Leaf hasher, for testing purposes
     * @return Map memoizer
     */
    @SuppressWarnings("unchecked")
    public static <I, K> MapMemoizer<I, K> create(
        KeyNormalizer<K> keyNormalizer,
        LeafHasher leafHasher
    ) {
        Supplier<HashBuilder<byte[]>> supplier = () ->
            new DigestiveHashBuilder<>(new ByteDigest());
        KeyNormalizer<K> normalizer = keyNormalizer == null
            ? key -> (K) key.toString()
            : keyNormalizer;
        LeafHasher leaves = leafHasher == null
            ? new DefaultLeafHasher(supplier, Object::hashCode)
            : leafHasher;
        return new MapMemoizerImpl<>(supplier, normalizer, leaves);
    }

    private MapMemoizerFactory() {
    }
}
