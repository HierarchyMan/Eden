package rip.diamond.practice.events.games.lms;

import lombok.Getter;
import lombok.Setter;
import rip.diamond.practice.events.EventPlayer;
import rip.diamond.practice.events.PracticeEvent;

import java.util.UUID;

@Setter
@Getter
public class LMSPlayer extends EventPlayer {

    private LMSState state = LMSState.WAITING;

    public LMSPlayer(UUID uuid, PracticeEvent<?> event) {
        super(uuid, event);
    }

    public enum LMSState {
        WAITING, FIGHTING, ELIMINATED
    }
}
