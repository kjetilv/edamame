package com.github.kjetilv.eda;

import java.util.Set;

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
    USE_SYSTEM_HC,
    /**
     * On completion, split to a new accessor and leave the mapmemoizer free to accept more maps
     */
    FORK_COMPLETE;

    public static boolean is(Option option, Option... options) {
        return Set.of(options).contains(option);
    }
}
