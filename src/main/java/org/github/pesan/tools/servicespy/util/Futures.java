package org.github.pesan.tools.servicespy.util;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;

public class Futures {
    @FunctionalInterface public interface Func0<R> { R apply(); }
    @FunctionalInterface public interface Func1<T1, R> { R apply(T1 t1); default Func0<R> provide(T1 t1) { return () -> apply(t1); } }
    @FunctionalInterface public interface Func2<T1, T2, R> { R apply(T1 t1, T2 t2); default Func1<T1, R> provide(T2 t2) { return (t1) -> apply(t1, t2); }}
    @FunctionalInterface public interface Func3<T1, T2, T3, R> { R apply(T1 t1, T2 t2, T3 t3); default Func2<T1, T2, R> provide(T3 t3) { return (t1, t2) -> apply(t1, t2, t3); }}
    @FunctionalInterface public interface Func4<T1, T2, T3, T4, R> { R apply(T1 t1, T2 t2, T3 t3, T4 t4); default Func3<T1, T2, T3, R> provide(T4 t4) { return (t1, t2, t3) -> apply(t1, t2, t3, t4); }}
    @FunctionalInterface public interface Func5<T1, T2, T3, T4, T5, R> { R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5); default Func4<T1, T2, T3, T4, R> provide(T5 t5) { return (t1, t2, t3, t4) -> apply(t1, t2, t3, t4, t5); }}
    /*@FunctionalInterface public interface Func6<T1, T2, T3, T4, T5, T6, R> { R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6); default Func5<T1, T2, T3, T4, T5, R> provide(T6 t6) { return (t1, t2, t3, t4, t5) -> apply(t1, t2, t3, t4, t5, t6); }}
    @FunctionalInterface public interface Func7<T1, T2, T3, T4, T5, T6, T7, R> { R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7); default Func6<T1, T2, T3, T4, T5, T6, R> provide(T7 t7) { return (t1, t2, t3, t4, t5, t6) -> apply(t1, t2, t3, t4, t5, t6, t7); }}
    @FunctionalInterface public interface Func8<T1, T2, T3, T4, T5, T6, T7, T8, R> { R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8); default Func7<T1, T2, T3, T4, T5, T6, T7, R> provide(T8 t8) { return (t1, t2, t3, t4, t5, t6, t7) -> apply(t1, t2, t3, t4, t5, t6, t7, t8); }}
    @FunctionalInterface public interface Func9<T1, T2, T3, T4, T5, T6, T7, T8, T9, R> { R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8, T9 t9); default Func8<T1, T2, T3, T4, T5, T6, T7, T8, R> provide(T9 t9) { return (t1, t2, t3, t4, t5, t6, t7, t8) -> apply(t1, t2, t3, t4, t5, t6, t7, t8, t9); }}
    */

    public static <R, T1, T2> Future<R> zip(Future<T1> f1, Future<T2> f2, Func2<T1, T2, R> combiner) { return CompositeFuture.all(f1, f2).map(e -> combiner.apply(e.resultAt(0), e.resultAt(1))); }
    public static <R, T1, T2, T3> Future<R> zip(Future<T1> f1, Future<T2> f2, Future<T3> f3, Func3<T1, T2, T3, R> combiner) { return CompositeFuture.all(f1, f2, f3).map(e -> combiner.apply(e.resultAt(0), e.resultAt(1), e.resultAt(2))); }
    public static <R, T1, T2, T3, T4> Future<R> zip(Future<T1> f1, Future<T2> f2, Future<T3> f3, Future<T4> f4, Func4<T1, T2, T3, T4, R> combiner) { return CompositeFuture.all(f1, f2, f3, f4).map(e -> combiner.apply(e.resultAt(0), e.resultAt(1), e.resultAt(2), e.resultAt(3))); }
    public static <R, T1, T2, T3, T4, T5> Future<R> zip(Future<T1> f1, Future<T2> f2, Future<T3> f3, Future<T4> f4, Future<T5> f5, Func5<T1, T2, T3, T4, T5, R> combiner) { return CompositeFuture.all(f1, f2, f3, f4, f5).map(e -> combiner.apply(e.resultAt(0), e.resultAt(1), e.resultAt(2), e.resultAt(3), e.resultAt(4))); }
}