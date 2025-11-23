package rip.diamond.practice.spigot.spigotapi.movementhandler;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import rip.diamond.practice.spigot.spigotapi.util.TriConsumer;

public abstract class AbstractMovementHandler {

    public abstract void injectLocationUpdate(TriConsumer<Player, Location, Location> data);

    public abstract void injectRotationUpdate(TriConsumer<Player, Location, Location> data);

}
