package com.github.kjetilv.eda.impl;

import java.security.MessageDigest;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class ByteDigest implements Consumer<byte[]>, Supplier<Hash> {

    private final MessageDigest messageDigest;

    ByteDigest() {
        this.messageDigest = messageDigest();
    }

    @Override
    public void accept(byte[] bytes) {
        messageDigest.update(bytes);
    }

    @Override
    public Hash get() {
        return Hashes.hash(messageDigest.digest());
    }

    private static MessageDigest messageDigest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            throw new IllegalStateException("Expected MD5 implementation", e);
        }
    }
}
