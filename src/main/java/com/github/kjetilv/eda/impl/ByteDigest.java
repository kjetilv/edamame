package com.github.kjetilv.eda.impl;

import java.security.MessageDigest;
import java.util.function.Consumer;

/**
 * A nicer wrapper for {@link MessageDigest MD5} digests.
 */
final class ByteDigest implements Consumer<byte[]> {

    private final MessageDigest messageDigest;

    ByteDigest() {
        this.messageDigest = messageDigest();
    }

    @Override
    public void accept(byte[] bytes) {
        messageDigest.update(bytes);
    }

    /**
     * Get the hash and reset the {@link MessageDigest digest}.
     *
     * @return Hash
     */
    public Hash hash() {
        return Hash.of(messageDigest.digest());
    }

    private static MessageDigest messageDigest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            throw new IllegalStateException("Expected MD5 implementation", e);
        }
    }
}
