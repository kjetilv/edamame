package com.github.kjetilv.eda.impl;

import java.util.Base64;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

final class Hashes {

    static Hash of(long[] ls) {
        return new Hash(ls[0], ls[1]);
    }

    static String digest(Hash hash) {
        byte[] bytes = new byte[16];
        longToBytes(hash.l0(), 0, bytes);
        longToBytes(hash.l1(), 8, bytes);
        return digest(bytes);
    }

    static Hash hash(byte[] bytes) {
        return toHash(bytes);
    }

    static byte[] bytes(int i) {
        byte[] bytes = new byte[4];
        intToBytes(i, 0, bytes);
        return bytes;
    }

    static byte[] bytes(short i) {
        byte[] bytes = new byte[4];
        shortToBytes(i, 0, bytes);
        return bytes;
    }

    static byte[] bytes(long l) {
        byte[] bytes = new byte[8];
        longToBytes(l, 0, bytes);
        return bytes;
    }

    static byte[] toBytes(long[] ls) {
        byte[] bytes = new byte[16];
        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) (ls[0] >>> 8 * (7 - i));
        }
        for (int i = 0; i < 8; i++) {
            bytes[8 + i] = (byte) (ls[1] >>> 8 * (7 - i));
        }
        return bytes;
    }

    private Hashes() {
    }

    static final Hash NULL = new Hash(0L, 0L);

    private static final String PADDING = "==";

    private static final int DIGEST_LEN = 22;

    private static final int RAW_LEN = DIGEST_LEN + PADDING.length();

    private static final Base64.Encoder ENCODER = Base64.getEncoder();

    private static Hash toHash(byte[] bytes) {
        long[] ls = new long[2];
        for (int i = 0; i < 8; i++) {
            ls[0] <<= 8;
            ls[0] |= bytes[i] & 0xFF;
            ls[1] <<= 8;
            ls[1] |= bytes[i + 8] & 0xFF;
        }
        return of(ls);
    }

    private static String digest(byte[] bytes) {
        String base64 = new String(ENCODER.encode(bytes), ISO_8859_1);
        if (base64.length() == RAW_LEN && base64.endsWith(PADDING)) {
            return base64.substring(0, DIGEST_LEN)
                .replace('/', '-')
                .replace('+', '_');
        }
        throw new IllegalStateException("Unusual hash: " + base64);
    }

    @SuppressWarnings("SameParameterValue")
    private static void shortToBytes(short s, int index, byte[] bytes) {
        int w = s;
        bytes[index + 1] = (byte) (w & 0xFF);
        w >>= 8;
        bytes[index] = (byte) (w & 0xFF);
    }

    @SuppressWarnings("SameParameterValue")
    private static void intToBytes(int l, int index, byte[] bytes) {
        long w = l;
        bytes[index + 3] = (byte) (w & 0xFF);
        w >>= 8;
        bytes[index + 2] = (byte) (w & 0xFF);
        w >>= 8;
        bytes[index + 1] = (byte) (w & 0xFF);
        w >>= 8;
        bytes[index] = (byte) (w & 0xFF);
    }

    private static void longToBytes(long i, int index, byte[] bytes) {
        long w = i;
        for (int j = 7; j > 0; j--) {
            bytes[index + j] = (byte) (w & 0xFF);
            w >>= 8;
        }
        bytes[index] = (byte) (w & 0xFF);
    }
}
