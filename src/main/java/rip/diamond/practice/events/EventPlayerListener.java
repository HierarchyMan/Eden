package rip.diamond.practice.events;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import rip.diamond.practice.Eden;
import rip.diamond.practice.events.games.brackets.BracketsEvent;
import rip.diamond.practice.events.games.brackets.BracketsPlayer;
import rip.diamond.practice.events.games.corners.FourCornersEvent;
import rip.diamond.practice.events.games.dropper.DropperEvent;
import rip.diamond.practice.events.games.gulag.GulagEvent;
import rip.diamond.practice.events.games.gulag.GulagPlayer;
import rip.diamond.practice.events.games.knockout.KnockoutEvent;
import rip.diamond.practice.events.games.knockout.KnockoutPlayer;
import rip.diamond.practice.events.games.lms.LMSEvent;
import rip.diamond.practice.events.games.lms.LMSPlayer;
import rip.diamond.practice.events.games.oitc.OITCEvent;
import rip.diamond.practice.events.games.oitc.OITCPlayer;
import rip.diamond.practice.events.games.parkour.ParkourEvent;
import rip.diamond.practice.events.games.skywars.SkyWarsEvent;
import rip.diamond.practice.events.games.skywars.SkyWarsPlayer;
import rip.diamond.practice.events.games.spleef.SpleefEvent;
import rip.diamond.practice.events.games.stoplight.StopLightEvent;
import rip.diamond.practice.events.games.sumo.SumoEvent;
import rip.diamond.practice.events.games.sumo.SumoPlayer;
import rip.diamond.practice.events.games.thimble.ThimbleEvent;
import rip.diamond.practice.events.games.tnttag.TNTTagEvent;
import rip.diamond.practice.events.games.tnttag.TNTTagPlayer;
import rip.diamond.practice.profile.PlayerProfile;
import rip.diamond.practice.profile.PlayerState;

public class EventPlayerListener implements Listener {

    private final Eden plugin = Eden.INSTANCE;

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (player == null)
                return;

            PlayerProfile profile = PlayerProfile.get(player.getUniqueId());
            if (profile != null && profile.getPlayerState() == PlayerState.IN_EVENT) {
                PracticeEvent<?> practiceEvent = plugin.getEventManager().getEventPlaying(player);
                if (practiceEvent != null) {
                    if (practiceEvent instanceof OITCEvent) {
                        OITCEvent oitcEvent = (OITCEvent) practiceEvent;
                        OITCPlayer oitcPlayer = oitcEvent.getPlayer(player);
                        event.setCancelled(
                                oitcPlayer == null || oitcPlayer.getState() != OITCPlayer.OITCState.FIGHTING);
                    } else if (practiceEvent instanceof SumoEvent) {
                        SumoEvent sumoEvent = (SumoEvent) practiceEvent;
                        SumoPlayer sumoPlayer = sumoEvent.getPlayer(player);
                        if (sumoPlayer != null && sumoPlayer.getState() == SumoPlayer.SumoState.FIGHTING) {
                            event.setCancelled(false);
                        }
                        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                            event.setCancelled(true);
                        }
                    } else if (practiceEvent instanceof BracketsEvent) {
                        BracketsEvent bracketsEvent = (BracketsEvent) practiceEvent;
                        BracketsPlayer bracketsPlayer = bracketsEvent.getPlayer(player);
                        if (bracketsPlayer != null
                                && bracketsPlayer.getState() == BracketsPlayer.BracketsState.FIGHTING) {
                            event.setCancelled(false);
                        }
                        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && bracketsPlayer != null
                                && bracketsPlayer.getState() == BracketsPlayer.BracketsState.WAITING) {
                            event.setCancelled(true);
                        }
                    } else if (practiceEvent instanceof LMSEvent) {
                        LMSEvent lmsEvent = (LMSEvent) practiceEvent;
                        LMSPlayer lmsPlayer = lmsEvent.getPlayer(player);
                        if (lmsPlayer != null && (lmsPlayer.getState() == LMSPlayer.LMSState.WAITING
                                || lmsPlayer.getState() == LMSPlayer.LMSState.ELIMINATED)) {
                            if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                                event.setCancelled(true);
                            }
                        }
                    } else if (practiceEvent instanceof SkyWarsEvent) {
                        SkyWarsEvent skyWarsEvent = (SkyWarsEvent) practiceEvent;
                        SkyWarsPlayer skyWarsPlayer = skyWarsEvent.getPlayer(player);
                        if (skyWarsPlayer != null && (skyWarsPlayer.getState() == SkyWarsPlayer.SkyWarsState.WAITING
                                || skyWarsPlayer.getState() == SkyWarsPlayer.SkyWarsState.ELIMINATED)) {
                            if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                                event.setCancelled(true);
                            }
                        }
                    } else if (practiceEvent instanceof ParkourEvent || practiceEvent instanceof SpleefEvent
                            || practiceEvent instanceof StopLightEvent || practiceEvent instanceof KnockoutEvent
                            || practiceEvent instanceof TNTTagEvent || practiceEvent instanceof ThimbleEvent
                            || practiceEvent instanceof DropperEvent || practiceEvent instanceof FourCornersEvent) {
                        if (event.getCause() == EntityDamageEvent.DamageCause.FALL
                                || event.getCause() == EntityDamageEvent.DamageCause.FIRE
                                || event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK) {
                            event.setCancelled(true);
                        }
                    }
                }
            }
            if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
                PlayerProfile profileVoid = PlayerProfile.get(player.getUniqueId());
                if (profileVoid != null && profileVoid.getPlayerState() == PlayerState.IN_EVENT) {
                    PracticeEvent<?> event1 = plugin.getEventManager().getEventPlaying(player);
                    if (event1 != null) {

                        event1.onDeath().accept(player);
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSpectatorDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            PlayerProfile profile = PlayerProfile.get(player);

            if (profile != null && profile.getPlayerState() == PlayerState.IN_EVENT) {
                PracticeEvent<?> practiceEvent = plugin.getEventManager().getEventPlaying(player);
                if (practiceEvent != null && !practiceEvent.getPlayers().containsKey(player.getUniqueId())) {
                    event.setCancelled(true);
                }
                if (plugin.getEventManager().getSpectators().containsKey(player.getUniqueId())) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player entity = (Player) event.getEntity();
        Player damager;

        if (event.getDamager() instanceof Player) {
            damager = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Arrow
                && ((Projectile) event.getDamager()).getShooter() instanceof Player) {
            damager = (Player) ((Projectile) event.getDamager()).getShooter();
        } else {
            return;
        }

        PlayerProfile entityProfile = PlayerProfile.get(entity.getUniqueId());
        PlayerProfile damagerProfile = PlayerProfile.get(damager.getUniqueId());

        if (entityProfile == null || damagerProfile == null)
            return;

        PracticeEvent<?> eventDamager = plugin.getEventManager().getEventPlaying(damager);
        PracticeEvent<?> eventEntity = plugin.getEventManager().getEventPlaying(entity);

        if (entityProfile.getPlayerState() == PlayerState.IN_EVENT
                || damagerProfile.getPlayerState() == PlayerState.IN_EVENT) {
            if (eventDamager != eventEntity) {
                event.setCancelled(true);
                return;
            }
            if (eventDamager == null) {
                return;
            }

            if (plugin.getEventManager().getSpectators().containsKey(damager.getUniqueId()) ||
                    plugin.getEventManager().getSpectators().containsKey(entity.getUniqueId())) {
                event.setCancelled(true);
                return;
            }

            if (eventDamager instanceof SumoEvent) {
                SumoEvent sumoEvent = (SumoEvent) eventDamager;
                if (sumoEvent.getPlayer(damager).getState() != SumoPlayer.SumoState.FIGHTING
                        || sumoEvent.getPlayer(entity).getState() != SumoPlayer.SumoState.FIGHTING) {
                    event.setCancelled(true);
                    return;
                }
                event.setDamage(0.0D);
            } else if (eventDamager instanceof KnockoutEvent) {
                KnockoutEvent knockoutEvent = (KnockoutEvent) eventDamager;
                if (knockoutEvent.getPlayer(damager).getState() != KnockoutPlayer.KnockoutState.FIGHTING
                        || knockoutEvent.getPlayer(entity).getState() != KnockoutPlayer.KnockoutState.FIGHTING) {
                    event.setCancelled(true);
                    return;
                }
                event.setDamage(0.0D);
            } else if (eventDamager instanceof BracketsEvent) {
                BracketsEvent bracketsEvent = (BracketsEvent) eventDamager;
                if (bracketsEvent.getPlayer(damager).getState() != BracketsPlayer.BracketsState.FIGHTING
                        || bracketsEvent.getPlayer(entity).getState() != BracketsPlayer.BracketsState.FIGHTING) {
                    event.setCancelled(true);
                    return;
                }
                event.setDamage(0.0D);
            } else if (eventDamager instanceof LMSEvent) {
                LMSEvent lmsEvent = (LMSEvent) eventDamager;
                if (lmsEvent.getPlayer(damager).getState() != LMSPlayer.LMSState.FIGHTING
                        || lmsEvent.getPlayer(entity).getState() != LMSPlayer.LMSState.FIGHTING) {
                    event.setCancelled(true);
                }
            } else if (eventDamager instanceof SkyWarsEvent) {
                SkyWarsEvent skyWarsEvent = (SkyWarsEvent) eventDamager;
                if (skyWarsEvent.getPlayer(damager).getState() != SkyWarsPlayer.SkyWarsState.FIGHTING
                        || skyWarsEvent.getPlayer(entity).getState() != SkyWarsPlayer.SkyWarsState.FIGHTING) {
                    event.setCancelled(true);
                }
            } else if (eventDamager instanceof TNTTagEvent) {
                TNTTagEvent tntEvent = (TNTTagEvent) eventDamager;
                event.setDamage(0.0D);

                TNTTagPlayer attackerPlayer = tntEvent.getPlayer(damager);
                TNTTagPlayer victimPlayer = tntEvent.getPlayer(entity);

                if (victimPlayer.getState() == TNTTagPlayer.TNTTagState.ELIMINATED) {
                    event.setCancelled(true);
                    return;
                }

                if (attackerPlayer.getState() == TNTTagPlayer.TNTTagState.TAGGED
                        && victimPlayer.getState() == TNTTagPlayer.TNTTagState.INGAME) {
                    tntEvent.tagPlayer(entity, damager);
                } else if (attackerPlayer.getState() != TNTTagPlayer.TNTTagState.TAGGED
                        && attackerPlayer.getState() != TNTTagPlayer.TNTTagState.INGAME) {
                    event.setCancelled(true);
                }
            } else if (eventDamager instanceof GulagEvent) {
                GulagEvent gulagEvent = (GulagEvent) eventDamager;
                GulagPlayer attackerP = gulagEvent.getPlayer(damager);
                GulagPlayer victimP = gulagEvent.getPlayer(entity);

                if (attackerP.getState() == GulagPlayer.GulagState.FIGHTING
                        && victimP.getState() == GulagPlayer.GulagState.FIGHTING) {
                    if (event.getDamager() instanceof Arrow) {
                        event.setDamage(7.0D);
                    } else {
                        event.setDamage(1.5D);
                    }
                } else {
                    event.setCancelled(true);
                }
            } else if (eventDamager instanceof ParkourEvent || eventDamager instanceof FourCornersEvent
                    || eventDamager instanceof ThimbleEvent || eventDamager instanceof DropperEvent
                    || eventDamager instanceof StopLightEvent || eventDamager instanceof SpleefEvent) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        PlayerProfile profile = PlayerProfile.get(player);

        if (profile != null && profile.getPlayerState() == PlayerState.IN_EVENT) {
            PracticeEvent<?> practiceEvent = plugin.getEventManager().getEventPlaying(player);
            if (practiceEvent != null) {
                if (!practiceEvent.canDropItems()) {
                    event.setCancelled(true);
                    player.updateInventory();
                }
            }
        }
    }

    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        PlayerProfile profile = PlayerProfile.get(player);

        if (profile != null && profile.getPlayerState() == PlayerState.IN_EVENT) {
            PracticeEvent<?> practiceEvent = plugin.getEventManager().getEventPlaying(player);
            if (practiceEvent != null) {
                if (!practiceEvent.canPickupItems()) {
                    event.setCancelled(true);
                }
            }
        }
    }
}
