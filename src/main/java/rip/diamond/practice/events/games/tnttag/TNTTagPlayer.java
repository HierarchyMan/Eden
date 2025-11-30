package rip.diamond.practice.events.games.tnttag;

import lombok.Getter;
import lombok.Setter;
import rip.diamond.practice.events.EventPlayer;
import rip.diamond.practice.events.PracticeEvent;

import java.util.UUID;

@Setter
@Getter
public class TNTTagPlayer extends EventPlayer {

    private TNTTagState state = TNTTagState.WAITING;
    private boolean tagged = false;

    public TNTTagPlayer(UUID uuid, PracticeEvent<?> event) {
        super(uuid, event);
    }

    public enum TNTTagState {
        WAITING, INGAME, TAGGED, ELIMINATED
    }
}
