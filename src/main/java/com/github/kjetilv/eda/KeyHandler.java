package com.github.kjetilv.eda;

/**
 * Strategy interface for key handling.  {@link #normalize(Object) Normalizes} arbitrary objects into keys,
 * and provides a way to {@link #bytes(Object) byte-encode} keys for hashing purposes.
 * <p>
 * Maps will likely have {@link String} keys, or at least some key type which has a
 * {@link Object#toString() natural projection (ie. a toString)} onto strings. In this case,
 * use {@link MapsMemoizers#create() the default memoizer}.
 * <p>
 * Rationale: Those who avoid stringly typed code might want to use e.g. a single-value
 * {@link Record}, or an {@link Enum}. In such cases, {@link KeyHandler#normalize(Object) implement} and
 * {@link MapsMemoizers#create(KeyHandler) plug in this interface}
 * to produce instances of that key type.
 *
 * @param <K>
 * @see MapsMemoizers#create(KeyHandler)
 */
@FunctionalInterface
public interface KeyHandler<K> {

    @SuppressWarnings("unchecked")
    static <K> KeyHandler<K> defaultHandler() {
        return key -> (K) key.toString();
    }

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
    K normalize(Object key);
}
