package rip.diamond.practice.events.games.knockout;

import lombok.Getter;
import lombok.Setter;
import rip.diamond.practice.events.EventPlayer;
import rip.diamond.practice.events.PracticeEvent;

import java.util.UUID;

@Setter
@Getter
public class KnockoutPlayer extends EventPlayer {

    private KnockoutState state = KnockoutState.WAITING;

    public KnockoutPlayer(UUID uuid, PracticeEvent<?> event) {
        super(uuid, event);
    }

    public enum KnockoutState {
        WAITING, FIGHTING, ELIMINATED
    }
}
