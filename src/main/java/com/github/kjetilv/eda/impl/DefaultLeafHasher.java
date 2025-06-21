package com.github.kjetilv.eda.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

final class DefaultLeafHasher implements LeafHasher {

    private final Supplier<HashBuilder<byte[]>> newBuilder;

    private final ToIntFunction<Object> anyHash;

    DefaultLeafHasher(Supplier<HashBuilder<byte[]>> newBuilder, ToIntFunction<Object> anyHash) {
        this.newBuilder = Objects.requireNonNull(newBuilder, "newBuilder");
        this.anyHash = Objects.requireNonNull(anyHash, "anyHash");
    }

    @Override
    public Hash hash(Object leaf) {
        return hash(newBuilder.get(), leaf, anyHash);
    }

    private static Hash hash(HashBuilder<byte[]> hb, Object leaf, ToIntFunction<Object> anyHash) {
        return (switch (leaf) {
            case String s -> hashString(T.STRING.tag(hb), s);
            case Boolean b -> hashString(T.BOOL.tag(hb), Boolean.toString(b));
            case BigDecimal b -> hashBigDecimal(T.BIG_DECIMAL.tag(hb), b);
            case BigInteger b -> hashBigInteger(T.BIG_INTEGER.tag(hb), b);
            case Number n -> hashNumber(T.NUMBER.tag(hb), n);
            case UUID uuid -> hashUUID(T.UUID.tag(hb), uuid);
            case TemporalAccessor a -> hashInstant(T.NUMBER.tag(hb), Instant.from(a));
            default -> hashLeaf(leaf, T.OBJECT.tag(hb), anyHash);
        }).get();
    }

    private static HashBuilder<byte[]> hashNumber(HashBuilder<byte[]> hb, Number n) {
        return switch (n) {
            case Double d -> T.DOUBLE.tag(hb).hash(Hashes.bytes(d));
            case Float f -> T.FLOAT.tag(hb).hash(Hashes.bytes(f));
            case Long l -> T.LONG.tag(hb).hash(Hashes.bytes(l));
            case Integer i -> T.INT.tag(hb).hash(Hashes.bytes(i));
            case Short s -> T.SHORT.tag(hb).hash(Hashes.bytes(s));
            case Byte b -> T.BYTE.tag(hb).hash(typeBytes(b));
            default -> hashString(T.OTHER_NUMERIC.tag(hb), n.toString());
        };
    }

    private static HashBuilder<?> hashLeaf(Object object, HashBuilder<byte[]> hb, ToIntFunction<Object> hc) {
        return hb.hash(Hashes.bytes(hc.applyAsInt(object.getClass())));
    }

    private static HashBuilder<byte[]> hashString(HashBuilder<byte[]> hb, String string) {
        return hb.hash(string.getBytes());
    }

    private static HashBuilder<?> hashInstant(HashBuilder<byte[]> hb, Instant instant) {
        return hb.<Long>map(Hashes::bytes)
            .hash(instant.getEpochSecond())
            .hash((long) instant.getNano());
    }

    private static HashBuilder<?> hashUUID(HashBuilder<byte[]> hb, UUID uuid) {
        return hb.<Long>map(Hashes::bytes)
            .hash(uuid.getMostSignificantBits())
            .hash(uuid.getLeastSignificantBits());
    }

    private static HashBuilder<?> hashBigDecimal(HashBuilder<byte[]> hb, BigDecimal bigDecimal) {
        return hb.hash(bigDecimal.unscaledValue().toByteArray())
            .hash(Hashes.bytes(bigDecimal.scale()));
    }

    private static HashBuilder<?> hashBigInteger(HashBuilder<byte[]> hashBuilder, BigInteger bigInteger) {
        return hashBuilder.hash(bigInteger.toByteArray());
    }

    private static byte[] typeBytes(int i) {
        return new byte[] {(byte) i};
    }

    private enum T {

        STRING,
        BIG_DECIMAL,
        BIG_INTEGER,
        UUID,
        NUMBER,
        BOOL,
        OBJECT,
        TEMPORAL,
        DOUBLE,
        FLOAT,
        LONG,
        INT,
        SHORT,
        BYTE,
        OTHER_NUMERIC;

        private final byte[] bytes = new byte[] {(byte) ordinal()};

        HashBuilder<byte[]> tag(HashBuilder<byte[]> hb) {
            return hb.hash(bytes);
        }
    }
}
