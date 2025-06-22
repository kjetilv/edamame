package com.github.kjetilv.eda.impl;

import com.github.kjetilv.eda.Memoized;
import com.github.kjetilv.eda.KeyNormalizer;
import com.github.kjetilv.eda.Memoizer;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.github.kjetilv.eda.impl.MapMemoizerFactory.create;
import static org.junit.jupiter.api.Assertions.*;

class MemoizersTest {

    static HashBuilder<byte[]> md5HashBuilder() {
        return new DigestiveHashBuilder<>(new ByteDigest());
    }

    @Test
    void shouldHandleLeafCollisions() {
        Object bd = new BigDecimal("123.234");
        Object bi = new BigInteger("424242");
        LeafHasher leafHasher = collidingLeafHasher();

        Memoizer<Long, String> memoizer = create(null, leafHasher);

        memoizer.put(
            42L, Map.of(
                "zot2", bd,
                "zot1", bi
            )
        );
        BigDecimal bdCopy = new BigDecimal("123.234");
        BigInteger biCopy = new BigInteger("424242");
        memoizer.put(
            43L, Map.of(
                "zot2", bdCopy,
                "zot1", biCopy
            )
        );
        Memoized<Long, String> access = memoizer.complete();

        Map<String, ?> map42 = access.get(42L);
        Map<String, ?> map43 = access.get(43L);

        Object bi42 = map42.get("zot1");
        Object bd42 = map42.get("zot2");

        Object bi43 = map43.get("zot1");
        Object bd43 = map43.get("zot2");

        assertSame(bi, bi42);
        assertEquals(bi, bi42);
        assertSame(bd, bd42);
        assertEquals(bd, bd42);

        assertNotSame(bi, bi43);
        assertEquals(bi, bi43);
        assertNotSame(bd, bd43);
        assertEquals(bd, bd43);

//        assertSame(bd, access.get(43L).get("zot1"));
//        assertSame(bi, access.get(43L).get("zot2"));
    }

    @Test
    void shouldHandleCollisions() {
        Hash collider = randomHash();
        LeafHasher leafHasher = leaf ->
            leaf.equals("3") || leaf.equals("7")
                ? collider
                : new DefaultLeafHasher(
                    MemoizersTest::md5HashBuilder,
                    Object::hashCode
                ).hash(leaf);
        Memoizer<Long, String> cache = create(null, leafHasher);

        for (int i = 0; i < 10; i++) {
            cache.put((long) i, Map.of("foo", String.valueOf(i)));
        }
        Memoized<Long, String> access = cache.complete();
        for (int i = 0; i < 10; i++) {
            Map<String, String> reconstructed = Map.of("foo", String.valueOf(i));
            assertEquals(reconstructed, access.get((long) i));
        }
    }

    @Test
    void shouldRespectCanonicalKeys() {
        KeyNormalizer<CaKe> caKeKeyNormalizer = s -> CaKe.get(s.toString());
        Memoizer<Long, CaKe> cache = create(caKeKeyNormalizer, null);

        Map<String, Object> in42 = build42(zot1Zot2());
        Map<String, ? extends Number> hh0hh1 = hh0hh1();

        Map<String, Object> in43 = Map.of(
            "fooTop", "zot",
            "zot", zot1Zot2(),
            "a", hh0hh1
        );

        Map<String, ? extends Number> hh0hh2 = hh0hh1();
        Map<String, Object> in44 = Map.of(
            "fooTop", "zot",
            "zot", zot1Zot2(),
            "a", Map.of(
                "e1", 2,
                "2", hh0hh2
            )
        );
        Map<String, Object> in48 = build42(zot1Zot2());

        cache.put(42L, in42);
        cache.put(48L, in48);
        Map<CaKe, ?> out42 = cache.get(42L);
        Map<CaKe, ?> out42as48 = cache.get(48L);
        assertSame(
            cache.get(42L),
            out42as48,
            "Same structure should return same identity"
        );
        cache.put(43L, in43);
        cache.put(44L, in44);

        Memoized<Long, CaKe> access = cache.complete();
        Map<CaKe, ?> cake43 = access.get(43L);
        Map<CaKe, ?> cake44 = access.get(44L);

        assertSame(
            getKey(cake43, "zot"),
            getKey(cake44, "zot")
        );
        assertSame(
            getKey(cake43, "fooTop"),
            getKey(cake44, "fooTop")
        );

        assertEquals(
            hh0hh1(),
            in43.get("a")
        );
        assertEquals(
            in44.get("a"),
            Map.of(
                "e1", 2,
                "2", hh0hh1()
            )
        );

        assertEquals(
            getDeep(in44, "a", "2"),
            hh0hh1()
        );

        assertNotSame(
            in42.get("zotCopy"),
            in43.get("zot")
        );

        assertSame(
            out42.get(CaKe.get("zot")),
            cake43.get(CaKe.get("zotCopy"))
        );

        assertSame(
            cake43.get(CaKe.get("zotCopy")),
            out42.get(CaKe.get("zot"))
        );

        Map<CaKe, ?> stringMap42 = access.get(42L);
        Map<CaKe, ?> stringMap43 = access.get(43L);
        Map<CaKe, ?> stringMap44 = access.get(44L);
        Map<CaKe, ?> stringMap44a = access.get(44L);

        assertSame(stringMap44, stringMap44a);

        Object inner42 = stringMap42.get(CaKe.get("zotCopy"));
        Object inner43 = stringMap43.get(CaKe.get("zot"));
        assertSame(inner42, inner43);
    }

    @Test
    void shouldStripBlankData() {
        Memoizer<Long, String> cache = create(null, null);

        cache.put(
            42L,
            Map.of(
                "foo", "bar",
                "zot", Collections.emptyList(),
                "zip", Collections.emptyMap()
            )
        );

        cache.put(
            45L, Map.of(
                "foo", "bar",
                "zot", Collections.emptyList()
            )
        );
        Memoized<Long, String> access = cache.complete();

        assertEquals(
            Map.of("foo", "bar"),
            access.get(42L)
        );

        assertSame(
            access.get(42L),
            access.get(45L)
        );
    }

    @Test
    void shouldStringify() {
        KeyNormalizer<String> stringKeyNormalizer = s -> s.toString().intern();
        Memoizer<Long, String> cache = create(stringKeyNormalizer, null);

        cache.put(
            42L,
            Map.of(1, "bar")
        );
        cache.put(
            45L,
            Map.of(
                1, "bar",
                true, Collections.emptyList()
            )
        );
        Memoized<Long, String> access = cache.complete();
        Map<String, ?> out42 = access.get(42L);
        Map<String, ?> out45 = access.get(45L);
        assertEquals(
            Map.of("1", "bar"),
            access.get(42L)
        );

        assertSame(
            access.get(42L),
            out45
        );
        assertSame(
            out42,
            out45
        );
        assertSame(
            out42,
            access.get(45L)
        );
    }

    @Test
    void shouldIgnoreKeyOrder() {
        Memoizer<Long, String> cache = create(null, null);

        cache.put(
            42L,
            map(IntStream.range(0, 10))
        );
        cache.put(
            43L,
            map(IntStream.range(0, 10)
                .map(i -> 9 - i))
        );
        Memoized<Long, String> access = cache.complete();
        Map<String, ?> canon42 = access.get(42L);
        Map<String, ?> canon43 = access.get(43L);
        assertEquals(canon42, canon43);
        assertSame(canon42, canon43);
    }

    @Test
    void shouldPreserveListOrder() {
        Memoizer<Long, String> cache = create(null, null);

        cache.put(
            42L,
            Map.of(
                "foo", IntStream.range(0, 10).mapToObj(String::valueOf)
                    .toList()
            )
        );
        cache.put(
            43L,
            Map.of(
                "foo", IntStream.range(0, 10)
                    .map(i -> 9 - i).mapToObj(String::valueOf)
                    .toList()
            )
        );
        Memoized<Long, String> access = cache.complete();
        Map<String, ?> canon42 = access.get(42L);
        Map<String, ?> canon43 = access.get(43L);
        assertNotEquals(canon42, canon43);
    }

    @Test
    void shouldPreserveIdentities() {
        Memoizer<Long, String> cache = create(null, null);
        Map<String, Object> in42 = build42(zot1Zot2());
        Map<String, ? extends Number> hh0hh1 = hh0hh1();
        Map<String, Object> in43 = Map.of(
            "fooTop", "zot",
            "zot", zot1Zot2(),
            "a", hh0hh1
        );
        Map<String, ? extends Number> hh0hh2 = hh0hh1();
        Map<String, Object> in44 = Map.of(
            "fooTop", "zot",
            "zot", zot1Zot2(),
            "a", Map.of(
                "e1", 2,
                "2", hh0hh2
            )
        );

        cache.put(42L, in42);
        cache.put(48L, build42(zot1Zot2()));
        cache.put(43L, in43);
        cache.put(44L, in44);

        Memoized<Long, String> access = cache.complete();
        Map<String, ?> out42as48 = access.get(48L);
        Map<String, ?> out42 = access.get(42L);

        assertSame(
            out42,
            out42as48,
            "Same structure should return same identity"
        );
        Map<String, ?> out43 = access.get(43L);
        assertEquals(
            hh0hh1(),
            in43.get("a")
        );
        assertEquals(
            in44.get("a"),
            Map.of(
                "e1", 2,
                "2", hh0hh1()
            )
        );

        assertEquals(
            getDeep(in44, "a", "2"),
            hh0hh1()
        );

        assertNotSame(
            in42.get("zotCopy"),
            in43.get("zot")
        );

        assertSame(
            out42.get("zot"),
            out43.get("zotCopy")
        );

        assertSame(
            out43.get("zotCopy"),
            out42.get("zot")
        );

        Map<String, ?> stringMap42 = access.get(42L);
        Map<String, ?> stringMap43 = access.get(43L);
        Map<String, ?> stringMap44 = access.get(44L);
        Map<String, ?> stringMap44a = access.get(44L);

        assertSame(stringMap44, stringMap44a);

        assertSame(
            stringMap42.get("zotCopy"),
            stringMap43.get("zot")
        );
        assertEquals(
            hh0hh1(),
            getDeep(stringMap44, "a", "2")
        );
        assertSame(
            stringMap43.get("a"),
            getDeep(stringMap44, "a", "2")
        );
    }

    @Test
    void shouldCanonicalizeLeaves() {
        Memoizer<Long, String> cache = create(null, null);

        BigDecimal bd = new BigDecimal("123.234");
        BigInteger bi = new BigInteger("424242");

        cache.put(
            42L, Map.of(
                "zot2", bd,
                "zot1", bi
            )
        );
        cache.put(
            43L, Map.of(
                "zot2", new BigDecimal("123.234"),
                "zot1", new BigInteger("424242")
            )
        );
        Memoized<Long, String> access = cache.complete();

        assertSame(bd, access.get(42L).get("zot2"));
        assertSame(bi, access.get(42L).get("zot1"));

        assertSame(bd, access.get(43L).get("zot2"));
        assertSame(bi, access.get(43L).get("zot1"));
    }

    private static LeafHasher collidingLeafHasher() {
        Hash collider = randomHash();
        return leaf -> collider;
    }

    private static Hash randomHash() {
        return new DigestiveHashBuilder<byte[]>(new ByteDigest())
            .<String>map(String::getBytes)
            .hash(UUID.randomUUID().toString()).get();
    }

    private static Map<String, Object> map(IntStream intStream) {
        return intStream
            .mapToObj(i -> Map.entry(String.valueOf(i), i))
            .collect(
                Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static Map<String, Object> build42(Map<String, ? extends Serializable> zot1Zot2) {
        return Map.of(
            "fooTop", "bar",
            "zotCopy", zot1Zot2
        );
    }

    private static Map<String, ? extends Number> hh0hh1() {
        return Map.of(
            "hh0", 1,
            "hh1", new BigDecimal("5.25")
        );
    }

    private static Map<String, ? extends Serializable> zot1Zot2() {
        return Map.of(
            "zot2", true,
            "zot1", 5
        );
    }

    @SuppressWarnings("SameParameterValue")
    private static CaKe getKey(Map<CaKe, ?> map, String key) {
        return map.keySet()
            .stream()
            .filter(k -> k.key().equals(key))
            .findFirst()
            .orElseThrow();
    }

    @SuppressWarnings({"unchecked", "SameParameterValue"})
    private static <K> Object getDeep(Map<K, ?> stringMap44, K one, K two) {
        return ((Map<K, ?>) stringMap44.get(one)).get(two);
    }
}