package com.github.kjetilv.eda;

import java.util.Set;

/**
 * Various ways to tweak behavior. Should not be necessary usually. Maybe throw this out and all the ifs and
 * buts with it?
 */
public enum Option {
    /**
     * Don't strip blank data (nulls, empty objects, empty lists).  Nothing comes in many forms, who are we
     * to judge?
     */
    KEEP_BLANKS,
    /**
     * Don't cache leaf values.  Maybe someone has some problem with leaves?
     */
    OMIT_LEAVES,
    /**
     * Don't remove unused nodes on {@link MapMemoizer#complete() completion}.  Maybe useful if speed is
     * more critical than memory – but then, why are you using
     */
    OMIT_GC,
    /**
     * For leaf values without explicit hashing support, use {@link System#identityHashCode(Object) identity hash code}
     * instead for hashing. This will work for objects that don't (really) obey pesky
     * {@link Object#equals(Object) equals}/{@link Object#hashCode() hashCode} contracts after all.
     */
    USE_SYSTEM_HC,
    /**
     * On completion, split to a new accessor and leave the mapmemoizer free to accept more maps. This could
     * even be useful.
     */
    FORK_COMPLETE;

    public static boolean is(Option option, Option... options) {
        return Set.of(options).contains(option);
    }
}
