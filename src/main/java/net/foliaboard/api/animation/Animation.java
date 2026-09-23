package net.foliaboard.api.animation;

import java.util.function.Function;

@FunctionalInterface
public interface Animation<T> {
    T current();

    default <R> Animation<R> map(Function<? super T, ? extends R> mapper) {
        return () -> mapper.apply(current());
    }

    static <T> Animation<T> constant(T value) {
        return () -> value;
    }
}
