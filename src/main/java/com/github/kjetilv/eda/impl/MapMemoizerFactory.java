package com.github.kjetilv.eda.impl;

import com.github.kjetilv.eda.KeyHandler;
import com.github.kjetilv.eda.MapsMemoizer;
import com.github.kjetilv.eda.PojoBytes;

import java.util.function.Supplier;

public final class MapMemoizerFactory {

    public static final PojoBytes HASHCODE = value -> Hashes.bytes(value.hashCode());

    public static final PojoBytes TOSTRING = value -> value.toString().getBytes();

    /**
     * @param <I>        Id type
     * @param <K>        Key type
     * @param keyHandler Key handler, null means default behaviour
     * @return Map memoizer
     */
    public static <I, K> MapsMemoizer<I, K> create(KeyHandler<K> keyHandler) {
        return create(keyHandler, null);
    }

    /**
     * @param handler   Key handler, null means default behaviour
     * @param pojoBytes Pojo bytes
     * @return Map memoizer
     */
    public static <I, K> MapsMemoizer<I, K> create(KeyHandler<K> handler, PojoBytes pojoBytes) {
        return create(handler, pojoBytes, null);
    }

    /**
     * @param pojoBytes Pojo bytes
     * @return Map memoizer
     */
    static <I, K> MapsMemoizer<I, K> create(PojoBytes pojoBytes) {
        return create(null, pojoBytes, null);
    }

    /**
     * @param <I>     Id type
     * @param <K>     Key type
     * @param handler Key handler, null means default behaviour
     * @param hasher  Leaf hasher, for testing purposes
     * @return Map memoizer
     */
    static <I, K> MapsMemoizer<I, K> create(KeyHandler<K> handler, PojoBytes pojoBytes, LeafHasher hasher) {
        return new MapsMemoizerImpl<>(
            HASH_BUILDER_SUPPLIER,
            handler == null ? KeyHandler.defaultHandler() : handler,
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
