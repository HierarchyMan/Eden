package rip.diamond.practice.events.games.gulag;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.scheduler.BukkitTask;
import rip.diamond.practice.events.EventPlayer;
import rip.diamond.practice.events.PracticeEvent;

import java.util.UUID;

@Setter
@Getter
public class GulagPlayer extends EventPlayer {

    private GulagState state = GulagState.WAITING;
    private int wins = 0;
    private BukkitTask fightTask;
    private GulagPlayer fighting;

    public GulagPlayer(UUID uuid, PracticeEvent<?> event) {
        super(uuid, event);
    }

    public enum GulagState {
        WAITING, PREPARING, FIGHTING, ELIMINATED
    }
}
