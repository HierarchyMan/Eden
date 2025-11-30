package rip.diamond.practice.events.games.dropper;

import lombok.Getter;
import lombok.Setter;
import rip.diamond.practice.events.EventPlayer;
import rip.diamond.practice.events.PracticeEvent;

import java.util.UUID;

@Setter
@Getter
public class DropperPlayer extends EventPlayer {

    private DropperState state = DropperState.WAITING;
    private int mapIndex = 0;

    public DropperPlayer(UUID uuid, PracticeEvent<?> event) {
        super(uuid, event);
    }

    public enum DropperState {
        WAITING, PLAYING, FINISHED, ELIMINATED
    }
}
