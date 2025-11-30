package rip.diamond.practice.events.games.spleef;

import lombok.Getter;
import lombok.Setter;
import rip.diamond.practice.events.EventPlayer;
import rip.diamond.practice.events.PracticeEvent;

import java.util.UUID;

@Setter
@Getter
public class SpleefPlayer extends EventPlayer {

    private SpleefState state = SpleefState.WAITING;

    public SpleefPlayer(UUID uuid, PracticeEvent<?> event) {
        super(uuid, event);
    }

    public enum SpleefState {
        WAITING, FIGHTING, ELIMINATED
    }
}
