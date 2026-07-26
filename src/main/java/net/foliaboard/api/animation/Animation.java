package net.foliaboard.api.animation;

@FunctionalInterface
public interface Animation<T> {
    T current();
}
