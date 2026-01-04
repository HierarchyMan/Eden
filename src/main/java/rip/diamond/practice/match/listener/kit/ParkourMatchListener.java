package rip.diamond.practice.match.listener.kit;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import rip.diamond.practice.config.EdenSound;
import rip.diamond.practice.config.Language;
import rip.diamond.practice.match.Match;
import rip.diamond.practice.match.MatchState;
import rip.diamond.practice.profile.PlayerProfile;
import rip.diamond.practice.profile.PlayerState;
import rip.diamond.practice.util.Common;
import rip.diamond.practice.config.Config;

/**
 * Handles Parkour-with-checkpoints behavior for matches.
 *
 * Gated behind KitGameRules#parkour.
 *
 * Responsibilities:
 * - Track when a player passes a checkpoint (movement)
 * - Prevent building within a configured radius of checkpoints
 * - When the player "fails" (void/y-limit), teleport them back to last checkpoint
 */
public class ParkourMatchListener implements Listener {

    private boolean isTeamA(Match match, Player player) {
        if (match == null || player == null || match.getArenaDetail() == null) {
            return true;
        }
        org.bukkit.Location a = match.getArenaDetail().getA();
        org.bukkit.Location b = match.getArenaDetail().getB();
        if (a == null || b == null) {
            return true;
        }
        try {
            rip.diamond.practice.match.team.Team team = match.getTeam(player);
            if (team == null || team.getSpawnLocation() == null) {
                return true;
            }
            org.bukkit.Location spawn = team.getSpawnLocation();
            return spawn.distanceSquared(a) <= spawn.distanceSquared(b);
        } catch (Exception ignored) {
            return true;
        }
    }

    /**
     * Validate that the player is in an active match with parkour enabled.
     */
    private Match validateParkourMatch(Player player) {
        if (player == null) {
            return null;
        }

        PlayerProfile profile = PlayerProfile.get(player);
        if (profile == null) {
            return null;
        }

        if (profile.getPlayerState() != PlayerState.IN_MATCH) {
            return null;
        }

        Match match = profile.getMatch();
        if (match == null) {
            return null;
        }

        if (match.getState() == MatchState.ENDING) {
            return null;
        }

        if (!match.getKit().getGameRules().isParkour()) {
            return null;
        }

        if (match.getArenaDetail() == null || match.getArenaDetail().getArena() == null) {
            return null;
        }

        // Must have checkpoints configured for the arena for THIS team side
        boolean teamA = isTeamA(match, player);
        if (match.getArenaDetail().getArena().getParkourCheckpoints(teamA).isEmpty()) {
            // Arena is misconfigured for this parkour kit. Cancel the match (no winners) and alert admins.
            cancelMisconfiguredMatch(match);
            return null;
        }

        return match;
    }

    private void cancelMisconfiguredMatch(Match match) {
        if (match == null || match.getState() == MatchState.ENDING) {
            return;
        }

        // Send players a friendly message.
        match.getPlayersAndSpectators().forEach(p -> {
            if (p != null) {
                Language.PARKOUR_MATCH_CANCELLED_NO_CHECKPOINTS_MESSAGE.sendMessage(p);
            }
        });

        // Alert online admins.
        String kitName = match.getKit() != null ? match.getKit().getName() : "?";
        String arenaName = (match.getArenaDetail() != null && match.getArenaDetail().getArena() != null)
                ? match.getArenaDetail().getArena().getName()
                : "?";

        org.bukkit.Bukkit.getOnlinePlayers().stream()
                .filter(p -> p != null && p.isOp())
                .forEach(p -> Language.PARKOUR_MATCH_CANCELLED_NO_CHECKPOINTS_ADMIN.sendMessage(p, kitName, arenaName));

        Common.log(Language.PARKOUR_MATCH_CANCELLED_NO_CHECKPOINTS_ADMIN.toString(null, kitName, arenaName));

        // Force end with reason (no winners).
        match.end(true, Language.PARKOUR_MATCH_CANCELLED_NO_CHECKPOINTS_MESSAGE.toString());
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Match match = validateParkourMatch(player);
        if (match == null) {
            return;
        }

        // Only react to actual block movement
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Location to = event.getTo();
        boolean teamA = isTeamA(match, player);

        // Resolve ordered checkpoint index by checking the team's arena list with small radius.
        java.util.List<Location> cps = match.getArenaDetail().getArena().getParkourCheckpoints(teamA);
        int hitIndex = -1;
        Location hitCheckpoint = null;
        for (int i = 0; i < cps.size(); i++) {
            Location cp = cps.get(i);
            if (cp == null || cp.getWorld() == null || to.getWorld() == null) {
                continue;
            }
            if (!cp.getWorld().getName().equals(to.getWorld().getName())) {
                continue;
            }
            // Use block-centered distance to avoid triggering when merely near.
            if (cp.getBlockX() == to.getBlockX() && cp.getBlockY() == to.getBlockY() && cp.getBlockZ() == to.getBlockZ()) {
                hitIndex = i;
                hitCheckpoint = cp;
                break;
            }
        }

        if (hitCheckpoint == null) {
            return;
        }

        int newIndex = hitIndex + 1; // 1..N
        int currentIndex = match.getLastParkourCheckpointIndex(player.getUniqueId());

        boolean allowSkipping = Config.MATCH_PARKOUR_ALLOW_CHECKPOINT_SKIPPING.toBoolean();

        // If skipping isn't allowed, require sequential progression (no backtracking still enforced by storing max index).
        if (!allowSkipping && newIndex > currentIndex + 1) {
            // Tell player to go back and collect missing checkpoints.
            Language.PARKOUR_MISSING_PREVIOUS_CHECKPOINTS.sendMessage(player, currentIndex + 1, newIndex);
            return;
        }

        // Update progress if improving (if skipping is enabled, this also auto-marks skipped checkpoints as passed)
        if (newIndex > currentIndex) {
            match.updateLastParkourCheckpoint(player.getUniqueId(), hitCheckpoint, newIndex);

            // Feedback: checkpoint reached
            Language.PARKOUR_CHECKPOINT_REACHED.sendMessage(player, newIndex);
            EdenSound.PARKOUR_CHECKPOINT.play(player);
            match.getMatchPlayers().stream()
                    .filter(p -> p != null && !p.getUniqueId().equals(player.getUniqueId()))
                    .forEach(p -> EdenSound.PARKOUR_OPPONENT_CHECKPOINT.play(p));
        }

        // Finish = last checkpoint (for this team side)
        int total = match.getTotalParkourCheckpoints(player);
        int effectiveIndex = Math.max(currentIndex, newIndex);
        if (total > 0 && effectiveIndex >= total) {
            // Winner is first TEAM/PLAYER to reach finish.
            rip.diamond.practice.match.team.Team winnerTeam = match.getTeam(player);

            // Sounds are safe here, but the actual match end should follow existing elimination logic.
            match.getMatchPlayers().forEach(p -> {
                if (p == null) {
                    return;
                }
                if (winnerTeam != null && match.getTeam(p) == winnerTeam) {
                    EdenSound.PARKOUR_WIN.play(p);
                } else {
                    EdenSound.PARKOUR_LOSE.play(p);
                }
            });

            // Eliminate all players NOT on the winner team using Match#die(), so post-match inventories/stats are created.
            for (Player p : match.getMatchPlayers()) {
                if (p == null) {
                    continue;
                }
                if (winnerTeam != null && match.getTeam(p) == winnerTeam) {
                    continue;
                }
                match.die(p, false);
            }

            return;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Match match = validateParkourMatch(player);
        if (match == null) {
            return;
        }

        int radius = match.getKit().getGameRules().getParkourCheckpointBuildRadius();
        if (radius <= 0) {
            return;
        }

        boolean teamA = isTeamA(match, player);
        if (match.getArenaDetail().getArena().isNearCheckpoint(teamA, event.getBlockPlaced().getLocation(), radius)) {
            event.setCancelled(true);
            player.updateInventory();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Match match = validateParkourMatch(player);
        if (match == null) {
            return;
        }

        int radius = match.getKit().getGameRules().getParkourCheckpointBuildRadius();
        if (radius <= 0) {
            return;
        }

        boolean teamA = isTeamA(match, player);
        if (match.getArenaDetail().getArena().isNearCheckpoint(teamA, event.getBlock().getLocation(), radius)) {
            event.setCancelled(true);
            player.updateInventory();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onVoidDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        if (event.getCause() != EntityDamageEvent.DamageCause.VOID) {
            return;
        }

        Match match = validateParkourMatch(player);
        if (match == null) {
            return;
        }

        // We replace void death with a teleport back.
        event.setCancelled(true);
        match.teleportToParkourRespawn(player);
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFatalDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        Match match = validateParkourMatch(player);
        if (match == null) {
            return;
        }

        // Ignore VOID here (handled by onVoidDamage + MatchMovementHandler)
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            return;
        }

        // Only intercept lethal damage while fighting.
        if (match.getState() != MatchState.FIGHTING) {
            return;
        }

        double finalDamage = event.getFinalDamage();
        double health = player.getHealth();
        if (finalDamage < health) {
            return;
        }

        // Cancel death/elimination and just respawn back.
        event.setCancelled(true);

        // Minimal reset to avoid stuck-in-combat states.
        player.setFireTicks(0);
        player.setVelocity(new org.bukkit.util.Vector());
        player.setHealth(player.getMaxHealth());

        match.teleportToParkourRespawn(player);
    }
}
