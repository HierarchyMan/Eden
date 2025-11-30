package rip.diamond.practice.events.games.parkour;

import lombok.Getter;
import lombok.Setter;
import rip.diamond.practice.events.EventPlayer;
import rip.diamond.practice.events.PracticeEvent;

import java.util.UUID;

@Setter
@Getter
public class ParkourPlayer extends EventPlayer {

    private ParkourState state = ParkourState.WAITING;
    private long startTime;
    private int currentCheckpoint = 0;
    private long checkpointTime;

    public ParkourPlayer(UUID uuid, PracticeEvent<?> event) {
        super(uuid, event);
    }

    public enum ParkourState {
        WAITING, PLAYING, FINISHED, ELIMINATED
    }
}
