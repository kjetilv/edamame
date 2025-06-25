package com.github.kjetilv.eda.impl;
import com.github.kjetilv.eda.KeyNormalizer;
import com.github.kjetilv.eda.MapsMemoizer;
import com.github.kjetilv.eda.PojoBytes;

import java.util.function.Supplier;

public final class MapMemoizerFactory {

    public static final PojoBytes HASHCODE = value -> Hashes.bytes(value.hashCode());

    public static final PojoBytes TOSTRING = value -> value.toString().getBytes();

    /**
     * @param <I>           Id type
     * @param <K>           Key type
     * @param keyNormalizer Key normalizer, null means default behaviour
     *
     * @return Map memoizer
     */
    public static <I, K> MapsMemoizer<I, K> create(KeyNormalizer<K> keyNormalizer) {
        return create(keyNormalizer, null);
    }

    /**
     * @param pojoBytes@return Map memoizer
     */
    static <I, K> MapsMemoizer<I, K> create(PojoBytes pojoBytes) {
        return create(null, pojoBytes, null);
    }

    /**
     * @param normalizer Key normalizer, null means default behaviour
     * @param pojoBytes  Leaf bytes
     *
     * @return Map memoizer
     */
    public static <I, K> MapsMemoizer<I, K> create(KeyNormalizer<K> normalizer, PojoBytes pojoBytes) {
        return create(normalizer, pojoBytes, null);
    }

    /**
     * @param <I>        Id type
     * @param <K>        Key type
     * @param normalizer Key normalizer, null means default behaviour
     * @param hasher     Leaf hasher, for testing purposes
     *
     * @return Map memoizer
     */
    static <I, K> MapsMemoizer<I, K> create(
        KeyNormalizer<K> normalizer,
        PojoBytes pojoBytes,
        LeafHasher hasher
    ) {
        return new MapsMemoizerImpl<>(
            HASH_BUILDER_SUPPLIER,
            normalizer == null ? KeyNormalizer.defaultNormalizer() : normalizer,
            hasher == null
                ? defaultLeafHasher(pojoBytes == null ? PojoBytes.HASHCODE : pojoBytes)
                : hasher
        );
    }

    private MapMemoizerFactory() {
    }

    private static final Supplier<HashBuilder<byte[]>> HASH_BUILDER_SUPPLIER =
        () -> DigestiveHashBuilder.create(new ByteDigest());

    private static LeafHasher defaultLeafHasher(PojoBytes leaf) {
        return new DefaultLeafHasher(HASH_BUILDER_SUPPLIER, leaf);
    }
}
