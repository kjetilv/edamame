package com.github.kjetilv.eda.impl;

import com.github.kjetilv.eda.hash.Hash;
import com.github.kjetilv.eda.hash.HashBuilder;
import com.github.kjetilv.eda.hash.Hashes;
import com.github.kjetilv.eda.hash.LeafHasher;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

public final class DefaultLeafHasher implements LeafHasher {

    private final Supplier<HashBuilder<byte[]>> newBuilder;

    private final ToIntFunction<Object> anyHash;

    public DefaultLeafHasher() {
        this(null, null);
    }

    public DefaultLeafHasher(Supplier<HashBuilder<byte[]>> newBuilder, ToIntFunction<Object> anyHash) {
        this.newBuilder = newBuilder == null
            ? Hashes::md5HashBuilder
            : newBuilder;
        this.anyHash = anyHash == null
            ? Object::hashCode
            : anyHash;
    }

    @Override
    public Hash hash(Object leaf) {
        HashBuilder<?> hb = switch (leaf) {
            case String string -> hashString(hb().hash(STRING_TYPE), string);
            case BigDecimal bigDecimal -> hashBigDecimal(hb().hash(BIG_DEC_TYPE), bigDecimal);
            case BigInteger bigInteger -> hashBigInteger(hb().hash(BIG_INT_TYPE), bigInteger);
            case UUID uuid -> hashUUID(hb().hash(UUID_TYPE), uuid);
            case Number number -> hashNumber(hb().hash(NUMBER_TYPE), number);
            case Boolean bool -> hashString(hb().hash(BOOL_TYPE), Boolean.toString(bool));
            default -> hashAny(leaf, hb().hash(OBJECT_TYPE), anyHash);
        };
        return hb.get();
    }

    private HashBuilder<byte[]> hb() {
        return newBuilder.get();
    }

    private static final byte[] STRING_TYPE = typeBytes(0);

    private static final byte[] BIG_DEC_TYPE = typeBytes(1);

    private static final byte[] BIG_INT_TYPE = typeBytes(2);

    private static final byte[] UUID_TYPE = typeBytes(3);

    private static final byte[] NUMBER_TYPE = typeBytes(4);

    private static final byte[] BOOL_TYPE = typeBytes(5);

    private static final byte[] OBJECT_TYPE = typeBytes(13);

    private static final byte[] DOUBLE_TYPE = typeBytes(6);

    private static final byte[] FLOAT_TYPE = typeBytes(7);

    private static final byte[] LONG_TYPE = typeBytes(8);

    private static final byte[] INT_TYPE = typeBytes(9);

    private static final byte[] SHORT_TYPE = typeBytes(10);

    private static final byte[] BYTE_TYPE = typeBytes(11);

    private static final byte[] OTHER_NUMBER_TYPE = typeBytes(12);

    private static HashBuilder<?> hashNumber(HashBuilder<byte[]> hb, Number number) {
        return switch (number) {
            case Double d -> hb.hash(DOUBLE_TYPE).hash(Hashes.bytes(d));
            case Float f -> hb.hash(FLOAT_TYPE).hash(Hashes.bytes(f));
            case Long l -> hb.hash(LONG_TYPE).hash(Hashes.bytes(l));
            case Integer i -> hb.hash(INT_TYPE).hash(Hashes.bytes(i));
            case Short s -> hb.hash(SHORT_TYPE).hash(Hashes.bytes(s));
            case Byte b -> hb.hash(BYTE_TYPE).hash(typeBytes(b));
            default -> hashString(hb.hash(OTHER_NUMBER_TYPE), number.toString());
        };
    }

    private static HashBuilder<?> hashAny(Object object, HashBuilder<byte[]> hb, ToIntFunction<Object> hc) {
        return hb.hash(Hashes.bytes(hc.applyAsInt(object.getClass())));
    }

    private static HashBuilder<?> hashString(HashBuilder<byte[]> hb, String object) {
        return hb.<String>map(String::getBytes).hash(object);
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

    private static HashBuilder<?> hashBigInteger(HashBuilder<byte[]> hashBuilder1, BigInteger bigInteger) {
        return hashBuilder1.hash(bigInteger.toByteArray());
    }

    private static byte[] typeBytes(int i) {
        return new byte[] {(byte) i};
    }
}
