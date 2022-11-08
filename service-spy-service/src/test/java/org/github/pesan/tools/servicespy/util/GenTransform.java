package org.github.pesan.tools.servicespy.util;

import org.quicktheories.api.Pair;
import org.quicktheories.core.Gen;

@FunctionalInterface
public
interface GenTransform<T, R> {
    static <T> GenTransform<T, T> id() {
        return n -> n;
    }

    static <T> GenTransform<T, Pair<T, T>> paired() {
        return gen -> prng -> Pair.of(gen.generate(prng), gen.generate(prng));
    }

    Gen<R> apply(Gen<T> gen);
}
