package rip.diamond.practice.match.util;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.util.Vector;
import rip.diamond.practice.config.Config;
import rip.diamond.practice.match.Match;
import rip.diamond.practice.match.team.TeamPlayer;
import rip.diamond.practice.profile.PlayerProfile;
import rip.diamond.practice.profile.PlayerState;

/**
 * Handles Sumo-style knockback and damage for Insta Boom TNT.
 */
public class InstaBoomKnockback {

    public static class KnockbackResult {
        public final boolean shouldDamage;
        public final Vector velocity;

        public KnockbackResult(boolean shouldDamage, Vector velocity) {
            this.shouldDamage = shouldDamage;
            this.velocity = velocity;
        }
    }

    public static boolean shouldExclude(Player player, Match match) {
        if (match == null) return true;

        PlayerProfile profile = PlayerProfile.get(player);
        if (profile == null) return true;

        if (profile.getPlayerState() == PlayerState.IN_SPECTATING) {
            return true;
        }

        TeamPlayer teamPlayer = match.getTeamPlayer(player);
        if (teamPlayer == null || teamPlayer.isRespawning() || !teamPlayer.isAlive()) {
            return true;
        }

        return false;
    }

    public static boolean isWithinRadius(Player player, Location tntLocation) {
        double radius = Config.MATCH_INSTA_TNT_RADIUS.toDouble();
        return player.getLocation().distance(tntLocation) <= radius;
    }

    public static KnockbackResult calculate(Player player, TNTPrimed tnt) {
        org.bukkit.entity.Entity sourceEntity = tnt.getSource();
        boolean isSelf = (sourceEntity instanceof Player && sourceEntity.equals(player));
        Location tntLoc = tnt.getLocation();
        Location playerLoc = player.getLocation();

        // Self on ground = no damage, no knockback
        if (isSelf && player.isOnGround()) {
            return new KnockbackResult(false, new Vector(0, 0, 0));
        }

        double kbVertical;
        double kbHorizontal;

        if (!player.isOnGround()) {
            // In air = full knockback
            kbVertical = Config.MATCH_INSTA_TNT_KNOCKBACK_VERTICAL.toDouble();
            kbHorizontal = Config.MATCH_INSTA_TNT_KNOCKBACK_HORIZONTAL.toDouble();
        } else {
            // Other on ground = reduced knockback (0.15 vertical, 0.5 horizontal from tntsumo)
            kbVertical = Config.MATCH_INSTA_TNT_KNOCKBACK_GROUND_VERTICAL.toDouble();
            kbHorizontal = Config.MATCH_INSTA_TNT_KNOCKBACK_GROUND_HORIZONTAL.toDouble();
        }

        Vector velocity = calculateDirectionalVelocity(playerLoc, tntLoc, kbVertical, kbHorizontal);

        // Y-limit caps the vertical velocity value itself
        double yLimit = Config.MATCH_INSTA_TNT_KNOCKBACK_Y_LIMIT.toDouble();
        if (velocity.getY() > yLimit) {
            velocity.setY(yLimit);
        }

        return new KnockbackResult(true, velocity);
    }

    private static Vector calculateDirectionalVelocity(Location playerLoc, Location tntLoc,
                                                        double vertical, double horizontal) {
        double dx = playerLoc.getX() - tntLoc.getX();
        double dz = playerLoc.getZ() - tntLoc.getZ();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        double threshold = Config.MATCH_INSTA_TNT_KNOCKBACK_DISTANCE_THRESHOLD.toDouble();
        double constantHorizontal = Config.MATCH_INSTA_TNT_KNOCKBACK_CONSTANT_HORIZONTAL.toDouble();

        double finalHorizontalMagnitude;

        if (horizontalDistance < 0.5) {
            // Center: Pure vertical launch
            return new Vector(0, vertical, 0);
        } else if (horizontalDistance <= threshold) {
            // Within threshold: Scale horizontal with distance
            double scale = (horizontalDistance - 0.5) / (threshold - 0.5);
            finalHorizontalMagnitude = horizontal * scale;
        } else {
            // Beyond threshold: Constant horizontal
            finalHorizontalMagnitude = constantHorizontal;
        }

        Vector direction = new Vector(dx, 0, dz).normalize();

        return new Vector(
                direction.getX() * finalHorizontalMagnitude,
                vertical,
                direction.getZ() * finalHorizontalMagnitude
        );
    }

    public static void applyKnockback(Player player, Vector velocity) {
        if (velocity.lengthSquared() == 0) {
            return;
        }
        player.setVelocity(velocity);
    }
}
