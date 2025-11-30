package rip.diamond.practice.match;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.v1_8_R3.EntityLightning;
import net.minecraft.server.v1_8_R3.PacketPlayOutSpawnEntityWeather;
import net.minecraft.server.v1_8_R3.PacketPlayOutTitle;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import rip.diamond.practice.Eden;
import rip.diamond.practice.arenas.ArenaDetail;
import rip.diamond.practice.config.Config;
import rip.diamond.practice.config.EdenSound;
import rip.diamond.practice.config.Language;
import rip.diamond.practice.event.MatchEndEvent;
import rip.diamond.practice.event.MatchPlayerDeathEvent;
import rip.diamond.practice.event.MatchStartEvent;
import rip.diamond.practice.event.MatchStateChangeEvent;
import rip.diamond.practice.kits.Kit;
import rip.diamond.practice.kits.KitLoadout;
import rip.diamond.practice.match.task.*;
import rip.diamond.practice.match.team.Team;
import rip.diamond.practice.match.team.TeamColor;
import rip.diamond.practice.match.team.TeamPlayer;
import rip.diamond.practice.profile.PlayerProfile;
import rip.diamond.practice.profile.PlayerState;
import rip.diamond.practice.profile.ProfileSettings;
import rip.diamond.practice.profile.cooldown.Cooldown;
import rip.diamond.practice.profile.cooldown.CooldownType;
import rip.diamond.practice.profile.task.ProfileCooldownTask;
import rip.diamond.practice.profile.data.ProfileKitData;
import rip.diamond.practice.queue.QueueType;
import rip.diamond.practice.util.*;
import rip.diamond.practice.util.exception.PracticeUnexpectedException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Getter
public abstract class Match {

    protected final Eden plugin = Eden.INSTANCE;

    @Getter
    private static final Map<UUID, Match> matches = new ConcurrentHashMap<>();
    @Getter
    private static final Map<UUID, PostMatchInventory> postMatchInventories = new HashMap<>();

    private final UUID uuid = UUID.randomUUID();
    @Setter
    private boolean duel = true;
    @Setter
    private QueueType queueType = QueueType.UNRANKED;
    protected final ArenaDetail arenaDetail;
    private final Kit kit;
    private final List<Team> teams;
    private MatchState state = MatchState.STARTING;
    private final List<UUID> spectators = new ArrayList<>();
    private final List<MatchEntity> entities = new ArrayList<>();
    private final List<Location> placedBlocks = new ArrayList<>();
    private final List<TaskTicker> tasks = new ArrayList<>();

    private final long startTimestamp = System.currentTimeMillis();

    public static void init() {
        new MatchClearItemTask();
        new MatchPostMatchInventoriesClearTask();
        new ProfileCooldownTask();
    }

    /**
     * Update Kit references in active Matches after kit reload
     * This is critical for ensuring leaderboard updates work correctly after reload
     * Note: Only updates the kit reference if the kit name matches - does not
     * reload match rules mid-game
     */
    public static void updateKitReferences() {
        for (Match match : matches.values()) {
            Kit oldKit = match.getKit();
            Kit newKit = Kit.getKits().stream()
                    .filter(k -> k.getName().equals(oldKit.getName()))
                    .findFirst()
                    .orElse(null);

            if (newKit != null && newKit != oldKit) {
                try {
                    java.lang.reflect.Field kitField = Match.class.getDeclaredField("kit");
                    kitField.setAccessible(true);
                    kitField.set(match, newKit);
                } catch (Exception e) {
                    Common.log("&cFailed to update kit reference for match " + match.getUuid());
                }
            }
        }
    }

    public Match(ArenaDetail arenaDetail, Kit kit, List<Team> teams) {
        this.arenaDetail = arenaDetail;
        this.kit = kit;
        this.teams = teams;

        matches.put(uuid, this);
    }

    /**
     * Start the match
     */
    public void start() {
        setupTeamSpawnLocation();

        if (kit.getGameRules().isBuild() || kit.getGameRules().isSpleef()) {
            if (Match.getMatches().values().stream().filter(match -> match != this).anyMatch(
                    match -> (match.getKit().getGameRules().isBuild() || match.getKit().getGameRules().isSpleef())
                            && match.getArenaDetail() == arenaDetail)) {
                end(true, "其他戰鬥正在使用這個場地, 並且該戰鬥的職業需要使用方塊");
                return;
            }
            arenaDetail.setUsing(true);
        }

        for (Player player : getMatchPlayers()) {
            PlayerProfile profile = PlayerProfile.get(player);
            profile.setMatch(this);
            profile.setPlayerState(PlayerState.IN_MATCH);

            PlayerUtil.reset(player);

            player.setSaturation(Config.MATCH_START_SATURATION.toInteger());
            player.addPotionEffects(kit.getEffects());
            player.setMaximumNoDamageTicks(kit.getDamageTicks());

            if (kit.getGameRules().isReceiveKitLoadoutBook()) {
                ProfileKitData kitData = profile.getKitData().get(kit.getName());
                int savedKitsCount = 0;
                KitLoadout firstSavedKit = null;

                if (kitData != null && kitData.getLoadouts() != null) {
                    for (KitLoadout loadout : kitData.getLoadouts()) {
                        if (loadout != null) {
                            savedKitsCount++;
                            if (firstSavedKit == null) {
                                firstSavedKit = loadout;
                            }
                        }
                    }
                }

                if (savedKitsCount >= 2) {
                    for (ItemStack itemStack : kitData.getKitItems(kit)) {
                        if (itemStack.getType().equals(Material.BOOK)) {
                            player.getInventory().setItem(8, itemStack);
                        } else {
                            player.getInventory().addItem(itemStack);
                        }
                    }
                } else if (savedKitsCount == 1) {
                    firstSavedKit.apply(kit, this, player);
                    Language.MATCH_AUTO_EQUIPPED_KIT.sendMessage(player, firstSavedKit.getCustomName());
                    rip.diamond.practice.match.kit.DynamicKitManager.getInstance().addSession(player, this, kit,
                            firstSavedKit);
                } else {
                    kit.getKitLoadout().apply(kit, this, player);
                    Language.MATCH_AUTO_EQUIPPED_KIT.sendMessage(player, kit.getKitLoadout().getCustomName());
                    rip.diamond.practice.match.kit.DynamicKitManager.getInstance().addSession(player, this, kit,
                            kit.getKitLoadout());
                }
            } else {
                kit.getKitLoadout().apply(kit, this, player);
            }
            player.updateInventory();

            if (kit.getGameRules().isShowHealth()) {
                plugin.getScoreboardHandler().getScoreboard(player).registerHealthObjective();
                player.setHealth(player.getMaxHealth() - 0.001);
            }

            plugin.getSpigotAPI().getKnockback().applyKnockback(player, kit.getGameRules().getKnockbackName());
        }

        teams.forEach(team -> team.teleport(team.getSpawnLocation()));

        if (getMatchType() != MatchType.FFA) {
            for (int i = 0; i < teams.size(); i++) {
                Team team = teams.get(i);
                team.setTeamColor(TeamColor.values()[i]);
                team.getTeamPlayers().forEach(team::dye);
            }
        }

        MatchStartEvent event = new MatchStartEvent(this);
        event.call();

        showStartingHolograms();

        new MatchNewRoundTask(this, null, false);
        new MatchBuildHeightDamageTask(this);

        rip.diamond.practice.arenas.ActiveArenaTracker.add(arenaDetail);
    }

    /**
     * @param profile A random profile from match players which is alive. This is
     *                used to create a score cooldown
     * @param scorer  The TeamPlayer who scored the point
     */
    public void score(PlayerProfile profile, TeamPlayer entity, TeamPlayer scorer) {
        getMatchPlayers().stream().map(PlayerProfile::get)
                .filter(p -> !p.getCooldowns().get(CooldownType.SCORE).isExpired()).findFirst()
                .ifPresent(lastScorerProfile -> Common.log("[Eden] " + scorer.getUsername() + " tries to score when "
                        + lastScorerProfile.getUsername() + " scored in last 3 seconds (UUID: " + uuid + ")"));

        profile.getCooldowns().put(CooldownType.SCORE, new Cooldown(3));

        Team team = getTeam(scorer);
        team.handlePoint();
        if (state == MatchState.FIGHTING && team.getPoints() < kit.getGameRules().getMaximumPoints()) {
            if (kit.getGameRules().isOnlyLoserResetPositionWhenGetPoint()) {
                new MatchRespawnTask(this, entity);
                return;
            }
            new MatchFireworkTask(team.getTeamColor().getDyeColor().getColor(), this);
            new MatchNewRoundTask(this, scorer, true);
            return;
        }

        getOpponentTeam(team).getAliveTeamPlayers().forEach(teamTarget -> die(teamTarget.getPlayer(), false));
    }

    public void die(Player deadPlayer, boolean disconnected) {
        TeamPlayer teamPlayer = getTeamPlayer(deadPlayer);
        PlayerProfile profile = PlayerProfile.get(deadPlayer);
        Team team = getTeam(deadPlayer);

        teamPlayer.setDisconnected(disconnected);

        if (!teamPlayer.isAlive()) {
            return;
        }

        teamPlayer.setAlive(false);
        getMatchPlayers().forEach(VisibilityController::updateVisibility);

        PostMatchInventory postMatchInventory = new PostMatchInventory(teamPlayer);
        postMatchInventories.put(teamPlayer.getUuid(), postMatchInventory);

        displayDeathMessage(teamPlayer, deadPlayer);

        MatchPlayerDeathEvent event = new MatchPlayerDeathEvent(this, deadPlayer);
        event.call();
        if (event.isPlayLightningEffect() && Config.MATCH_DEATH_LIGHTNING.toBoolean()) {
            EntityLightning lightning = new EntityLightning(((CraftPlayer) deadPlayer).getHandle().getWorld(),
                    deadPlayer.getLocation().getX(), deadPlayer.getLocation().getY(), deadPlayer.getLocation().getZ(),
                    true, true);
            for (Player player : getPlayersAndSpectators()) {
                ((CraftPlayer) player).getHandle().playerConnection
                        .sendPacket(new PacketPlayOutSpawnEntityWeather(lightning));
                EdenSound.FINAL_DEATH_SOUND.play(player);
            }
        }
        if (event.isPlayDeathEffect() && Config.MATCH_DEATH_ANIMATION.toBoolean()) {
            Util.playDeathAnimation(deadPlayer, getPlayersAndSpectators().stream()
                    .filter(player -> player != deadPlayer).collect(Collectors.toList()));
        }

        if (canEnd()) {
            end();
        } else if (!disconnected) {
            PlayerUtil.spectator(deadPlayer);
            Tasks.runLater(profile::setupItems, 1L);
        }
    }

    public void respawn(TeamPlayer teamPlayer) {
        Player player = teamPlayer.getPlayer();
        Team team = getTeam(player);
        team.getSpawnLocation().clone().add(0, 0, 0).getBlock().setType(Material.AIR);
        team.getSpawnLocation().clone().add(0, 1, 0).getBlock().setType(Material.AIR);
        Util.teleport(player, team.getSpawnLocation());
        player.setAllowFlight(false);
        player.setFlying(false);
        teamPlayer.setRespawning(false);
        getMatchPlayers().forEach(VisibilityController::updateVisibility);

        PlayerProfile profile = PlayerProfile.get(player);

        profile.getCooldowns().get(CooldownType.ARROW).cancelCountdown();

        player.setExp(0);
        player.setLevel(0);

        teamPlayer.setProtectionUntil(System.currentTimeMillis() + (3 * 1000));
        teamPlayer.respawn(this);
        Language.MATCH_RESPAWN_MESSAGE.sendMessage(player);
    }

    public void displayDeathMessage(TeamPlayer teamPlayer, Player deadPlayer) {
        if (teamPlayer.isDisconnected()) {
            getPlayersAndSpectators().forEach(player -> Language.MATCH_DEATH_MESSAGE_DISCONNECT.sendMessage(player,
                    getRelationColor(player, deadPlayer),
                    teamPlayer.getUsername(),
                    getTeam(teamPlayer).getTeamColor().getColor()));
        } else if (teamPlayer.getLastHitDamager() != null && teamPlayer.getLastHitDamager().getPlayer() != null) {
            getPlayersAndSpectators().forEach(player -> Language.MATCH_DEATH_MESSAGE_KILLED.sendMessage(player,
                    getRelationColor(player, deadPlayer),
                    teamPlayer.getUsername(),
                    getRelationColor(player, teamPlayer.getLastHitDamager().getPlayer()),
                    teamPlayer.getLastHitDamager().getUsername(),
                    getTeam(teamPlayer).getTeamColor().getColor(),
                    getTeam(teamPlayer.getLastHitDamager()).getTeamColor().getColor()));
        } else {
            getPlayersAndSpectators().forEach(player -> Language.MATCH_DEATH_MESSAGE_DEFAULT.sendMessage(player,
                    getRelationColor(player, deadPlayer),
                    teamPlayer.getUsername(),
                    getTeam(teamPlayer).getTeamColor().getColor()));
        }
    }

    public void end() {
        end(false, null);
    }

    public void end(boolean forced, String reason) {
        if (state == MatchState.ENDING) {
            return;
        }

        Common.debug("正在結束 " + getClass().getSimpleName() + " 戰鬥 ("
                + teams.stream().map(team -> team.getLeader().getUsername()).collect(Collectors.joining(" vs "))
                + ") (職業: " + kit.getName() + ") (地圖: " + arenaDetail.getArena().getName() + ") (UUID: " + uuid + ")");
        state = MatchState.ENDING;

        MatchEndEvent event = new MatchEndEvent(this, forced);
        event.call();

        if (forced) {
            broadcastMessage(Language.MATCH_FORCE_END_MESSAGE.toString(reason));
        } else {

            for (TeamPlayer teamPlayer : getWinningPlayers()) {
                PostMatchInventory postMatchInventory = new PostMatchInventory(teamPlayer);
                postMatchInventories.put(teamPlayer.getUuid(), postMatchInventory);
            }

            for (Player player : getMatchPlayers()) {
                plugin.getScoreboardHandler().getScoreboard(player).unregisterHealthObjective();
            }

            if (Config.MATCH_TITLE_END.toBoolean()) {
                displayMatchEndTitle();
            }
            displayMatchEndMessages();
            calculateMatchStats();
        }

        if (kit.getGameRules().isPortalGoal()) {
            getTeams().forEach(t -> t.teleport(t.getSpawnLocation()));
        }

        removeStartingHolograms();

        new MatchResetTask(this);
    }

    public void joinSpectate(Player player, Player target) {
        PlayerProfile profile = PlayerProfile.get(player);

        spectators.add(player.getUniqueId());

        getPlayersAndSpectators().forEach(other -> {

            if (other != null && !Util.isNPC(other)) {
                PlayerProfile otherProfile = PlayerProfile.get(other);
                if (otherProfile.getSettings().get(ProfileSettings.SPECTATOR_JOIN_LEAVE_MESSAGE).isEnabled()) {
                    Common.sendMessage(other, Language.MATCH_JOIN_SPECTATE.toString(player.getName()));
                }
            }
        });

        profile.setMatch(this);
        profile.setPlayerState(PlayerState.IN_SPECTATING);
        PlayerUtil.spectator(player);
        profile.setupItems();

        Util.teleport(player, getArenaDetail().getSpectator());

        if (kit.getGameRules().isShowHealth()) {
            plugin.getScoreboardHandler().getScoreboard(player).registerHealthObjective();
        }
    }

    public void leaveSpectate(Player player) {
        spectators.remove(player.getUniqueId());

        getPlayersAndSpectators().forEach(other -> {

            if (other != null && !Util.isNPC(other)) {
                PlayerProfile otherProfile = PlayerProfile.get(other);
                if (otherProfile.getSettings().get(ProfileSettings.SPECTATOR_JOIN_LEAVE_MESSAGE).isEnabled()) {
                    Common.sendMessage(other, Language.MATCH_LEAVE_SPECTATE.toString(player.getName()));
                }
            }
        });

        plugin.getScoreboardHandler().getScoreboard(player).unregisterHealthObjective();
        plugin.getLobbyManager().sendToSpawnAndReset(player);
    }

    public void addDroppedItem(Item item, String whoDropped) {
        if (whoDropped != null) {
            plugin.getEntityHider().setPlayerWhoDropped(item, whoDropped);
        }
        getEntities().add(new MatchEntity(this, item));
    }

    public void clearEntities(boolean forced) {
        Iterator<MatchEntity> iterator = entities.iterator();
        while (iterator.hasNext()) {
            MatchEntity matchEntity = iterator.next();
            if (forced || System.currentTimeMillis() - matchEntity.getTimestamp() >= 10000) {
                matchEntity.getEntity().remove();
                iterator.remove();
            }
        }
    }

    public void setState(MatchState state) {
        this.state = state;

        getPlayersAndSpectators().forEach(VisibilityController::updateVisibility);

        MatchStateChangeEvent event = new MatchStateChangeEvent(this);
        Bukkit.getPluginManager().callEvent(event);
    }

    public boolean canEnd() {
        return teams.stream().filter(team -> !team.isEliminated()).count() <= 1;
    }

    public boolean isProtected(Location location, boolean isPlacing) {
        return isProtected(location, isPlacing, null);
    }

    public boolean isProtected(Location location, boolean isPlacing, Block block) {
        if (block != null && block.getType() == Material.TNT && Config.MATCH_TNT_ENABLED.toBoolean()) {

            return false;
        }
        if (location.getBlockY() >= arenaDetail.getArena().getBuildMax()
                || location.getBlockY() <= arenaDetail.getArena().getYLimit()) {
            return true;
        }
        if (!arenaDetail.getCuboid().contains(location)) {
            return true;
        }
        if (kit.getGameRules().isSpleef()) {
            return location.getBlock().getType() != Material.SNOW_BLOCK
                    && location.getBlock().getType() != Material.SAND;
        }
        if (kit.getGameRules().isBed()) {
            switch (location.getBlock().getType()) {
                case BED_BLOCK:
                case WOOD:
                case ENDER_STONE:
                    return false;
                default:
                    break;
            }
        }
        if (kit.getGameRules().isBreakGoal() && location.getBlock().getType() == Material.BED_BLOCK) {
            return false;
        }
        if (kit.getGameRules().isPortalGoal()) {
            long count = Util.getBlocksAroundCenter(location, arenaDetail.getArena().getPortalProtectionRadius())
                    .stream().filter(b -> b.getType() == Material.ENDER_PORTAL).count();
            if (count > 0) {
                return true;
            }
            if (location.getBlock().getType() == Material.STAINED_CLAY && (location.getBlock().getData() == 0
                    || location.getBlock().getData() == 11 || location.getBlock().getData() == 14)) {
                return false;
            }
        }
        if (Config.MATCH_ALLOW_BREAKING_BLOCKS.toStringList().contains(location.getBlock().getType().name())) {
            return false;
        }
        if (!isPlacing) {
            return !getPlacedBlocks().contains(location);
        }
        return false;
    }

    public String getRelationColor(Player viewer, Player target) {
        if (viewer.equals(target)) {
            return CC.GREEN;
        }

        Team team = getTeam(target);
        Team viewerTeam = getTeam(viewer);

        if (team == null || viewerTeam == null) {
            return Config.NAMETAG_PREFIX_OTHER.toString();
        }

        if (team.equals(viewerTeam)) {
            return Config.NAMETAG_PREFIX_TEAMMATE.toString();
        } else {
            return Config.NAMETAG_PREFIX_OPPONENT.toString();
        }
    }

    /**
     * @return A list of players who are in the match, without spectators and
     *         disconnected players
     */
    public List<Player> getMatchPlayers() {
        List<Player> players = new ArrayList<>();
        teams.forEach(team -> players.addAll(team.getTeamPlayers().stream()

                .filter(tP -> !tP.isDisconnected())

                .map(TeamPlayer::getPlayer)

                .filter(Objects::nonNull)
                .collect(Collectors.toList())));
        return players;
    }

    public List<Player> getSpectators() {
        List<Player> players = new ArrayList<>();
        spectators.forEach(spectatorUUID -> players.add(Bukkit.getPlayer(spectatorUUID)));
        return players;
    }

    public List<Player> getPlayersAndSpectators() {
        List<Player> players = new ArrayList<>(getMatchPlayers());
        players.addAll(getSpectators());
        return players;
    }

    public Team getTeam(TeamPlayer player) {
        for (Team team : teams) {
            if (team.getTeamPlayers().stream().filter(Objects::nonNull).collect(Collectors.toList()).contains(player)) {
                return team;
            }
        }
        return null;
    }

    public Team getTeam(Player player) {
        for (Team team : teams) {
            if (team.getTeamPlayers().stream().map(TeamPlayer::getPlayer).filter(Objects::nonNull)
                    .collect(Collectors.toList()).contains(player)) {
                return team;
            }
        }
        return null;
    }

    public TeamPlayer getTeamPlayer(Player player) {
        return getTeamPlayer(player.getUniqueId());
    }

    public TeamPlayer getTeamPlayer(UUID uuid) {
        for (Team team : teams) {
            for (TeamPlayer teamPlayer : team.getTeamPlayers()) {
                if (teamPlayer.getUuid().equals(uuid)) {
                    return teamPlayer;
                }
            }
        }
        return null;
    }

    public List<TeamPlayer> getTeamPlayers() {
        List<TeamPlayer> players = new ArrayList<>();
        teams.stream().map(Team::getTeamPlayers).forEach(players::addAll);
        return players.stream().filter(teamPlayer -> !teamPlayer.isDisconnected()).collect(Collectors.toList());
    }

    public int getMaximumBoxingHits() {
        if (!kit.getGameRules().isBoxing()) {
            throw new PracticeUnexpectedException("Kit type is not boxing");
        }
        Team team = getTeams().stream().max(Comparator.comparing(t -> t.getTeamPlayers().size())).orElse(null);
        if (team == null) {
            throw new PracticeUnexpectedException(
                    "Cannot find a suitable team to calculate the maximum allowed hits in boxing");
        }
        return team.getTeamPlayers().size() * 100;
    }

    public long getElapsedDuration() {
        return System.currentTimeMillis() - startTimestamp;
    }

    public void broadcastMessage(String... message) {
        getPlayersAndSpectators().forEach(player -> Common.sendMessage(player, message));
    }

    public void broadcastMessage(List<String> messages) {
        getPlayersAndSpectators().forEach(player -> Common.sendMessage(player, messages));
    }

    public void broadcastTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        getPlayersAndSpectators().forEach(player -> {
            TitleSender.sendTitle(player, title, PacketPlayOutTitle.EnumTitleAction.TITLE, fadeIn, stay, fadeOut);
            TitleSender.sendTitle(player, subtitle, PacketPlayOutTitle.EnumTitleAction.SUBTITLE, fadeIn, stay, fadeOut);
        });
    }

    public void broadcastTitle(String message) {
        getPlayersAndSpectators().forEach(
                player -> TitleSender.sendTitle(player, message, PacketPlayOutTitle.EnumTitleAction.TITLE, 0, 21, 5));
    }

    public void broadcastSubTitle(String message) {
        getPlayersAndSpectators().forEach(player -> TitleSender.sendTitle(player, message,
                PacketPlayOutTitle.EnumTitleAction.SUBTITLE, 0, 21, 5));
    }

    public void broadcastTitle(Team team, String message) {
        for (TeamPlayer teamPlayer : team.getTeamPlayers())
            if (teamPlayer.getPlayer() != null)
                TitleSender.sendTitle(teamPlayer.getPlayer(), message, PacketPlayOutTitle.EnumTitleAction.TITLE, 0, 21,
                        5);
    }

    public void broadcastSubTitle(Team team, String message) {
        for (TeamPlayer teamPlayer : team.getTeamPlayers())
            if (teamPlayer.getPlayer() != null)
                TitleSender.sendTitle(teamPlayer.getPlayer(), message, PacketPlayOutTitle.EnumTitleAction.SUBTITLE, 0,
                        21, 5);
    }

    public void broadcastSound(Sound sound) {
        getPlayersAndSpectators().forEach(player -> player.playSound(player.getLocation(), sound, 10, 1));
    }

    public void broadcastSound(EdenSound sound) {
        getPlayersAndSpectators().forEach(player -> sound.play(player, player.getLocation()));
    }

    public void broadcastSound(Team team, Sound sound) {
        team.getTeamPlayers().stream().map(TeamPlayer::getPlayer).filter(Objects::nonNull)
                .forEach(player -> player.playSound(player.getLocation(), sound, 10, 1));
    }

    public void broadcastSound(Team team, EdenSound sound) {
        team.getTeamPlayers().stream().map(TeamPlayer::getPlayer).filter(Objects::nonNull)
                .forEach(player -> sound.play(player, player.getLocation()));
    }

    public void broadcastSpectatorsSound(Sound sound) {
        getSpectators().forEach(player -> player.playSound(player.getLocation(), sound, 10, 1));
    }

    public void broadcastSpectatorsSound(EdenSound sound) {
        getSpectators().forEach(sound::play);
    }

    public abstract void setupTeamSpawnLocation();

    public abstract void displayMatchEndMessages();

    public abstract void displayMatchEndTitle();

    public abstract void calculateMatchStats();

    public abstract MatchType getMatchType();

    public abstract Team getOpponentTeam(Team team);

    public abstract TeamPlayer getOpponent(TeamPlayer teamPlayer);

    public abstract List<TeamPlayer> getWinningPlayers();

    public abstract Team getWinningTeam();

    public abstract List<String> getMatchScoreboard(Player player);

    public abstract List<String> getSpectateScoreboard(Player player);

    private final List<rip.diamond.practice.leaderboard.hologram.PracticeHologram> startingHolograms = new ArrayList<>();

    public void showStartingHolograms() {
        System.out.println("MatchStartingHologram: showStartingHolograms called for match " + uuid);
        if (!kit.getGameRules().isShowMatchStartScoreboard()) {
            System.out.println("MatchStartingHologram: Rule disabled for kit " + kit.getName());
            return;
        }
        System.out.println("MatchStartingHologram: Showing holograms for match " + uuid);
        for (Team team : getTeams()) {
            createStartingHologram(team, "left");
            createStartingHologram(team, "right");
        }
    }

    private void createStartingHologram(Team team, String side) {
        rip.diamond.practice.util.file.ConfigCursor config = new rip.diamond.practice.util.file.ConfigCursor(
                Eden.INSTANCE.getLeaderboardsConfig(), "match-starting-holograms." + side);
        if (!config.exists("type")) {
            System.out.println("MatchStartingHologram: Config type not found for side " + side);
            return;
        }

        rip.diamond.practice.leaderboard.LeaderboardType type;
        try {
            type = rip.diamond.practice.leaderboard.LeaderboardType.valueOf(config.getString("type"));
        } catch (IllegalArgumentException e) {
            System.out.println("MatchStartingHologram: Invalid type " + config.getString("type"));
            return;
        }

        double rightOffset = config.getDouble("offset.right");
        double frontOffset = config.getDouble("offset.front");
        double upOffset = config.getDouble("offset.up");

        Location loc = team.getSpawnLocation();

        System.out.println("MatchStartingHologram: Team " + team.getTeamColor() + " spawn location: " +
                loc.getX() + ", " + loc.getY() + ", " + loc.getZ() +
                " (yaw: " + loc.getYaw() + ", pitch: " + loc.getPitch() + ") in world " + loc.getWorld().getName());

        org.bukkit.util.Vector dir = loc.getDirection().setY(0).normalize();
        System.out.println(
                "MatchStartingHologram: Direction vector: " + dir.getX() + ", " + dir.getY() + ", " + dir.getZ());

        org.bukkit.util.Vector right = dir.clone().crossProduct(new org.bukkit.util.Vector(0, 1, 0)).normalize();

        Location hologramLoc = loc.clone()
                .add(dir.multiply(frontOffset))
                .add(right.multiply(rightOffset))
                .add(0, upOffset, 0);

        System.out.println("MatchStartingHologram: Calculated hologram location: " +
                hologramLoc.getX() + ", " + hologramLoc.getY() + ", " + hologramLoc.getZ());

        rip.diamond.practice.leaderboard.hologram.impl.MatchStartingHologram hologram = new rip.diamond.practice.leaderboard.hologram.impl.MatchStartingHologram(
                hologramLoc, 20, type, type.getTimePeriod(), getKit(), team, side);
        hologram.start();
        startingHolograms.add(hologram);
    }

    public void removeStartingHolograms() {
        startingHolograms.forEach(rip.diamond.practice.leaderboard.hologram.PracticeHologram::stop);
        startingHolograms.clear();
    }
}
