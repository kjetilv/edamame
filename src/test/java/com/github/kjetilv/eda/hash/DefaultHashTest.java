package com.github.kjetilv.eda.hash;

import com.github.kjetilv.eda.impl.Hash;
import com.github.kjetilv.eda.impl.Hashes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultHashTest {

    @Test
    void string() {
        assertEquals(10, Hashes.random().toString().length());
    }

    @Test
    void byteAt() {
        Hash random = Hashes.random();
        byte[] bytes = random.bytes();
        for (int i = 0; i < 16; i++) {
            int finalI = i;
            assertEquals(
                bytes[i], random.byteAt(i),
                () -> "Diasagree on " + finalI
            );
        }
    }
}