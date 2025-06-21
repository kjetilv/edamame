package com.github.kjetilv.eda.impl;

import com.github.kjetilv.eda.hash.Hash;
import com.github.kjetilv.eda.hash.HashBuilder;
import com.github.kjetilv.eda.hash.Hashes;
import com.github.kjetilv.eda.hash.LeafHasher;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import static com.github.kjetilv.eda.impl.Type.*;
import static com.github.kjetilv.eda.impl.Type.DOUBLE;

public final class DefaultLeafHasher implements LeafHasher {

    private final Supplier<HashBuilder<byte[]>> newBuilder;

    private final ToIntFunction<Object> anyHash;

    public DefaultLeafHasher() {
        this(null, null);
    }

    public DefaultLeafHasher(Supplier<HashBuilder<byte[]>> newBuilder, ToIntFunction<Object> anyHash) {
        this.newBuilder = newBuilder == null ? Hashes::md5HashBuilder : newBuilder;
        this.anyHash = anyHash == null ? Object::hashCode : anyHash;
    }

    @Override
    public Hash hash(Object leaf) {
        return hash(newBuilder.get(), leaf, anyHash);
    }

    private static Hash hash(HashBuilder<byte[]> hb, Object leaf, ToIntFunction<Object> anyHash) {
        return (switch (leaf) {
            case String s -> hashString(STRING.tag(hb), s);
            case Boolean b -> hashString(BOOL.tag(hb), Boolean.toString(b));
            case BigDecimal b -> hashBigDecimal(BIG_DECIMAL.tag(hb), b);
            case BigInteger b -> hashBigInteger(BIG_INTEGER.tag(hb), b);
            case Number n -> hashNumber(NUMBER.tag(hb), n);
            case UUID uuid -> hashUUID(UUID.tag(hb), uuid);
            case TemporalAccessor a -> hashInstant(NUMBER.tag(hb), Instant.from(a));
            default -> hashLeaf(leaf, OBJECT.tag(hb), anyHash);
        }).get();
    }

    private static HashBuilder<byte[]> hashNumber(HashBuilder<byte[]> hb, Number n) {
        return switch (n) {
            case Double d -> DOUBLE.tag(hb).hash(Hashes.bytes(d));
            case Float f -> FLOAT.tag(hb).hash(Hashes.bytes(f));
            case Long l -> LONG.tag(hb).hash(Hashes.bytes(l));
            case Integer i -> INT.tag(hb).hash(Hashes.bytes(i));
            case Short s -> SHORT.tag(hb).hash(Hashes.bytes(s));
            case Byte b -> BYTE.tag(hb).hash(typeBytes(b));
            default -> hashString(OTHER_NUMERIC.tag(hb), n.toString());
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
}
