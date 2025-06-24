package com.github.kjetilv.eda.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.time.chrono.Era;
import java.time.temporal.Temporal;
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
        HashBuilder<byte[]> hb = newBuilder.get();
        return hashAny(hb, leaf, anyHash).get();
    }

    private static HashBuilder<byte[]> hashAny(
        HashBuilder<byte[]> hb,
        Object leaf,
        ToIntFunction<Object> anyHash
    ) {
        return switch (leaf) {
            case String s -> hashString(T.STRING.tag(hb), s);
            case Boolean b -> hashString(T.BOOL.tag(hb), Boolean.toString(b));
            case BigDecimal b -> hashBigDecimal(T.BIG_DECIMAL.tag(hb), b);
            case BigInteger b -> hashBigInteger(T.BIG_INTEGER.tag(hb), b);
            case Number n -> hashNumber(T.NUMBER.tag(hb), n);
            case Temporal temporal -> hashTemporal(hb, temporal);
            case UUID u -> hashUUID(T.UUID.tag(hb), u);
            default -> hashLeaf(T.OBJECT.tag(hb), leaf, anyHash);
        };
    }

    private static HashBuilder<byte[]> hashNumber(HashBuilder<byte[]> hb, Number n) {
        return switch (n) {
            case Long l -> T.LONG.tag(hb).hash(Hashes.bytes(l));
            case Integer i -> T.INT.tag(hb).hash(Hashes.bytes(i));
            case Double d -> T.DOUBLE.tag(hb).hash(Hashes.bytes(Double.doubleToRawLongBits(d)));
            case Float f -> T.FLOAT.tag(hb).hash(Hashes.bytes(Float.floatToRawIntBits(f)));
            case Short s -> T.SHORT.tag(hb).hash(Hashes.bytes(s));
            case Byte b -> T.BYTE.tag(hb).hash(new byte[] {(byte) (int) b});
            default -> hashString(T.OTHER_NUMERIC.tag(hb), n.toString());
        };
    }

    private static HashBuilder<byte[]> hashTemporal(HashBuilder<byte[]> hb, TemporalAccessor t) {
        return switch (t) {
            case Instant i -> hashInstant(T.INSTANT.tag(hb), i);
            case ChronoLocalDate l -> hashNumber(T.LOCAL_DATE.tag(hb), l.toEpochDay());
            case ChronoLocalDateTime<?> l -> hashNumber(T.LOCAL_DATE_TIME.tag(hb), l.toEpochSecond(ZoneOffset.UTC));
            case ChronoZonedDateTime<?> z -> hashNumber(T.ZONED_DATETIME.tag(hb), z.toEpochSecond());
            case OffsetTime o -> hashNumber(T.OFFSET_TIME.tag(hb), o.toEpochSecond(LocalDate.EPOCH));
            case OffsetDateTime o -> hashNumber(T.OFFSET_DATETIME.tag(hb), o.toEpochSecond());
            case Year y -> hashNumber(T.YEAR.tag(hb), y.getValue());
            case YearMonth y -> hashNumber(T.YEAR_MONTH.tag(hb), y.getYear() * 12 + y.getMonthValue());
            case Month m -> hashNumber(T.MONTH.tag(hb), m.getValue());
            case MonthDay m -> hashNumber(T.MONTH_DAY.tag(hb), m.getMonthValue() * 12 + m.getDayOfMonth());
            case DayOfWeek d -> hashNumber(T.DAY_OF_WEEK.tag(hb), d.getValue());
            case Era e -> hashNumber(T.ERA.tag(hb), e.getValue());
            case TemporalAccessor ta -> hashString(T.OTHER_TEMPORAL.tag(hb), ta.toString());
        };
    }

    private static HashBuilder<byte[]> hashLeaf(
        HashBuilder<byte[]> hb,
        Object object,
        ToIntFunction<Object> anyHash
    ) {
        hb.<Integer>map(Hashes::bytes)
            .hash(System.identityHashCode(object.getClass()))
            .hash(anyHash.applyAsInt(object));
        return hb;
    }

    private static HashBuilder<byte[]> hashString(HashBuilder<byte[]> hb, String string) {
        return hb.hash(string.getBytes());
    }

    private static HashBuilder<byte[]> hashInstant(HashBuilder<byte[]> hb, Instant instant) {
        hb.<Long>map(Hashes::bytes)
            .hash(instant.getEpochSecond())
            .hash((long) instant.getNano());
        return hb;
    }

    private static HashBuilder<byte[]> hashUUID(HashBuilder<byte[]> hb, UUID uuid) {
        hb.<Long>map(Hashes::bytes)
            .hash(uuid.getMostSignificantBits())
            .hash(uuid.getLeastSignificantBits());
        return hb;
    }

    private static HashBuilder<byte[]> hashBigDecimal(HashBuilder<byte[]> hb, BigDecimal bigDecimal) {
        return hb.hash(bigDecimal.unscaledValue().toByteArray())
            .hash(Hashes.bytes(bigDecimal.scale()));
    }

    private static HashBuilder<byte[]> hashBigInteger(HashBuilder<byte[]> hashBuilder, BigInteger bigInteger) {
        return hashBuilder.hash(bigInteger.toByteArray());
    }

    private enum T {
        OBJECT,
        STRING,
        BOOL,
        NUMBER,
        DOUBLE,
        FLOAT,
        LONG,
        INT,
        SHORT,
        BYTE,
        BIG_DECIMAL,
        BIG_INTEGER,
        OTHER_NUMERIC,
        LOCAL_DATE,
        LOCAL_DATE_TIME,
        ZONED_DATETIME,
        OFFSET_TIME,
        OFFSET_DATETIME,
        YEAR,
        YEAR_MONTH,
        MONTH,
        MONTH_DAY,
        DAY_OF_WEEK,
        ERA,
        INSTANT,
        OTHER_TEMPORAL,
        UUID;

        private final byte[] bytes = new byte[] {(byte) ordinal()};

        HashBuilder<byte[]> tag(HashBuilder<byte[]> hb) {
            return hb.hash(bytes);
        }
    }
}
