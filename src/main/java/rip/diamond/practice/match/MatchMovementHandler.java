package rip.diamond.practice.match;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import rip.diamond.practice.Eden;
import rip.diamond.practice.arenas.Arena;
import rip.diamond.practice.arenas.ArenaDetail;
import rip.diamond.practice.config.Config;
import rip.diamond.practice.kits.Kit;
import rip.diamond.practice.kits.KitGameRules;
import rip.diamond.practice.match.team.Team;
import rip.diamond.practice.match.team.TeamPlayer;
import rip.diamond.practice.profile.PlayerProfile;
import rip.diamond.practice.profile.PlayerState;
import rip.diamond.practice.profile.cooldown.CooldownType;
import rip.diamond.practice.util.Common;
import rip.diamond.practice.util.Util;
import rip.diamond.practice.util.cuboid.CuboidDirection;

import java.util.Comparator;

public class MatchMovementHandler {

    public MatchMovementHandler() {
        Eden.INSTANCE.getSpigotAPI().getMovementHandler().injectLocationUpdate((player, from, to) -> {
            PlayerProfile profile = PlayerProfile.get(player);

            Block block = to.getBlock();
            Block underBlock = to.clone().add(0, -1, 0).getBlock();

            if (profile.getPlayerState() == PlayerState.IN_MATCH && profile.getMatch() != null) {
                Match match = profile.getMatch();
                ArenaDetail arenaDetail = match.getArenaDetail();
                Arena arena = arenaDetail.getArena();
                Kit kit = match.getKit();
                KitGameRules gameRules = kit.getGameRules();

                if (gameRules.isStartFreeze() && match.getState() == MatchState.STARTING
                        && (from.getX() != to.getX() || from.getZ() != to.getZ())) {
                    Location location = match.getTeam(player).getSpawnLocation();
                    
                    
                    location.setY(from.getY());
                    location.setPitch(from.getPitch());
                    location.setYaw(from.getYaw());
                    Util.teleport(player, location);
                    return;
                }

                if ((!arenaDetail.getCuboid().clone().outset(CuboidDirection.HORIZONTAL, 10).contains(player)
                        && Config.MATCH_OUTSIDE_CUBOID_INSTANT_DEATH.toBoolean())
                        || arena.getYLimit() > player.getLocation().getY()) {
                    TeamPlayer teamPlayer = match.getTeamPlayer(player);
                    
                    if (teamPlayer.isAlive() && !teamPlayer.isRespawning()) {
                        Util.damage(player, 99999);
                    }
                    return;
                }

                
                
                
                
                
                if (match.getMatchPlayers().stream().allMatch(p -> PlayerProfile
                        .get(p)
                        .getCooldowns()
                        .get(CooldownType.SCORE)
                        .isExpired())) {
                    TeamPlayer teamPlayer = match.getTeamPlayer(player);
                    if (match.getState() == MatchState.FIGHTING && !teamPlayer.isRespawning()) {
                        
                        if (gameRules.isDeathOnWater() && (block.getType() == Material.WATER
                                || block.getType() == Material.STATIONARY_WATER)) {
                            if (gameRules.isPoint(match)) {
                                TeamPlayer lastHitDamager = teamPlayer.getLastHitDamager();
                                
                                if (lastHitDamager == null) {
                                    lastHitDamager = match.getOpponentTeam(match.getTeam(player)).getAliveTeamPlayers()
                                            .get(0);
                                }
                                match.score(profile, teamPlayer, lastHitDamager);
                            } else {
                                Util.damage(player, 99999);
                            }
                            return;
                        }

                        
                        if (gameRules.isPortalGoal() && block.getType() == Material.ENDER_PORTAL) {
                            Team playerTeam = match.getTeam(player);
                            Team portalBelongsTo = match.getTeams().stream()
                                    .min(Comparator.comparing(team -> team.getSpawnLocation().distance(to)))
                                    .orElse(null);
                            if (portalBelongsTo == null) {
                                Common.log(
                                        "An error occurred while finding portalBelongsTo, please contact GoodestEnglish to fix");
                                return;
                            }
                            if (portalBelongsTo != playerTeam) {
                                match.score(profile, null, match.getTeamPlayer(player));
                            } else {
                                
                                Util.damage(player, 99999);
                            }
                            return;
                        }
                    }
                }
            } else if (profile.getPlayerState() == PlayerState.IN_SPECTATING && profile.getMatch() != null) {
                Match match = profile.getMatch();
                ArenaDetail arenaDetail = match.getArenaDetail();

                if (!arenaDetail.getCuboid().clone()
                        .outset(CuboidDirection.HORIZONTAL, Config.MATCH_SPECTATE_EXPEND_CUBOID.toInteger())
                        .contains(player)) {
                    player.teleport(arenaDetail.getSpectator());
                    return;
                }
            }
        });
    }
}
