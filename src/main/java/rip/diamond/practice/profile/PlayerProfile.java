package rip.diamond.practice.profile;

import lombok.Getter;
import lombok.Setter;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import rip.diamond.practice.Eden;
import rip.diamond.practice.EdenItems;
import rip.diamond.practice.event.PlayerProfileDataLoadEvent;
import rip.diamond.practice.event.PlayerProfileDataSaveEvent;
import rip.diamond.practice.events.PracticeEvent;
import rip.diamond.practice.kits.Kit;
import rip.diamond.practice.match.Match;
import rip.diamond.practice.party.Party;
import rip.diamond.practice.profile.cooldown.Cooldown;
import rip.diamond.practice.profile.cooldown.CooldownType;
import rip.diamond.practice.profile.data.ProfileKitData;
import rip.diamond.practice.profile.task.ProfileAutoSaveTask;
import rip.diamond.practice.util.*;
import rip.diamond.practice.util.option.Option;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Getter
public class PlayerProfile {

    @Getter
    private static final Map<UUID, PlayerProfile> profiles = new ConcurrentHashMap<>();

    private final UUID uniqueId;
    private final String username;
    private final Map<String, ProfileKitData> kitData = new HashMap<>();
    private final Map<ProfileSettings, Option> settings = new HashMap<>();
    private PlayerState playerState = PlayerState.LOADING;
    private final Map<CooldownType, Cooldown> cooldowns = new ConcurrentHashMap<>();
    @Setter
    private Match match;
    @Setter
    private Party party;

    @Getter
    @Setter
    private int eventWins = 0;
    @Getter
    @Setter
    private int dailyEventWins = 0;
    @Getter
    @Setter
    private int weeklyEventWins = 0;
    @Getter
    @Setter
    private int monthlyEventWins = 0;

    @Getter
    @Setter
    private long lastDailyEventReset = System.currentTimeMillis();
    @Getter
    @Setter
    private long lastWeeklyEventReset = System.currentTimeMillis();
    @Getter
    @Setter
    private long lastMonthlyEventReset = System.currentTimeMillis();

    @Getter
    @Setter
    private int eventsPlayed = 0;

    @Setter
    private boolean temporary = false;
    private boolean saving = false;

    public PlayerProfile(UUID uniqueId, String username) {
        this.uniqueId = uniqueId;
        this.username = username;
    }

    public static void init() {
        new ProfileAutoSaveTask();
    }

    public void fromBson(Document document) {
        Document settingsDocument = document.get("settings", Document.class);
        for (String data : settingsDocument.keySet()) {
            try {
                ProfileSettings s = ProfileSettings.valueOf(data);
                Option option = s.find(settingsDocument.getString(data));

                if (option == null) {
                    continue;
                }
                settings.put(s, option);
            } catch (IllegalArgumentException e) {
                Common.debug(username + " 的 SettingsDocument 裏面有不存在的 '" + data + "' 設定, 由於無法找到合適的設定, 所以已把它忽略");
            }
        }

        Document kitDataDocument = document.get("kitData", Document.class);
        for (String data : kitDataDocument.keySet()) {

            kitData.putIfAbsent(data, new ProfileKitData());
            kitData.get(data).fromBson(kitDataDocument.get(data, Document.class));
        }

        if (document.containsKey("eventWins")) {
            this.eventWins = document.getInteger("eventWins", 0);
        }
        if (document.containsKey("dailyEventWins")) {
            this.dailyEventWins = document.getInteger("dailyEventWins", 0);
        }
        if (document.containsKey("weeklyEventWins")) {
            this.weeklyEventWins = document.getInteger("weeklyEventWins", 0);
        }
        if (document.containsKey("monthlyEventWins")) {
            this.monthlyEventWins = document.getInteger("monthlyEventWins", 0);
        }

        if (document.containsKey("lastDailyEventReset")) {
            this.lastDailyEventReset = document.getLong("lastDailyEventReset");
        }
        if (document.containsKey("lastWeeklyEventReset")) {
            this.lastWeeklyEventReset = document.getLong("lastWeeklyEventReset");
        }
        if (document.containsKey("lastMonthlyEventReset")) {
            this.lastMonthlyEventReset = document.getLong("lastMonthlyEventReset");
        }

        if (document.containsKey("eventsPlayed")) {
            this.eventsPlayed = document.getInteger("eventsPlayed", 0);
        }

        PlayerProfileDataLoadEvent event = new PlayerProfileDataLoadEvent(this, document);
        event.call();
    }

    public Document toBson() {
        Document settingsDocument = new Document();
        for (Map.Entry<ProfileSettings, Option> options : settings.entrySet()) {
            settingsDocument.put(options.getKey().name(), options.getValue().getValue());
        }

        Document kitDataDocument = new Document();
        for (Map.Entry<String, ProfileKitData> kitDataMap : kitData.entrySet()) {
            kitDataDocument.put(kitDataMap.getKey(), kitDataMap.getValue().toBson());
        }

        Document temporaryDocument = new Document()
                .append("globalElo", kitData.values().stream().mapToInt(ProfileKitData::getElo).sum()
                        / (kitData.size() == 0 ? 1 : kitData.size()));

        Document document = new Document()
                .append("uuid", uniqueId.toString())
                .append("username", username)
                .append("lowerCaseUsername", username.toLowerCase())
                .append("settings", settingsDocument)
                .append("kitData", kitDataDocument)
                .append("eventWins", eventWins)
                .append("dailyEventWins", dailyEventWins)
                .append("weeklyEventWins", weeklyEventWins)
                .append("monthlyEventWins", monthlyEventWins)
                .append("lastDailyEventReset", lastDailyEventReset)
                .append("lastWeeklyEventReset", lastWeeklyEventReset)
                .append("lastMonthlyEventReset", lastMonthlyEventReset)
                .append("eventsPlayed", eventsPlayed)

                .append("temporary", temporaryDocument);

        PlayerProfileDataSaveEvent event = new PlayerProfileDataSaveEvent(this, document);
        event.call();

        return document;
    }

    public Player getPlayer() {
        if (Util.isNPC(uniqueId)) {
            return Eden.INSTANCE.getHookManager().getCitizensHook().getNPCPlayer(uniqueId);
        }
        return Bukkit.getPlayer(uniqueId);
    }

    public void setPlayerState(PlayerState playerState) {
        this.playerState = playerState;

        if (getPlayer() != null) {
            VisibilityController.updateVisibility(getPlayer());
        }
    }

    public void setupItems() {
        Player player = getPlayer();
        if (player == null)
            return;

        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        if (playerState == PlayerState.IN_LOBBY) {

            if (Party.getByPlayer(player) != null) {
                EdenItems.giveItem(player, EdenItems.PARTY_PARTY_LIST);
                EdenItems.giveItem(player, EdenItems.PARTY_PARTY_FIGHT);
                EdenItems.giveItem(player, EdenItems.PARTY_OTHER_PARTIES);
                EdenItems.giveItem(player, EdenItems.PARTY_EDITOR);
                EdenItems.giveItem(player, EdenItems.PARTY_PARTY_LEAVE);
            } else {

                EdenItems.giveItem(player, EdenItems.LOBBY_UNRANKED_QUEUE);
                EdenItems.giveItem(player, EdenItems.LOBBY_RANKED_QUEUE);
                EdenItems.giveItem(player, EdenItems.LOBBY_LEADERBOARD);
                EdenItems.giveItem(player, EdenItems.LOBBY_SETTINGS);
                EdenItems.giveItem(player, EdenItems.LOBBY_EDITOR);

                PracticeEvent<?> ongoingEvent = Eden.INSTANCE.getEventManager().getOngoingEvent();

                if (ongoingEvent != null && ongoingEvent.getHost() != null
                        && ongoingEvent.getHost().getUniqueId().equals(player.getUniqueId())) {

                    if (ongoingEvent.getState() == rip.diamond.practice.events.EventState.WAITING) {
                        player.getInventory().setItem(0,
                                new ItemBuilder(org.bukkit.Material.EMERALD).name("&a&lStart Event").build());
                        player.getInventory().setItem(1, new ItemBuilder(org.bukkit.Material.REDSTONE_COMPARATOR)
                                .name("&e&lManage Event").build());
                    }
                }

                if (ongoingEvent == null) {
                    EdenItems.giveItem(player, EdenItems.LOBBY_CREATE_EVENT);
                } else {
                    EdenItems.giveItem(player, EdenItems.LOBBY_JOIN_EVENT);
                }

                if (ongoingEvent != null && player.hasPermission("eden.admin")) {
                    EdenItems.giveItem(player, EdenItems.HOST_MANAGE_EVENT);
                } else {

                    EdenItems.giveItem(player, EdenItems.LOBBY_PARTY_OPEN);
                }
            }
        } else if (playerState == PlayerState.IN_QUEUE) {
            EdenItems.giveItem(player, EdenItems.QUEUE_LEAVE_QUEUE);
        } else if (playerState == PlayerState.IN_MATCH && match != null
                && !match.getTeamPlayer(getPlayer()).isAlive()) {
            EdenItems.giveItem(player, EdenItems.SPECTATE_TELEPORTER);
        } else if (playerState == PlayerState.IN_SPECTATING && match != null) {
            EdenItems.giveItem(player, EdenItems.SPECTATE_TELEPORTER);
            EdenItems.giveItem(player, EdenItems.SPECTATE_LEAVE_SPECTATE);
            EdenItems.giveItem(player,
                    settings.get(ProfileSettings.SPECTATOR_VISIBILITY).isEnabled()
                            ? EdenItems.SPECTATE_TOGGLE_VISIBILITY_OFF
                            : EdenItems.SPECTATE_TOGGLE_VISIBILITY_ON);
        } else if (playerState == PlayerState.IN_EVENT) {
            EdenItems.giveItem(player, EdenItems.EVENT_LEAVE);

            PracticeEvent<?> event = Eden.INSTANCE.getEventManager().getEventPlaying(player);
            if (event != null && event.getSpectators().contains(player)) {
                EdenItems.giveItem(player,
                        settings.get(ProfileSettings.SPECTATOR_VISIBILITY).isEnabled()
                                ? EdenItems.SPECTATE_TOGGLE_VISIBILITY_OFF
                                : EdenItems.SPECTATE_TOGGLE_VISIBILITY_ON);
            }

            if (Eden.INSTANCE.getEventManager()
                    .getOngoingEvent() instanceof rip.diamond.practice.events.games.parkour.ParkourEvent) {
                EdenItems.giveItem(player, EdenItems.PARKOUR_HIDE_PLAYERS);
                EdenItems.giveItem(player, EdenItems.PARKOUR_CHECKPOINT);
                EdenItems.giveItem(player, EdenItems.PARKOUR_RESET);
            } else if (Eden.INSTANCE.getEventManager()
                    .getOngoingEvent() instanceof rip.diamond.practice.events.games.dropper.DropperEvent) {
                EdenItems.giveItem(player, EdenItems.PARKOUR_HIDE_PLAYERS);
                EdenItems.giveItem(player, EdenItems.PARKOUR_RESET);
            } else if (Eden.INSTANCE.getEventManager()
                    .getOngoingEvent() instanceof rip.diamond.practice.events.games.thimble.ThimbleEvent) {
                EdenItems.giveItem(player, EdenItems.PARKOUR_HIDE_PLAYERS);
            }
        }

        player.updateInventory();
    }

    public void giveItems() {
        setupItems();
    }

    public void loadDefault() {

        Kit.getKits().forEach(kit -> kitData.putIfAbsent(kit.getName(), new ProfileKitData()));

        for (CooldownType type : CooldownType.values()) {
            cooldowns.put(type, new Cooldown(0));
        }
    }

    public void loadDefaultAfter() {

        Arrays.asList(ProfileSettings.values())
                .forEach(profileSettings -> settings.putIfAbsent(profileSettings, profileSettings.getDefault()));
    }

    public void load(Consumer<Boolean> callback) {
        if (playerState != PlayerState.LOADING) {
            return;
        }

        Eden.INSTANCE.getDatabaseManager().getHandler().loadProfile(uniqueId, (document) -> {
            load(document, callback);
        });
    }

    public void load(Document document, Consumer<Boolean> callback) {
        if (playerState != PlayerState.LOADING) {
            return;
        }
        Tasks.runAsync(() -> {
            try {
                loadDefault();

                if (document != null) {
                    fromBson(document);
                }

                loadDefaultAfter();

                callback.accept(true);
            } catch (Exception e) {
                e.printStackTrace();
                callback.accept(false);
            }
        });
    }

    public void save(boolean async, Consumer<Boolean> callback) {
        try {
            saving = true;
            if (playerState != PlayerState.LOADING) {
                Eden.INSTANCE.getDatabaseManager().getHandler().saveProfile(this, async);
            }
            callback.accept(true);
            saving = false;
        } catch (Exception e) {
            e.printStackTrace();
            callback.accept(false);
        }
    }

    public static PlayerProfile get(Player player) {
        return get(player.getUniqueId());
    }

    public static PlayerProfile get(String username) {
        return profiles.values().stream().filter(profile -> profile.getUsername().equalsIgnoreCase(username)).findAny()
                .orElse(null);
    }

    public static PlayerProfile get(UUID uuid) {
        return profiles.get(uuid);
    }

    public static PlayerProfile createPlayerProfile(Player player) {
        PlayerProfile profile = new PlayerProfile(player.getUniqueId(), player.getName());
        profiles.put(player.getUniqueId(), profile);
        return profile;
    }

    public static PlayerProfile createPlayerProfile(UUID uuid, String username) {
        PlayerProfile profile = new PlayerProfile(uuid, username);
        profiles.put(uuid, profile);
        return profile;
    }

    public void incrementEventWins() {
        checkAndResetEventTimePeriods();
        eventWins++;
        dailyEventWins++;
        weeklyEventWins++;
        monthlyEventWins++;
    }

    private void checkAndResetEventTimePeriods() {
        long now = System.currentTimeMillis();

        if (!isSameDay(lastDailyEventReset, now)) {
            dailyEventWins = 0;
            lastDailyEventReset = now;
        }

        if (!isSameWeek(lastWeeklyEventReset, now)) {
            weeklyEventWins = 0;
            lastWeeklyEventReset = now;
        }

        if (!isSameMonth(lastMonthlyEventReset, now)) {
            monthlyEventWins = 0;
            lastMonthlyEventReset = now;
        }
    }

    private boolean isSameDay(long time1, long time2) {
        java.util.Calendar cal1 = java.util.Calendar.getInstance();
        cal1.setTimeInMillis(time1);
        java.util.Calendar cal2 = java.util.Calendar.getInstance();
        cal2.setTimeInMillis(time2);
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
                cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR);
    }

    private boolean isSameWeek(long time1, long time2) {
        java.util.Calendar cal1 = java.util.Calendar.getInstance();
        cal1.setTimeInMillis(time1);
        java.util.Calendar cal2 = java.util.Calendar.getInstance();
        cal2.setTimeInMillis(time2);
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
                cal1.get(java.util.Calendar.WEEK_OF_YEAR) == cal2.get(java.util.Calendar.WEEK_OF_YEAR);
    }

    private boolean isSameMonth(long time1, long time2) {
        java.util.Calendar cal1 = java.util.Calendar.getInstance();
        cal1.setTimeInMillis(time1);
        java.util.Calendar cal2 = java.util.Calendar.getInstance();
        cal2.setTimeInMillis(time2);
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
                cal1.get(java.util.Calendar.MONTH) == cal2.get(java.util.Calendar.MONTH);
    }

}
