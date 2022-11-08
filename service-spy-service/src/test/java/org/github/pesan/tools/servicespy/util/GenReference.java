package org.github.pesan.tools.servicespy.util;

import org.quicktheories.core.Gen;
import org.quicktheories.core.RandomnessSource;

import java.util.concurrent.atomic.AtomicReference;

public class GenReference<T> implements Gen<T> {
    private final AtomicReference<Gen<T>> ref = new AtomicReference<>();

    public static <T> GenReference<T> create() {
        return new GenReference<>();
    }

    public void set(Gen<T> gen) {
        ref.set(gen);
    }

    @Override
    public T generate(RandomnessSource in) {
        return ref.get().generate(in);
    }
}
