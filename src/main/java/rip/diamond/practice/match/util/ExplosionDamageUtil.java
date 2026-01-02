package rip.diamond.practice.match.util;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import rip.diamond.practice.match.Match;
import rip.diamond.practice.match.team.Team;
import rip.diamond.practice.profile.PlayerProfile;

/**
 * Utility class for explosion damage calculations.
 */
public class ExplosionDamageUtil {

    /**
     * Calculates the appropriate max damage based on whether the victim is on the same team as the source.
     *
     * @param victim       The player taking damage
     * @param source       The entity that caused the explosion (shooter/placer)
     * @param selfDamage   Max damage for self/teammates
     * @param othersDamage Max damage for enemies
     * @return The appropriate max damage value
     */
    public static double getMaxDamage(Player victim, LivingEntity source, double selfDamage, double othersDamage) {
        if (!(source instanceof Player)) {
            return othersDamage;
        }

        Player sourcePlayer = (Player) source;
        PlayerProfile sourceProfile = PlayerProfile.get(sourcePlayer);
        PlayerProfile victimProfile = PlayerProfile.get(victim);

        if (sourceProfile == null || victimProfile == null) {
            return othersDamage;
        }

        Match match = sourceProfile.getMatch();
        if (match == null || match != victimProfile.getMatch()) {
            return othersDamage;
        }

        Team sourceTeam = match.getTeam(sourcePlayer);
        Team victimTeam = match.getTeam(victim);

        if (sourceTeam != null && sourceTeam.equals(victimTeam)) {
            return selfDamage;
        }

        return othersDamage;
    }

    /**
     * Calculates explosion damage based on distance from explosion center.
     * Config value represents max damage at point-blank, linear falloff to zero at edge of blast.
     *
     * @param victimLocation    The victim's location
     * @param explosionLocation The explosion center location
     * @param explosionYield    The explosion power/yield
     * @param configMaxDamage   The configured max damage at point-blank
     * @param obstructionBlocks Number of solid blocks between explosion and victim (pre-calculated)
     * @return The calculated damage value
     */
    public static double calculateDamage(org.bukkit.Location victimLocation, org.bukkit.Location explosionLocation, 
                                          double explosionYield, double configMaxDamage, int obstructionBlocks) {
        double distance = victimLocation.distance(explosionLocation);
        double radius = explosionYield * 2.0;
        
        // Gradual falloff using square root - damage drops slower near center
        double distanceRatio = Math.max(0.0, 1.0 - (distance / radius));
        double gradualRatio = Math.sqrt(distanceRatio);
        double baseDamage = configMaxDamage * gradualRatio;
        
        // Apply obstruction: reduce damage by 90% per solid block in the way
        double obstructionMultiplier = Math.pow(0.1, obstructionBlocks);
        
        return baseDamage * obstructionMultiplier;
    }
    
    /**
     * Counts solid blocks between two locations using ray trace.
     */
    public static int countBlocksInPath(org.bukkit.Location from, org.bukkit.Location to) {
        int count = 0;
        org.bukkit.util.Vector direction = to.toVector().subtract(from.toVector());
        double distance = direction.length();
        
        if (distance < 1) return 0;
        
        direction.normalize();
        org.bukkit.Location current = from.clone();
        
        // Step through in 0.5 block increments
        java.util.Set<org.bukkit.block.Block> checked = new java.util.HashSet<>();
        for (double d = 0.5; d < distance; d += 0.5) {
            current = from.clone().add(direction.clone().multiply(d));
            org.bukkit.block.Block block = current.getBlock();
            
            if (!checked.contains(block) && block.getType().isSolid()) {
                count++;
                checked.add(block);
            }
        }
        
        return count;
    }
}
