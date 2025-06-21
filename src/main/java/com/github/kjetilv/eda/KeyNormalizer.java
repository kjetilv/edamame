package com.github.kjetilv.eda;

/**
 * Most of the time, maps will have {@link String} keys, or at least some key type which has
 * a natural projection onto {@link String}. In this case, use
 * {@link MapMemoizers#create() the default memoizer}.
 * <p>
 * Oftentimes, to avoid stringly typed code one might want to use e.g. a single-value
 * {@link Record} or an {@link Enum}. In such cases, {@link KeyNormalizer#toKey(Object) implement} and
 * {@link MapMemoizers#create(KeyNormalizer) plug in this interface}
 * to produce instances of that key type.
 *
 * @param <K>
 * @see MapMemoizers#create(KeyNormalizer)
 */
@FunctionalInterface
public interface KeyNormalizer<K> {

    /**
     * Affects how maps are hashed wrt. their keys.  Default implementation is to get the
     * bytes of its {@link Object#toString()}.
     *
     * @param key Key
     * @return byte array for hashing
     */
    default byte[] bytes(K key) {
        return key.toString().getBytes();
    }

    /**
     * Normalises a map's key to a K instance.  The returned value will be canonicalized
     * so that the same key gets the same {@code K} instance.
     *
     * @param key Key
     * @return A K instance
     */
    K toKey(Object key);
}
