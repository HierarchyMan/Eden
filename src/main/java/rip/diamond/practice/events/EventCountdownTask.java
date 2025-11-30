package rip.diamond.practice.events;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import rip.diamond.practice.Eden;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.Clickable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Setter
@Getter
public abstract class EventCountdownTask extends BukkitRunnable {

    private final PracticeEvent<?> event;
    private final int countdownTime;

    private int timeUntilStart;
    private boolean ended;

    public EventCountdownTask(PracticeEvent<?> event, int countdownTime) {
        this.event = event;
        this.countdownTime = 300; 
        this.timeUntilStart = this.countdownTime;
    }

    @Override
    public void run() {
        if (isEnded()) {
            return;
        }

        
        if (canStart() && timeUntilStart > 60) {
            timeUntilStart = 60;
            
            
            
        }

        if (timeUntilStart <= 0) {
            if (canStart()) {
                event.start();
            } else {
                onCancel();
            }

            ended = true;
            return;
        }

        
        if (shouldAnnounce(timeUntilStart) || timeUntilStart <= 5) {

            boolean actuallyStarting = canStart();

            
            
            

            if (timeUntilStart > 5) {
                
                List<String> messages;
                String clickableText;

                if (actuallyStarting) {
                    messages = CC.translate(
                            Eden.INSTANCE.getLanguageFile().getConfiguration().getStringList("EVENT.ANNOUNCEMENT"));
                    clickableText = CC.translate(Eden.INSTANCE.getLanguageFile().getConfiguration()
                            .getString("EVENT.ANNOUNCEMENT-CLICKABLE"));
                } else {
                    messages = CC.translate(Eden.INSTANCE.getLanguageFile().getConfiguration()
                            .getStringList("EVENT.ANNOUNCEMENT_WAITING"));
                    if (messages.isEmpty()) {
                        
                        messages = Collections.singletonList(
                                "&e[Event] &a<eventName> &ehosted by &a<host> &eis waiting for players! &7(&a<players>&7/&a<maxPlayers>&7)");
                    }
                    clickableText = CC.translate(Eden.INSTANCE.getLanguageFile().getConfiguration()
                            .getString("EVENT.ANNOUNCEMENT-CLICKABLE"));
                }

                for (String message : messages) {
                    String finalMessage = message
                            .replace("<host>", event.getHost().getName())
                            .replace("<countdown>", actuallyStarting ? String.valueOf(timeUntilStart) : "")
                            .replace("<maxPlayers>", String.valueOf(event.getLimit()))
                            .replace("<players>", String.valueOf(event.getPlayers().size()))
                            .replace("<eventName>", event.getName());

                    Clickable clickable = new Clickable(finalMessage, clickableText, "/join_event " + event.getName());

                    Bukkit.getServer().getOnlinePlayers()
                            .stream()
                            .filter(eventPlayer -> !event.getPlayers().containsKey(eventPlayer.getUniqueId()))
                            .collect(Collectors.toList()).forEach(clickable::sendToPlayer);
                }
            }

            
            String plainMessage;
            if (actuallyStarting) {
                plainMessage = CC.translate(Eden.INSTANCE.getLanguageFile().getConfiguration()
                        .getString("EVENT.ANNOUNCEMENT-MESSAGE",
                                "&e[Event] &a<eventName> &ehosted by &a<host> &eis starting in &a<countdown> &eseconds! &7(&a<players>&7/&a<maxPlayers>&7)")
                        .replace("<host>", event.getHost().getName())
                        .replace("<countdown>", String.valueOf(timeUntilStart))
                        .replace("<maxPlayers>", String.valueOf(event.getLimit()))
                        .replace("<players>", String.valueOf(event.getPlayers().size()))
                        .replace("<eventName>", event.getName()));
            } else {
                plainMessage = CC.translate(Eden.INSTANCE.getLanguageFile().getConfiguration()
                        .getString("EVENT.ANNOUNCEMENT-MESSAGE-WAITING",
                                "&e[Event] &a<eventName> &ehosted by &a<host> &eis waiting for players! &7(&a<players>&7/&a<maxPlayers>&7)")
                        .replace("<host>", event.getHost().getName())
                        .replace("<countdown>", "") 
                        .replace("<maxPlayers>", String.valueOf(event.getLimit()))
                        .replace("<players>", String.valueOf(event.getPlayers().size()))
                        .replace("<eventName>", event.getName()));
            }

            for (java.util.UUID uuid : event.getPlayers().keySet()) {
                org.bukkit.entity.Player p = Bukkit.getPlayer(uuid);
                if (p != null)
                    p.sendMessage(plainMessage);
            }
        }

        timeUntilStart--;
    }

    public void cancelCountdown() {
        this.ended = true;
        try {
            this.cancel();
        } catch (IllegalStateException e) {
            
        }
    }

    public abstract boolean shouldAnnounce(int timeUntilStart);

    public abstract boolean canStart();

    public abstract void onCancel();
}
