package rip.diamond.practice.events.games.sumo;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.scheduler.BukkitTask;
import rip.diamond.practice.events.EventPlayer;
import rip.diamond.practice.events.PracticeEvent;

import java.util.UUID;

@Setter
@Getter
public class SumoPlayer extends EventPlayer {

    private SumoState state = SumoState.WAITING;
    private int wins = 0;
    private BukkitTask fightTask;
    private SumoPlayer fighting;

    public SumoPlayer(UUID uuid, PracticeEvent<?> event) {
        super(uuid, event);
    }

    public enum SumoState {
        WAITING, PREPARING, FIGHTING, ELIMINATED
    }
}
