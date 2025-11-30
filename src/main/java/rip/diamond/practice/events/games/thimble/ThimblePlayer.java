package rip.diamond.practice.events.games.thimble;

import lombok.Getter;
import lombok.Setter;
import rip.diamond.practice.events.EventPlayer;
import rip.diamond.practice.events.PracticeEvent;

import java.util.UUID;

@Setter
@Getter
public class ThimblePlayer extends EventPlayer {

    private ThimbleState state = ThimbleState.WAITING;
    private int jumps = 0;
    private int fails = 0;

    public ThimblePlayer(UUID uuid, PracticeEvent<?> event) {
        super(uuid, event);
    }

    public enum ThimbleState {
        WAITING, JUMPING, SPECTATING, ELIMINATED
    }
}
