package com.github.kjetilv.eda;

import com.github.kjetilv.eda.impl.MapMemoizerFactory;

/**
 * Factory methods for {@link MapMemoizer}s.
 */
public final class MapMemoizers {

    /**
     * For simple maps with {@link String string} keys – or any keys that map naturally to
     * strings via regular {@link Object#toString() toString}
     *
     * @param <I> Type of id's
     * @return {@link MapMemoizer} for String-keyed maps
     */
    public static <I> MapMemoizer<I, String> create() {
        return create(null);
    }

    /**
     * This method allows better control over stored keys. Stored maps will be normalized to use {@link K}'s as map keys
     * on all levels. The {@code keyNormalizer} argument provides a callback that will produce (preferably)
     * canonical {@link K} instances from keys in incoming maps.
     * <p>
     * Since {@link MapMemoizer} accepts {@link java.util.Map Map<?, ?>}, this function needs to accept any
     * input, i.e. {@link Object ?}.
     *
     * @param <I>           Id type
     * @param <K>           Key type
     * @param keyNormalizer Key normalizer
     * @return Map memoizer
     */
    public static <I, K> MapMemoizer<I, K> create(KeyNormalizer<K> keyNormalizer) {
        return MapMemoizerFactory.create(keyNormalizer, null);
    }

    private MapMemoizers() {
    }
}
