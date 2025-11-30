package rip.diamond.practice.events.games.stoplight;

import lombok.Getter;
import lombok.Setter;
import rip.diamond.practice.events.EventPlayer;
import rip.diamond.practice.events.PracticeEvent;

import java.util.UUID;

@Setter
@Getter
public class StopLightPlayer extends EventPlayer {

    private StopLightState state = StopLightState.WAITING;

    public StopLightPlayer(UUID uuid, PracticeEvent<?> event) {
        super(uuid, event);
    }

    public enum StopLightState {
        WAITING, PLAYING, ELIMINATED
    }
}
