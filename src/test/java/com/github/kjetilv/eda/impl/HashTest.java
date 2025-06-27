package com.github.kjetilv.eda.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HashTest {

    @Test
    void testNull() {
        assertEquals(Hash.NULL, Hashes.of(0L, 0L));
    }

    @Test
    void testHash() {
        Hash hash = Hashes.of(123L, 234L);
        assertEquals("⟨AAAAAAAA⟩", hash.toString());
    }
}