package com.github.kjetilv.eda;

import com.github.kjetilv.eda.hash.HashBuilder;
import com.github.kjetilv.eda.hash.Hasher;
import com.github.kjetilv.eda.hash.Hashes;
import com.github.kjetilv.eda.impl.CanonicalMapBuilder;
import com.github.kjetilv.eda.impl.DefaultHasher;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.github.kjetilv.eda.MapMemoizers.Option.USE_SYSTEM_HC;
import static com.github.kjetilv.eda.MapMemoizers.Option.is;

@SuppressWarnings("unused")
public final class MapMemoizers {

    /**
     * String-keyed maps.
     *
     * @param options Options
     * @param <I>     Type of id's
     * @return {@link MapMemoizer} for String-keyed maps
     */
    public static <I> MapMemoizer<I, String> create(Option... options) {
        return create(null, Function.identity(), null, options);
    }

    /**
     * @param hb      Provider for new {@link HashBuilder hash builders}
     * @param options Options
     * @param <I>     Type of id's
     * @return {@link MapMemoizer} for String-keyed maps
     */
    public static <I> MapMemoizer<I, String> create(
        Supplier<HashBuilder<byte[]>> hb,
        Option... options
    ) {
        return create(hb, Object::toString, null, options);
    }

    /**
     * This method allows control over stored keys. Stored maps will be normalized to use {@link K}'s as map keys
     * on all levels. The {@code ck} argument provides a function that will produce (preferably) canonical {@link K}
     * instances, from the keys in incoming maps. Since {@link MapMemoizer} accepts {@code Map<?, ?>} and
     * this function needs to accept {@code ?} (any input) so it must handle whatever maps are thrown at it later.
     *
     * @param <I>           Id type
     * @param <K>           Key type
     * @param keyNormalizer Canonical key provider
     * @param options       Options
     * @return Map memoizer
     */
    public static <I, K> MapMemoizer<I, K> create(Function<?, K> keyNormalizer, Option... options) {
        return create(null, keyNormalizer, null, options);
    }

    /**
     * @param hasher  Hasher
     * @param options Options
     * @return Map memoizer
     * @see #create(Function, Option...)
     * @see #create(Supplier, Option...)
     */
    public static <I, K> MapMemoizer<I, K> create(
        Hasher hasher,
        Option... options
    ) {
        return create(null, null, hasher, options);
    }

    /**
     * @param keyNormalizer Canonical key provider, see {@link #create(Function, Option...)}
     * @param hasher        Hasher
     * @param options       Options
     * @return Map memoizer
     * @see #create(Function, Option...)
     * @see #create(Supplier, Option...)
     */
    public static <I, K> MapMemoizer<I, K> create(
        Function<?, K> keyNormalizer,
        Hasher hasher,
        Option... options
    ) {
        return create(null, keyNormalizer, hasher, options);
    }

    /**
     * @param hashBuilderSupplier Provider for new {@link HashBuilder hash builders}, see {@link #create(Supplier, Option...)}
     * @param keyNormalizer       Canonical key provider, see {@link #create(Function, Option...)}
     * @param options             Options
     * @param <I>                 Id type
     * @param <K>                 Key type
     * @return Map memoizer
     * @see #create(Function, Option...)
     * @see #create(Supplier, Option...)
     */
    public static <I, K> MapMemoizer<I, K> create(
        Supplier<HashBuilder<byte[]>> hashBuilderSupplier,
        Function<?, K> keyNormalizer,
        Hasher hasher,
        Option... options
    ) {
        return new CanonicalMapBuilder<>(
            hashBuilderSupplier == null
                ? Hashes::md5HashBuilder
                : hashBuilderSupplier,
            keyNormalizer == null
                ? Function.identity()
                : keyNormalizer,
            hasher == null
                ? new DefaultHasher(is(USE_SYSTEM_HC, options))
                : hasher,
            options
        );
    }

    private MapMemoizers() {
    }

    /**
     * Various ways to tweak behavior. Should not be necessary usually.
     */
    public enum Option {
        /**
         * Don't strip blank data (nulls, empty objects, empty lists)
         */
        KEEP_BLANKS,
        /**
         * Don't cache leaf values.
         */
        OMIT_LEAVES,
        /**
         * Don't remove unused nodes on {@link MapMemoizer#complete() completion}
         */
        OMIT_GC,
        /**
         * For leaf values without explicit hash logic, use {@link System#identityHashCode(Object) identity hash code}
         * for hashing.  This will work for objects that don't
         */
        USE_SYSTEM_HC;

        public static boolean is(Option option, Option... options) {
            return Set.of(options).contains(option);
        }
    }
}
