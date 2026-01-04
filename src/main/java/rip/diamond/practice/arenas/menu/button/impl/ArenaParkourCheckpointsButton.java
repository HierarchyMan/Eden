package rip.diamond.practice.arenas.menu.button.impl;

import rip.diamond.practice.arenas.Arena;
import rip.diamond.practice.util.menu.Menu;

/**
 * @deprecated Use {@link ArenaParkourCheckpointsTeamButton} (team A/team B) and {@link ArenaParkourCheckpointsMenuButton}.
 */
@Deprecated
public class ArenaParkourCheckpointsButton extends ArenaParkourCheckpointsTeamButton {

    public ArenaParkourCheckpointsButton(Arena arena, Menu backMenu) {
        super(arena, backMenu, true);
    }
}
