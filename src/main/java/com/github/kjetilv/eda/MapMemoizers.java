package com.github.kjetilv.eda;

import com.github.kjetilv.eda.hash.HashBuilder;
import com.github.kjetilv.eda.hash.Hasher;
import com.github.kjetilv.eda.hash.Hashes;
import com.github.kjetilv.eda.impl.CanonicalMapBuilder;
import com.github.kjetilv.eda.impl.DefaultHasher;

import java.util.function.Supplier;

import static com.github.kjetilv.eda.Option.USE_SYSTEM_HC;
import static com.github.kjetilv.eda.Option.is;

/**
 * Factory methods for {@link MapMemoizer}s.
 */
@SuppressWarnings("unused")
public final class MapMemoizers {

    /**
     * String-keyed maps.
     *
     * @param options Options
     * @param <I>     Type of id's
     * @return {@link MapMemoizer} for String-keyed maps
     */
    public static <I> MapMemoizer<I, String> create(Option... options) {
        return create(null, null, null, options);
    }

    /**
     * @param newBuilder Provider for new {@link HashBuilder hash builders}
     * @param options    Options
     * @param <I>        Type of id's
     * @return {@link MapMemoizer} for String-keyed maps
     */
    public static <I> MapMemoizer<I, String> create(
        Supplier<HashBuilder<byte[]>> newBuilder,
        Option... options
    ) {
        return create(newBuilder, null, null, options);
    }

    /**
     * This method allows control over stored keys. Stored maps will be normalized to use {@link K}'s as map keys
     * on all levels. The {@code keyNormalizer} argument provides a callback that will produce (preferably) canonical {@link K}
     * instances, from the keys in incoming maps. Since {@link MapMemoizer} accepts {@code Map<?, ?>} and
     * this function needs to accept {@code ?} (any input) so it must handle whatever maps are thrown at it later.
     *
     * @param <I>           Id type
     * @param <K>           Key type
     * @param keyNormalizer Canonical key provider
     * @param options       Options
     * @return Map memoizer
     */
    public static <I, K> MapMemoizer<I, K> create(
        KeyNormalizer<K> keyNormalizer,
        Option... options
    ) {
        return create(null, keyNormalizer, null, options);
    }

    /**
     * @param hasher  Hasher
     * @param options Options
     * @return Map memoizer
     * @see #create(KeyNormalizer, Option...)
     * @see #create(Supplier, Option...)
     */
    public static <I, K> MapMemoizer<I, K> create(
        Hasher hasher,
        Option... options
    ) {
        return create(null, null, hasher, options);
    }

    /**
     * @param keyNormalizer Key normalizer, see {@link #create(KeyNormalizer, Option...)}
     * @param hasher        Hasher
     * @param options       Options
     * @return Map memoizer
     * @see #create(KeyNormalizer, Option...)
     * @see #create(Supplier, Option...)
     */
    public static <I, K> MapMemoizer<I, K> create(
        KeyNormalizer<K> keyNormalizer,
        Hasher hasher,
        Option... options
    ) {
        return create(null, keyNormalizer, hasher, options);
    }

    /**
     * @param newBuilder    Provider for new {@link HashBuilder hash builders}, see {@link #create(Supplier, Option...)}
     * @param keyNormalizer Key normalizer, see {@link #create(KeyNormalizer, Option...)}
     * @param options       Options
     * @param <I>           Id type
     * @param <K>           Key type
     * @return Map memoizer
     * @see #create(KeyNormalizer, Option...)
     * @see #create(Supplier, Option...)
     */
    public static <I, K> MapMemoizer<I, K> create(
        Supplier<HashBuilder<byte[]>> newBuilder,
        KeyNormalizer<K> keyNormalizer,
        Hasher hasher,
        Option... options
    ) {
        return new CanonicalMapBuilder<>(
            newBuilder == null ? Hashes::md5HashBuilder : newBuilder,
            keyNormalizer == null ? KeyNormalizer.keyToString() : keyNormalizer,
            hasher == null
                ? new DefaultHasher(newBuilder, is(USE_SYSTEM_HC, options))
                : hasher,
            options
        );
    }

    private MapMemoizers() {
    }

}
