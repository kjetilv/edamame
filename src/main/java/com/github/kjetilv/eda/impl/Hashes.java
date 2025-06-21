package com.github.kjetilv.eda.impl;

import java.util.Base64;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.util.Objects.requireNonNull;

public final class Hashes {

    public static Hash random() {
        UUID u1 = UUID.randomUUID();
        return of(
            u1.getMostSignificantBits(),
            u1.getLeastSignificantBits()
        );
    }

    public static String digest(Hash hash) {
        byte[] bytes = new byte[16];
        long[] ls = hash.ls();
        for (int l = 0; l < 2; l++) {
            longToBytes(ls[l], l * 8, bytes);
        }
        String base64 = new String(ENCODER.encode(bytes), ISO_8859_1);
        if (base64.length() == RAW_LEN && base64.endsWith(PADDING)) {
            return base64.substring(0, Hash.DIGEST_LEN)
                .replace(BAD_1, GOOD_1)
                .replace(BAD_2, GOOD_2);
        }
        throw new IllegalStateException("Unusual hash: " + base64);
    }

    public static Hash of(long[] ls) {
        return new DefaultHash(ls[0], ls[1]);
    }

    public static Hash of(long l0, long l1) {
        return new DefaultHash(l0, l1);
    }

    public static byte[] bytes(Hash hash) {
        if (hash == null) {
            return null;
        }
        byte[] bytes = new byte[16];
        long[] ls = hash.ls();
        for (int l = 0; l < 2; l++) {
            int l8 = l * 8;
            for (int i = 0; i < 8; i++) {
                bytes[l8 + i] = (byte) (ls[l] >>> 8 * (7 - i));
            }
        }
        return bytes;
    }

    public static Hash hash(String raw) {
        requireNonNull(raw, "raw");
        int length = raw.length();
        if (length < Hash.DIGEST_LEN) {
            throw new IllegalArgumentException("Malformed: " + raw);
        }
        String digest = raw.substring(0, Hash.DIGEST_LEN)
            .replace(GOOD_1, BAD_1)
            .replace(GOOD_2, BAD_2);
        byte[] decoded = DECODER.decode(digest);
        long[] ls = new long[4];
        for (int l = 0; l < 2; l++) {
            ls[l] = bytesToLong(decoded, l * 8);
        }
        return of(ls);
    }

    public static Hash hash(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        if (bytes.length != 16) {
            throw new IllegalStateException("Expected 128 bits of hash: " + bytes.length + " bytes");
        }
        long[] ls = new long[2];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 2; j++) {
                ls[j] <<= 8;
                ls[j] |= bytes[i + j * 8] & 0xFF;
            }
        }
        return of(ls);
    }

    public static byte[] bytes(int i) {
        byte[] bytes = new byte[4];
        intToBytes(i, 0, bytes);
        return bytes;
    }

    public static byte[] bytes(short i) {
        byte[] bytes = new byte[4];
        shortToBytes(i, 0, bytes);
        return bytes;
    }

    public static byte[] bytes(double d) {
        return bytes(Double.doubleToRawLongBits(d));
    }

    public static byte[] bytes(float d) {
        return bytes(Float.floatToRawIntBits(d));
    }

    public static byte[] bytes(long l) {
        byte[] bytes = new byte[8];
        longToBytes(l, 0, bytes);
        return bytes;
    }

    public static byte[] bytes(long l1, long l2) {
        byte[] bytes = new byte[16];
        longToBytes(l1, 0, bytes);
        longToBytes(l2, 8, bytes);
        return bytes;
    }

    public static HashBuilder<byte[]> md5HashBuilder() {
        return new DigestiveHashBuilder<>(new Md5ByteDigest(), IDENTITY);
    }

    private Hashes() {
    }

    static final char GOOD_1 = '-';

    static final char GOOD_2 = '_';

    static final char BAD_2 = '+';

    static final char BAD_1 = '/';

    static final String PADDING = "==";

    private static final int RAW_LEN = Hash.DIGEST_LEN + PADDING.length();

    private static final Base64.Encoder ENCODER = Base64.getEncoder();

    private static final Base64.Decoder DECODER = Base64.getDecoder();

    private static final Function<byte[], Stream<byte[]>> IDENTITY = Stream::of;

    @SuppressWarnings("SameParameterValue")
    private static void shortToBytes(short s, int index, byte[] bytes) {
        int w = s;
        for (int j = 1; j > 0; j--) {
            bytes[index + j] = (byte) (w & 0xFF);
            w >>= 8;
        }
        bytes[index] = (byte) (w & 0xFF);
    }

    @SuppressWarnings("SameParameterValue")
    private static void intToBytes(int l, int index, byte[] bytes) {
        long w = l;
        for (int j = 3; j > 0; j--) {
            bytes[index + j] = (byte) (w & 0xFF);
            w >>= 8;
        }
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

    private static long bytesToLong(byte[] bytes, int start) {
        long lw = 0;
        for (int i = 0; i < 7; i++) {
            lw |= bytes[i + start] & 0xFF;
            lw <<= 8;
        }
        lw |= bytes[7 + start] & 0xFF;
        return lw;
    }
}
