package com.github.kjetilv.eda;

/**
 * Most of the time, maps will have {@link String} keys, or at least some key type which has
 * a natural projection onto {@link String}, typically via {@link Object#toString()}. In these
 * cases, {@link #keyToString()} will suffice and is also the default.
 * <p>
 * In other cases, one might want e.g. a custom single-field record type (typically to reduce
 * stringly typed code), or an enum, etc.  In such cases, implement a {@link KeyNormalizer}
 * which {@link #toKey(Object) toKey's} the keys of input maps and normalizes them to an
 * instance of that key type.
 *
 * @param <K>
 */
@SuppressWarnings("unchecked")
@FunctionalInterface
public interface KeyNormalizer<K> {

    static <K> KeyNormalizer<K> keyToString() {
        return key -> (K) key.toString();
    }

    /**
     * Affects how maps are hashed wrt. their keys
     *
     * @param key Key
     * @return byte array for hashing
     */
    default byte[] bytes(K key) {
        return key.toString().getBytes();
    }

    /**
     * Normalises a map's key to a K instance.  The returned value will be caonicalized so that the next object with
     * the same key gets the same {@code K}.
     *
     * @param key Key
     * @return A K instance
     */
    K toKey(Object key);
}
