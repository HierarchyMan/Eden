package rip.diamond.practice.events.games.corners;

import lombok.Getter;
import lombok.Setter;
import rip.diamond.practice.events.EventPlayer;
import rip.diamond.practice.events.PracticeEvent;

import java.util.UUID;

@Setter
@Getter
public class FourCornersPlayer extends EventPlayer {

    private FourCornersState state = FourCornersState.WAITING;
    private int corner = 0;

    public FourCornersPlayer(UUID uuid, PracticeEvent<?> event) {
        super(uuid, event);
    }

    public enum FourCornersState {
        WAITING, PLAYING, ELIMINATED
    }
}
