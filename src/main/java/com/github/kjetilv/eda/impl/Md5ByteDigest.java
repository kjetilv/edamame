package com.github.kjetilv.eda.impl;

import java.security.MessageDigest;

final class Md5ByteDigest implements ByteDigest {

    private final MessageDigest messageDigest;

    Md5ByteDigest() {
        this.messageDigest = messageDigest(MD5);
    }

    @Override
    public void accept(byte[] bytes) {
        messageDigest.update(bytes);
    }

    @Override
    public Hash get() {
        return Hashes.hash(messageDigest.digest());
    }

    private static final String MD5 = "MD5";

    @SuppressWarnings("SameParameterValue")
    private static MessageDigest messageDigest(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (Exception e) {
            throw new IllegalStateException("Expected " + algorithm + " implementation", e);
        }
    }
}
