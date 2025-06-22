package com.github.kjetilv.eda.impl;

import com.github.kjetilv.eda.KeyNormalizer;
import com.github.kjetilv.eda.MapsMemoizer;

import java.util.function.Supplier;
import java.util.function.ToIntFunction;

public final class MapMemoizerFactory {

    /**
     * @param <I>           Id type
     * @param <K>           Key type
     * @param keyNormalizer Key normalizer, null means default behaviour
     * @return Map memoizer
     */
    public static <I, K> MapsMemoizer<I, K> create(KeyNormalizer<K> keyNormalizer) {
        return create(keyNormalizer, null);
    }

    /**
     * @param <I>           Id type
     * @param <K>           Key type
     * @param keyNormalizer Key normalizer, null means default behaviour
     * @param leafHasher    Leaf hasher, for testing purposes
     * @return Map memoizer
     */
    static <I, K> MapsMemoizer<I, K> create(KeyNormalizer<K> keyNormalizer, LeafHasher leafHasher) {
        return new MapsMemoizerImpl<>(
            HASH_BUILDER_SUPPLIER,
            keyNormalizer == null ? defaultNormalizer() : keyNormalizer,
            leafHasher == null ? DEFAULT_LEAF_HASHER : leafHasher
        );
    }

    private MapMemoizerFactory() {
    }

    private static final Supplier<HashBuilder<byte[]>> HASH_BUILDER_SUPPLIER =
        () -> new DigestiveHashBuilder<>(new ByteDigest());

    private static final ToIntFunction<Object> ANY_HASH =
        Object::hashCode;

    private static final LeafHasher DEFAULT_LEAF_HASHER =
        new DefaultLeafHasher(HASH_BUILDER_SUPPLIER, ANY_HASH);

    @SuppressWarnings("unchecked")
    private static <K> KeyNormalizer<K> defaultNormalizer() {
        return key -> (K) key.toString();
    }
}
