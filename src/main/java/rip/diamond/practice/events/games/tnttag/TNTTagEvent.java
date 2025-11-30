package rip.diamond.practice.events.games.tnttag;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import rip.diamond.practice.Eden;
import rip.diamond.practice.events.EventCountdownTask;
import rip.diamond.practice.events.EventState;
import rip.diamond.practice.events.PracticeEvent;
import rip.diamond.practice.util.CC;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Getter
public class TNTTagEvent extends PracticeEvent<TNTTagPlayer> {

    private final Map<UUID, TNTTagPlayer> players = new HashMap<>();
    private TNTTagCountdown eventCountdown;
    private TNTTagGameTask gameTask;
    private int round = 0;
    private Player winner;
    private List<TNTTagPlayer> topPlayers = new ArrayList<>();

    public TNTTagEvent() {
        super("TNT Tag");
        this.eventCountdown = new TNTTagCountdown(this, 2);
    }

    @Override
    public Map<UUID, TNTTagPlayer> getPlayers() {
        return players;
    }

    @Override
    public EventCountdownTask getCountdownTask() {

        if (eventCountdown == null || eventCountdown.isEnded()) {
            eventCountdown = new TNTTagCountdown(this, 2);
        }
        return eventCountdown;
    }

    @Override
    public List<Location> getSpawnLocations() {
        return Collections.singletonList(plugin.getSpawnManager().getTntTagGameLocation());
    }

    @Override
    public List<String> getScoreboard(Player player) {
        List<String> lines;

        if (getState() == EventState.WAITING || getState() == EventState.STARTING) {
            lines = new ArrayList<>(Eden.INSTANCE.getScoreboardFile().getLines("TNTTAG", "WAITING"));
        } else {

            lines = new ArrayList<>(Eden.INSTANCE.getScoreboardFile().getLines("TNTTAG", "PLAYING"));
        }


        List<String> replaced = new ArrayList<>();
        for (String line : lines) {

            line = line.replace("{event_name}", getName());
            line = line.replace("{event_players}", String.valueOf(getPlayers().size()));
            line = line.replace("{event_limit}", String.valueOf(getLimit()));
            line = line.replace("{event_host}", getHost() != null ? getHost().getName() : "None");


            if (getState() == EventState.PLAYING) {
                line = line.replace("{round}", String.valueOf(round));
                line = line.replace("{players_remaining}",
                        String.valueOf(getByState(TNTTagPlayer.TNTTagState.INGAME).size()
                                + getByState(TNTTagPlayer.TNTTagState.TAGGED).size()));
                line = line.replace("{players_eliminated}",
                        String.valueOf(getByState(TNTTagPlayer.TNTTagState.ELIMINATED).size()));

                if (gameTask != null) {
                    line = line.replace("{time}", String.valueOf(gameTask.time));
                } else {
                    line = line.replace("{time}", "0");
                }
            } else if (getState() == EventState.ENDING) {
                line = line.replace("{winner_name}", winner != null ? winner.getName() : "None");
                line = line.replace("{round}", String.valueOf(round));
                line = line.replace("{players_remaining}", "0");
                line = line.replace("{players_eliminated}", String.valueOf(getPlayers().size()));
                line = line.replace("{time}", "0");
            } else {
                line = line.replace("{round}", "0");
                line = line.replace("{players_remaining}", String.valueOf(getPlayers().size()));
                line = line.replace("{players_eliminated}", "0");
                line = line.replace("{time}", "0");
            }

            replaced.add(CC.translate(line));
        }

        return replaced;
    }

    @Override
    public void onStart() {
        gameTask = new TNTTagGameTask();
        gameTask.runTaskTimer(plugin, 0, 20L);
    }

    @Override
    public Consumer<Player> onJoin() {
        return player -> players.put(player.getUniqueId(), new TNTTagPlayer(player.getUniqueId(), this));
    }

    @Override
    public Consumer<Player> onDeath() {
        return player -> {
            TNTTagPlayer data = getPlayer(player);
            if (data.getState() != TNTTagPlayer.TNTTagState.INGAME
                    && data.getState() != TNTTagPlayer.TNTTagState.TAGGED) {
                return;
            }

            data.setState(TNTTagPlayer.TNTTagState.ELIMINATED);
            getPlayers().remove(player.getUniqueId());
            plugin.getEventManager().addSpectator(player,
                    rip.diamond.practice.profile.PlayerProfile.get(player), this);

            checkWin();
        };
    }

    private void preEnd(Player winner) {
        if (getState() == EventState.ENDING)
            return;
        this.winner = winner;
        setState(EventState.ENDING);

        if (winner != null) {
            handleWin(winner);
        }


        Location lobby = getSpawnLocations().isEmpty() ? null : getSpawnLocations().get(0);
        if (lobby != null) {
            for (Player p : getBukkitPlayers()) {
                p.teleport(lobby);
                rip.diamond.practice.util.PlayerUtil.reset(p);
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, this::end, 60L);
    }

    private void checkWin() {
        List<TNTTagPlayer> playing = getByState(TNTTagPlayer.TNTTagState.INGAME);
        playing.addAll(getByState(TNTTagPlayer.TNTTagState.TAGGED));

        if (playing.size() <= 1) {
            Player winner = playing.isEmpty() ? null : Bukkit.getPlayer(playing.get(0).getUuid());
            preEnd(winner);
        }
    }

    @Override
    public void leave(Player player) {
        TNTTagPlayer tntTagPlayer = getPlayer(player);
        if (tntTagPlayer != null) {
            if (tntTagPlayer.getState() == TNTTagPlayer.TNTTagState.INGAME
                    || tntTagPlayer.getState() == TNTTagPlayer.TNTTagState.TAGGED) {
                tntTagPlayer.setState(TNTTagPlayer.TNTTagState.ELIMINATED);
                sendMessage(CC.translate("&c" + player.getName() + " &edisconnected."));
            }
        }

        super.leave(player);

        if (getState() == EventState.PLAYING) {
            checkWin();
        }
    }

    public void tagPlayer(Player victim, Player attacker) {
        TNTTagPlayer victimData = getPlayer(victim);
        TNTTagPlayer attackerData = getPlayer(attacker);

        if (victimData != null && attackerData != null) {
            if (getState() != EventState.PLAYING) {
                return;
            }

            if (victimData.getState() == TNTTagPlayer.TNTTagState.ELIMINATED
                    || attackerData.getState() == TNTTagPlayer.TNTTagState.ELIMINATED) {
                return;
            }
            attackerData.setState(TNTTagPlayer.TNTTagState.INGAME);
            victimData.setState(TNTTagPlayer.TNTTagState.TAGGED);

            attacker.getInventory().clear();
            victim.getInventory().setItem(0, new ItemStack(Material.TNT));
            victim.updateInventory();

            sendMessage(CC.translate("&c" + attacker.getName() + " &etagged &c" + victim.getName() + "&e!"));
            attacker.sendMessage(CC.GREEN + "You tagged " + victim.getName() + "!");
            victim.sendMessage(CC.RED + "You were tagged by " + attacker.getName() + "!");
        }
    }

    public List<TNTTagPlayer> getByState(TNTTagPlayer.TNTTagState state) {
        return players.values().stream().filter(player -> player.getState() == state)
                .collect(Collectors.toList());
    }

    @Override
    public void end() {

        if (gameTask != null) {
            gameTask.cancel();
        }


        for (Player player : getBukkitPlayers()) {
            plugin.getScoreboardHandler().getScoreboard(player).unregisterHealthObjective();
        }


        super.end();
    }

    @Getter
    @RequiredArgsConstructor
    public class TNTTagGameTask extends BukkitRunnable {

        private int time = 30;
        private boolean roundActive = false;

        @Override
        public void run() {
            if (!roundActive) {
                startRound();
                return;
            }

            if (time <= 0) {
                eliminateTaggedPlayers();
                roundActive = false;
                time = 30;
                return;
            }

            if (time <= 10 || time % 10 == 0) {
                sendMessage(CC.translate("&eTNT explosion in &c" + time + " &eseconds!"));
            }

            time--;
        }

        private void startRound() {
            List<TNTTagPlayer> playing = getByState(TNTTagPlayer.TNTTagState.INGAME);
            playing.addAll(getByState(TNTTagPlayer.TNTTagState.TAGGED));

            if (playing.size() <= 1) {
                checkWin();
                return;
            }

            round++;


            if (round == 1) {
                setState(rip.diamond.practice.events.EventState.PLAYING);

                for (Player player : getBukkitPlayers()) {
                    plugin.getScoreboardHandler().getScoreboard(player).registerHealthObjective();
                }
            }


            for (Player player : getBukkitPlayers()) {
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED,
                        Integer.MAX_VALUE, 2));
            }

            sendMessage(CC.translate("&eRound &c" + round + " &ehas started!"));


            int amountToTag = (int) Math.ceil(playing.size() / 3.0);
            Collections.shuffle(playing);

            for (int i = 0; i < amountToTag; i++) {
                TNTTagPlayer tagged = playing.get(i);
                tagged.setState(TNTTagPlayer.TNTTagState.TAGGED);
                Player player = Bukkit.getPlayer(tagged.getUuid());
                if (player != null) {
                    player.getInventory().setItem(0, new ItemStack(Material.TNT));
                    player.sendMessage(CC.RED + "You are IT! Tag someone else!");
                }
            }


            for (int i = amountToTag; i < playing.size(); i++) {
                playing.get(i).setState(TNTTagPlayer.TNTTagState.INGAME);
            }

            roundActive = true;
        }

        private void eliminateTaggedPlayers() {
            List<TNTTagPlayer> tagged = new ArrayList<>(getByState(TNTTagPlayer.TNTTagState.TAGGED));


            for (TNTTagPlayer player : tagged) {
                Player p = Bukkit.getPlayer(player.getUuid());
                if (p != null) {
                    p.getWorld().createExplosion(p.getLocation(), 0F);
                    player.setState(TNTTagPlayer.TNTTagState.ELIMINATED);
                    getPlayers().remove(player.getUuid());
                    plugin.getEventManager().addSpectator(p,
                            rip.diamond.practice.profile.PlayerProfile.get(p), TNTTagEvent.this);
                }
            }


            checkWin();
        }
    }

    public class TNTTagCountdown extends EventCountdownTask {
        private final int requiredPlayers;

        public TNTTagCountdown(PracticeEvent<?> event, int requiredPlayers) {
            super(event, 60);
            this.requiredPlayers = requiredPlayers;
        }

        @Override
        public boolean shouldAnnounce(int timeUntilStart) {
            return Arrays.asList(60, 45, 30, 15, 10, 5).contains(timeUntilStart);
        }

        @Override
        public boolean canStart() {
            return this.getEvent().getPlayers().size() >= requiredPlayers;
        }

        @Override
        public void onCancel() {
            this.getEvent().sendMessage(CC.RED + "There were not enough players to start the event.");
            this.getEvent().end();
        }
    }
}
