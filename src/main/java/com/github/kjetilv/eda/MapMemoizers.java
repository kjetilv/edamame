package com.github.kjetilv.eda;

import com.github.kjetilv.eda.hash.HashBuilder;
import com.github.kjetilv.eda.hash.Hashes;
import com.github.kjetilv.eda.hash.LeafHasher;
import com.github.kjetilv.eda.impl.CanonicalMapBuilder;
import com.github.kjetilv.eda.impl.DefaultLeafHasher;

import java.util.function.Supplier;

/**
 * Factory methods for {@link MapMemoizer}s.
 */
public final class MapMemoizers {

    /**
     * String-keyed maps.
     *
     * @param <I> Type of id's
     * @return {@link MapMemoizer} for String-keyed maps
     */
    public static <I> MapMemoizer<I, String> create() {
        return create(null, null, null);
    }

    /**
     * @param newBuilder Provider for new {@link HashBuilder hash builders}
     * @param <I>        Type of id's
     * @return {@link MapMemoizer} for String-keyed maps
     */
    public static <I> MapMemoizer<I, String> create(
        Supplier<HashBuilder<byte[]>> newBuilder
    ) {
        return create(newBuilder, null, null);
    }

    /**
     * This method allows control over stored keys. Stored maps will be normalized to use {@link K}'s as map keys
     * on all levels. The {@code keyNormalizer} argument provides a callback that will produce (preferably)
     * canonical {@link K} instances, from the keys in incoming maps. Since {@link MapMemoizer} accepts
     * {@code Map<?, ?>} and this function needs to accept {@code ?} (any input) so it must handle
     * whatever maps are thrown at it later.
     *
     * @param <I>           Id type
     * @param <K>           Key type
     * @param keyNormalizer Canonical key provider
     * @return Map memoizer
     */
    public static <I, K> MapMemoizer<I, K> create(
        KeyNormalizer<K> keyNormalizer
    ) {
        return create(null, keyNormalizer, null);
    }

    /**
     * @param leafHasher Hasher
     * @return Map memoizer
     * @see #create(KeyNormalizer)
     * @see #create(Supplier)
     */
    public static <I, K> MapMemoizer<I, K> create(
        LeafHasher leafHasher
    ) {
        return create(null, null, leafHasher);
    }

    /**
     * @param keyNormalizer Key normalizer, see {@link #create(KeyNormalizer)}
     * @param leafHasher    Hasher
     * @return Map memoizer
     * @see #create(KeyNormalizer)
     * @see #create(Supplier)
     */
    public static <I, K> MapMemoizer<I, K> create(
        KeyNormalizer<K> keyNormalizer,
        LeafHasher leafHasher
    ) {
        return create(null, keyNormalizer, leafHasher);
    }

    /**
     * @param newBuilder    Provider for new {@link HashBuilder hash builders}, see {@link #create(Supplier)}
     * @param keyNormalizer Key normalizer, see {@link #create(KeyNormalizer)}
     * @param <I>           Id type
     * @param <K>           Key type
     * @return Map memoizer
     * @see #create(KeyNormalizer)
     * @see #create(Supplier)
     */
    public static <I, K> MapMemoizer<I, K> create(
        Supplier<HashBuilder<byte[]>> newBuilder,
        KeyNormalizer<K> keyNormalizer,
        LeafHasher leafHasher
    ) {
        return new CanonicalMapBuilder<>(
            newBuilder == null
                ? Hashes::md5HashBuilder
                : newBuilder,
            keyNormalizer == null
                ? KeyNormalizer.keyToString()
                : keyNormalizer,
            leafHasher == null
                ? new DefaultLeafHasher(newBuilder, Object::hashCode)
                : leafHasher
        );
    }

    private MapMemoizers() {
    }
}
