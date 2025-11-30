package rip.diamond.practice.events.games.oitc;

import lombok.RequiredArgsConstructor;
import net.minecraft.server.v1_8_R3.PacketPlayOutTitle;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.PlayerUtil;
import rip.diamond.practice.util.TitleSender;
import rip.diamond.practice.util.VisibilityController;

@RequiredArgsConstructor
public class OITCRespawnTask extends BukkitRunnable {

    private final OITCEvent event;
    private final Player player;
    private final OITCPlayer oitcPlayer;
    private int time = 3;

    @Override
    public void run() {
        if (!event.isRunning()) {
            cancel();
            return;
        }

        if (time == 3) {
            
            PlayerUtil.reset(player);
            player.setGameMode(GameMode.ADVENTURE);
            player.setAllowFlight(true);
            player.setFlying(true);

            oitcPlayer.setState(OITCPlayer.OITCState.RESPAWNING);

            
            VisibilityController.hideFromEvent(player);

            
            if (event.getSpectatorLocation() != null) {
                player.teleport(event.getSpectatorLocation());
            }
        }

        if (time > 0) {
            
            player.sendMessage(CC.translate("&eRespawning in &c" + time + "&e..."));
            TitleSender.sendTitle(player, CC.translate("&c&lYOU DIED"), PacketPlayOutTitle.EnumTitleAction.TITLE, 0, 21, 0);
            TitleSender.sendTitle(player, CC.translate("&eRespawning in &c" + time + "&e..."), PacketPlayOutTitle.EnumTitleAction.SUBTITLE, 0, 21, 0);
        } else {
            
            player.sendMessage(CC.translate("&aRespawned!"));
            TitleSender.sendTitle(player, CC.translate("&a&lRESPAWNED"), PacketPlayOutTitle.EnumTitleAction.TITLE, 0, 21, 5);
            TitleSender.sendTitle(player, "", PacketPlayOutTitle.EnumTitleAction.SUBTITLE, 0, 21, 5);

            oitcPlayer.setState(OITCPlayer.OITCState.FIGHTING);
            event.giveRespawnItems(player);
            event.teleportNextLocation(player);

            
            VisibilityController.showToEvent(player);

            cancel();
        }

        time--;
    }
}
