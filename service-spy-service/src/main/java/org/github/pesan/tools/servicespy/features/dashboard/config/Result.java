package org.github.pesan.tools.servicespy.features.dashboard.config;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class Result<T, E> {

    private final T t;
    private final E e;

    private Result(T t, E e) {
        if (e == null) {
            Objects.requireNonNull(t);
        } else if (t == null) {
            Objects.requireNonNull(e);
        } else {
            throw new IllegalArgumentException("A Result need to be either successful or failed");
        }
        this.t = t;
        this.e = e;
    }

    public static <T, E> Result<T, E> success(T value) {
        return new Result<>(value, null);
    }

    public static <T, E> Result<T, E> error(E value) {
        return new Result<>(null, value);
    }

    public static <T, E> Result<T, E> fromOptional(Optional<T> optional, E error) {
        return fromOptional(optional, () -> error);
    }

    public static <T, E> Result<T, E> fromOptional(Optional<T> optional, Supplier<E> error) {
        return optional.map(Result::<T, E>success).orElseGet(() -> Result.error(error.get()));
    }

    public <T2> Result<T2, E> map(Function<? super T, ? extends T2> mapper) {
        return t != null
                ? success(mapper.apply(t))
                : cast(this);
    }

    public <E2> Result<T, E2> mapError(Function<? super E, ? extends E2> mapper) {
        return e != null
                ? error(mapper.apply(e))
                : cast(this);
    }

    @SafeVarargs
    static <T, R> Function<T, R> matching(Function<T, Optional<R>>... fs) {
        return t -> Arrays.stream(fs)
                .flatMap(f -> f.apply(t).stream())
                .findFirst()
                .orElseThrow();
    }

    public static <T, R, W extends T> Function<T, Optional<R>> type(Class<W> type, Function<? super W, ? extends R> mapper) {
        return t -> type.isInstance(t) ? Optional.of(mapper.apply((W)t)) : Optional.empty();
    }

    public static <T, R> Function<T, Optional<R>> otherwise(Supplier<? extends R> mapper) {
        return t -> Optional.of(mapper.get());
    }

    public <T2, E2> Result<? extends T2, ? extends E2> bimap(
            Function<? super T, ? extends Result<? extends T2, ? extends E2>> mapper,
            Function<? super E, ? extends Result<? extends T2, ? extends E2>> errorMapper
    ) {
        return cast(new Result<>(
                t != null ? mapper.apply(t) : null,
                e != null ? errorMapper.apply(e) : null
        ));
    }

    public <T2> Result<T2, ? extends E> flatMap(Function<? super T, Result<T2, ? extends E>> mapper) {
        return t != null ? mapper.apply(t) : cast(this);
    }

    public Optional<T> toOptional() {
        return handle(Optional::of, e -> Optional.empty());
    }

    public Stream<T> stream() {
       return handle(Stream::of, __ -> Stream.empty());
    }

    public boolean isSuccess() {
        return t != null;
    }

    public <R> R handle(Function<? super T, ? extends R> success,
                 Function<? super E, ? extends R> error) {
        return t != null ? success.apply(t) : error.apply(e);
    }

    public <X extends Throwable> T orElseThrow(Function<E, ? extends X> exceptionMapper) throws X {
        if (e != null)
            throw exceptionMapper.apply(e);
        return t;
    }

    @SuppressWarnings("unchecked")
    private static <T1, E1, T2, E2> Result<T2, E2> cast(Result<? extends T1, ? extends E1> result) {
        return (Result<T2, E2>) result;
    }
}

