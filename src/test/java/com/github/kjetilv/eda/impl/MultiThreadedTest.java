package com.github.kjetilv.eda.impl;

import com.github.kjetilv.eda.MapsMemoizer;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.kjetilv.eda.impl.MapMemoizerFactory.create;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MultiThreadedTest {

    @Test
    void test() {
        MapsMemoizer<Object, CaKe> mapsMemoizer = create(key -> CaKe.get(key.toString()), null);
        AtomicBoolean complete = new AtomicBoolean();
        CompletableFuture<Void> voider = CompletableFuture.runAsync(() -> {
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 100; j++) {
                    Map<CaKe, Object> caKeMap = map(i, j);
                    zz(1);
                    mapsMemoizer.putIfAbsent(i * 100 + j, caKeMap);
                }
            }
            mapsMemoizer.complete();
            complete.set(true);
        });

        int comparisons = 0;
        for (int x = 0; x < 5 && !complete.get(); x++) {
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 100; j++) {
                    zz(1);
                    Map<CaKe, ?> map = mapsMemoizer.get(i * 100 + j);
                    if (map != null) {
                        comparisons++;
                        Map<CaKe, Object> cake = map(i, j);
                        assertEquals(cake.keySet(), map.keySet());
                        cake.keySet().forEach(key -> {
                            Map<CaKe, ?> cakeValue = (Map<CaKe, ?>) cake.get(key);
                            Map<CaKe, ?> mapValue = (Map<CaKe, ?>) map.get(key);
                            assertEquals(
                                new TreeMap<>(cakeValue).toString(),
                                new TreeMap<>(mapValue).toString(),
                                key::toString
                            );
                        });
                    }
                }
            }
        }
        voider.join();
        assertTrue(comparisons > 0);
    }

    private static void zz(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private static Map<CaKe, Object> map(int i, int j) {
        return Map.of(
            CaKe.get("foo-" + i + "-" + j),
            Map.of(
                "fooi", i,
                "fooj", j
            ),
            CaKe.get("foo-" + (i + j)),
            Map.of(
                "bar", i,
                "zot", j
            )
        );
    }
}
