package rip.diamond.practice.spigot.spigotapi;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import rip.diamond.practice.spigot.spigotapi.knockback.AbstractKnockback;
import rip.diamond.practice.spigot.spigotapi.knockback.impl.*;
import rip.diamond.practice.spigot.spigotapi.movementhandler.AbstractMovementHandler;
import rip.diamond.practice.spigot.spigotapi.movementhandler.impl.*;

import java.util.Arrays;

@AllArgsConstructor
public enum SpigotType {

    SPIGOT("org.spigotmc.SpigotConfig", rip.diamond.practice.spigot.spigotapi.knockback.impl.DefaultKnockback.class, DefaultMovementHandler.class),
    CARBON_SPIGOT("xyz.refinedev.spigot.knockback.KnockbackProfile", CarbonSpigotKnockback.class, CarbonSpigotMovementHandler.class),
    WIND_SPIGOT("ga.windpvp.windspigot.WindSpigot", rip.diamond.practice.spigot.spigotapi.knockback.impl.WindSpigotKnockback.class, DefaultMovementHandler.class),
    ;

    private final String package_;
    public final Class<?> knockback;
    public final Class<?> movementHandler;

    public String getPackage() {
        return package_;
    }

    /**
     * Detect which spigot is being used and initialize
     * @author Drizzy
     */
    public static SpigotType get() {
        return Arrays
                .stream(SpigotType.values())
                .filter(type -> !type.equals(SpigotType.SPIGOT) && check(type.getPackage()))
                .findFirst()
                .orElse(SpigotType.SPIGOT);
    }

    @SneakyThrows
    public static AbstractKnockback getKnockback() {
        return (AbstractKnockback) get().knockback.newInstance();
    }

    @SneakyThrows
    public static AbstractMovementHandler getMovementHandler() {
        return (AbstractMovementHandler) get().movementHandler.newInstance();
    }

    public static boolean check(String string) {
        try {
            Class.forName(string);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
