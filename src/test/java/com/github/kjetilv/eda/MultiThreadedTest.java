package com.github.kjetilv.eda;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class MultiThreadedTest {

    @Test
    void test() {
        MapMemoizer<Object, CaKe> memoizer = MapMemoizers.create((KeyNormalizer<CaKe>) key ->
            CaKe.get(key.toString()));

        CompletableFuture<Void> voider = CompletableFuture.runAsync(() -> {
            for (int i = 0; i < 100; i++) {
                for (int j = 0; j < 100; j++) {
                    Map<String, Object> caKeMap = Map.of(
                        "foo-" + i + "-" + j,
                        Map.of(
                            "fooi", i,
                            "fooj", j
                        ),
                        "foo-" + (i + j),
                        Map.of(
                            "bar", i,
                            "zot", j
                        )
                    );
                    memoizer.put(i * 100 + j, caKeMap);
                }
            }
            memoizer.complete();
        });

        voider.join();
    }
}
