package rip.diamond.practice.spigot.spigotapi.util;

@FunctionalInterface
public interface TriConsumer<T, U, V> {
    void accept(T t, U u, V v);
}
