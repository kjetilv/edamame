package com.github.kjetilv.eda;

import com.github.kjetilv.eda.impl.MapMemoizerFactory;

import java.util.Map;

/**
 * Factory methods for {@link MapsMemoizer}s.
 */
public final class MapsMemoizers {

    /**
     * For simple maps with {@link String string} keys – or any keys that map naturally to
     * strings via regular {@link Object#toString() toString}
     *
     * @param <I> Type of id's
     * @return {@link MapsMemoizer} for String-keyed maps
     */
    public static <I> MapsMemoizer<I, String> create() {
        return create(null, null);
    }

    /**
     * This method affords control over stored keys. Stored maps will be normalized to use {@link K}'s
     * as map keys, on all levels. The {@code keyNormalizer} argument provides a callback that will
     * produce {@link K} instances from keys in incoming maps.
     * <p>
     * Since {@link MapsMemoizer} accepts {@link Map Map<?, ?>}, this function needs to accept any
     * input, i.e. {@link Object ?}.
     *
     * @param <I>           Id type
     * @param <K>           Key type
     * @param keyNormalizer Key normalizer
     * @return Map memoizer
     */
    public static <I, K> MapsMemoizer<I, K> create(KeyNormalizer<K> keyNormalizer) {
        return MapMemoizerFactory.create(keyNormalizer);
    }

    public static <I, K> MapsMemoizer<I, K> create(PojoBytes pojoBytes) {
        return create(null, pojoBytes);
    }

    public static <I, K> MapsMemoizer<I, K> create(KeyNormalizer<K> keyNormalizer, PojoBytes pojoBytes) {
        return MapMemoizerFactory.create(keyNormalizer, pojoBytes);
    }

    private MapsMemoizers() {
    }
}
