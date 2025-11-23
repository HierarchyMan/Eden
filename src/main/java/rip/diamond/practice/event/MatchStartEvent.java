package rip.diamond.practice.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import rip.diamond.practice.match.Match;
import rip.diamond.practice.util.BaseEvent;

@RequiredArgsConstructor
public class MatchStartEvent extends BaseEvent{

    @Getter private final Match match;
}
