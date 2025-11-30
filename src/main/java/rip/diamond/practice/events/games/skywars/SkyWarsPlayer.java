package rip.diamond.practice.events.games.skywars;

import lombok.Getter;
import lombok.Setter;
import rip.diamond.practice.events.EventPlayer;
import rip.diamond.practice.events.PracticeEvent;

import java.util.UUID;

@Setter
@Getter
public class SkyWarsPlayer extends EventPlayer {

    private SkyWarsState state = SkyWarsState.WAITING;
    private int kills = 0;

    public SkyWarsPlayer(UUID uuid, PracticeEvent<?> event) {
        super(uuid, event);
    }

    public enum SkyWarsState {
        WAITING, FIGHTING, ELIMINATED
    }
}
