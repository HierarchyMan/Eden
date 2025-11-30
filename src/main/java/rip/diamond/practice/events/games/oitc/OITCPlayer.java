package rip.diamond.practice.events.games.oitc;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.scheduler.BukkitTask;
import rip.diamond.practice.events.EventPlayer;
import rip.diamond.practice.events.PracticeEvent;

import java.util.UUID;

@Setter
@Getter
public class OITCPlayer extends EventPlayer {

    private OITCState state = OITCState.WAITING;
    private int score = 0;
    private int currentStreak = 0;
    private OITCPlayer lastKiller;
    private KillType lastKillType = KillType.MELEE;
    private BukkitTask respawnTask;

    public OITCPlayer(UUID uuid, PracticeEvent<?> event) {
        super(uuid, event);
    }

    public enum OITCState {
        WAITING, FIGHTING, ELIMINATED, RESPAWNING
    }

    public enum KillType {
        ARROW, MELEE
    }
}
