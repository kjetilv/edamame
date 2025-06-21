package com.github.kjetilv.eda.impl;

import com.github.kjetilv.eda.MapMemoizer;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.github.kjetilv.eda.impl.MapMemoizerFactory.create;

public class MultiThreadedTest {

    @Test
    void test() {
        MapMemoizer<Object, CaKe> memoizer = create(key -> CaKe.get(key.toString()), null
        );

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
