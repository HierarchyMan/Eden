package rip.diamond.practice.spigot.spigotapi.movementhandler.impl;

import net.minecraft.server.v1_8_R3.PacketPlayInFlying;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import rip.diamond.practice.spigot.spigotapi.SpigotAPI;
import rip.diamond.practice.spigot.spigotapi.movementhandler.AbstractMovementHandler;
import rip.diamond.practice.spigot.spigotapi.util.TriConsumer;
import xyz.refinedev.spigot.api.handlers.PacketAPI;
import xyz.refinedev.spigot.api.handlers.impl.MovementHandler;

public class CarbonSpigotMovementHandler extends AbstractMovementHandler {

    @Override
    public void injectLocationUpdate(TriConsumer<Player, Location, Location> data) {
        MovementHandler movementHandler = new MovementHandler() {
            @Override
            public void handleUpdateLocation(Player player, Location location, Location location1, PacketPlayInFlying packetPlayInFlying) {
                data.accept(player, location, location1);
            }

            @Override
            public void handleUpdateRotation(Player player, Location location, Location location1, PacketPlayInFlying packetPlayInFlying) {

            }
        };
        PacketAPI.getInstance().registerMovementHandler(SpigotAPI.PLUGIN, movementHandler);
    }

    @Override
    public void injectRotationUpdate(TriConsumer<Player, Location, Location> data) {
        MovementHandler movementHandler = new MovementHandler() {
            @Override
            public void handleUpdateLocation(Player player, Location location, Location location1, PacketPlayInFlying packetPlayInFlying) {

            }

            @Override
            public void handleUpdateRotation(Player player, Location location, Location location1, PacketPlayInFlying packetPlayInFlying) {
                data.accept(player, location, location1);
            }
        };
        PacketAPI.getInstance().registerMovementHandler(SpigotAPI.PLUGIN, movementHandler);
    }
}
