package rip.diamond.practice.events.games.brackets;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.scheduler.BukkitTask;
import rip.diamond.practice.events.EventPlayer;
import rip.diamond.practice.events.PracticeEvent;

import java.util.UUID;

@Setter
@Getter
public class BracketsPlayer extends EventPlayer {

    private BracketsState state = BracketsState.WAITING;
    private int wins = 0;
    private BukkitTask fightTask;
    private BracketsPlayer fighting;

    public BracketsPlayer(UUID uuid, PracticeEvent<?> event) {
        super(uuid, event);
    }

    public enum BracketsState {
        WAITING, PREPARING, FIGHTING, ELIMINATED
    }
}
