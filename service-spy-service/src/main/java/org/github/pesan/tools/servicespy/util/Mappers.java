package org.github.pesan.tools.servicespy.util;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class Mappers {
    private Mappers() {}

    public interface EnumMapper<M extends Enum<M>, D extends Enum<D>> {
        M toModel(D d);
        D fromModel(M d);
    }

    static <K, V> Function<K, Optional<V>> fromMap(Map<? super K, ? extends V> map) {
        return key -> Optional.ofNullable(map.get(key));
    }

    static <F extends Enum<F>, T extends Enum<T>> Function<T, Optional<F>> reverse(Class<F> fromType, Class<T> toType, Function<? super F, ? extends T> mapper) {
        EnumMap<T, F> reverse = Arrays.stream(fromType.getEnumConstants())
                .collect(Collectors.toMap(
                        mapper,
                        Function.identity(),
                        (a, b) -> {throw new IllegalArgumentException();},
                        () -> new EnumMap<>(toType)
                ));

        return t -> Optional.ofNullable(reverse.get(t));
    }

    static <M extends Enum<M>, D extends Enum<D>> EnumMapper<M, D> usingModel(Class<M> modelType, Class<D> domainType, Function<? super M, ? extends D> mapper) {
        EnumMap<D, M> reverse = Arrays.stream(modelType.getEnumConstants())
                .collect(Collectors.toMap(
                        mapper,
                        Function.identity(),
                        (a, b) -> {throw new IllegalArgumentException();},
                        () -> new EnumMap<>(domainType)
                ));

        return new EnumMapper<>() {
            @Override
            public M toModel(D d) {
                return reverse.get(d);
            }

            @Override
            public D fromModel(M d) {
                return mapper.apply(d);
            }
        };
    }

    static <M extends Enum<M>, D extends Enum<D>> EnumMapper<M, D> usingDomain(Class<D> domainType, Class<M> modelType, Function<? super D, ? extends M> mapper) {
        EnumMap<M, D> reverse = Arrays.stream(domainType.getEnumConstants())
                .collect(Collectors.toMap(
                        mapper,
                        Function.identity(),
                        (a, b) -> {throw new IllegalArgumentException();},
                        () -> new EnumMap<>(modelType)
                ));

        return new EnumMapper<>() {
            @Override
            public M toModel(D d) {
                return mapper.apply(d);
            }

            @Override
            public D fromModel(M d) {
                return reverse.get(d);
            }
        };
    }

    public static <T, R> List<R> mapList(List<T> list, Function<? super T, ? extends R> mapper) {
        return list.stream().map(mapper).collect(toList());
    }

    public static Single<byte[]> bufferChunks(Flowable<byte[]> chunks) {
        return chunks
                .toList()
                .map(chunkList -> {
                    int pos = 0;
                    byte[] result = new byte[chunkList.stream().mapToInt(n -> n.length).sum()];
                    for (byte[] i : chunkList) {
                        System.arraycopy(i, 0, result, pos, i.length);
                        pos += i.length;
                    }
                    return result;
                });
    }
}
