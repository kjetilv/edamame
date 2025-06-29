package com.github.kjetilv.eda;

/**
 * Strategy interface for key handling.  {@link #normalize(Object) Normalizes} arbitrary objects into keys,
 * and provides a way to {@link #bytes(Object) byte-encode} keys for hashing purposes.
 * <p>
 * Maps will likely have {@link String} keys, or at least some key type which has a
 * {@link Object#toString() natural projection} onto strings – this is fancy for "implements toString". In this case,
 * use {@link MapsMemoizers#create() the default memoizer}.
 * <p>
 * Rationale: You may be the type to avoid stringly typed code, using e.g. a single-value
 * {@link Record}, or an {@link Enum}. In such cases, {@link KeyHandler#normalize(Object) implement} this interface and
 * {@link MapsMemoizers#create(KeyHandler) plug it in} to produce instances of that key type.
 *
 * @param <K> Key type
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
     * <p>
     * Note that this method must accept {@link Object any} value. The {@link MapsMemoizer} needs to work
     * on {@code Map<?, ?>}, so it is up to the handler to resolve maps' keys into {@link K}'s.
     *
     * @param key Key
     * @return A K instance
     */
    K normalize(Object key);
}
