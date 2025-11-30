package rip.diamond.practice.config;

import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import rip.diamond.practice.Eden;
import rip.diamond.practice.util.Common;
import rip.diamond.practice.util.Util;

@RequiredArgsConstructor
public enum EdenSound {

    RECEIVE_DUEL_REQUEST("receive-duel-request", Sound.CHICKEN_EGG_POP, 1f, 1f),
    GOLDEN_HEAD_EAT("golden-head-eat", Sound.EAT, 1f, 1f),
    BED_SELF_BROKEN("bed-self-broken", Sound.VILLAGER_NO, 1f, 1f),
    BED_OPPONENT_BROKEN("bed-opponent-broken", Sound.WITHER_DEATH, 1f, 1f),
    NEW_ROUND_COUNTDOWN("new-round-countdown", Sound.CLICK, 1f, 1f),
    MATCH_START("match-start", Sound.FIREWORK_BLAST, 1f, 1f),
    OITC_ARROW_KILL("oitc-arrow-kill", Sound.NOTE_PLING, 1f, 1.5f),
    OITC_MELEE_KILL("oitc-melee-kill", Sound.ORB_PICKUP, 1f, 1f),
    OITC_DEATH("oitc-death", Sound.ENDERMAN_TELEPORT, 1f, 1f),
    OPPONENT_DEATH("opponent-death", Sound.ARROW_HIT, 1f, 1f),
    POINT_SCORED("point-scored", Sound.ORB_PICKUP, 1f, 1f),
    FINAL_DEATH_SOUND("final-death-sound", Sound.AMBIENCE_THUNDER, 1f, 1f),
    ;

    private final String path;
    private final Sound sound;
    private final float volume;
    private final float pitch;

    private Sound getSound() {
        String str = Eden.INSTANCE.getSoundFile().getString(path);
        if (Util.isNull(str)) {
            return sound;
        }
        return Sound.valueOf(str.split(";")[0]);
    }

    public float getVolume() {
        String str = Eden.INSTANCE.getSoundFile().getString(path);
        if (Util.isNull(str)) {
            return volume;
        }
        return Float.parseFloat(str.split(";")[1]);
    }

    public float getPitch() {
        String str = Eden.INSTANCE.getSoundFile().getString(path);
        if (Util.isNull(str)) {
            return pitch;
        }
        return Float.parseFloat(str.split(";")[2]);
    }

    public void play(Player player) {
        Common.playSound(player, getSound(), getVolume(), getPitch());
    }

    public void play(Player player, Location location) {
        Common.playSound(player, location, getSound(), getVolume(), getPitch());
    }

}
