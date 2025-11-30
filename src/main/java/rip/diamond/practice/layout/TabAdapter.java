package rip.diamond.practice.layout;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import rip.diamond.practice.Eden;
import rip.diamond.practice.config.Config;
import rip.diamond.practice.util.CC;
import rip.diamond.practice.util.tablist.ImanityTabAdapter;
import rip.diamond.practice.util.tablist.util.BufferedTabObject;
import rip.diamond.practice.util.tablist.util.Skin;
import rip.diamond.practice.util.tablist.util.TabColumn;
import rip.diamond.practice.util.tablist.util.TablistUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TabAdapter implements ImanityTabAdapter {

    @Override
    public Set<BufferedTabObject> getSlots(Player player) {
        Set<BufferedTabObject> objects = new HashSet<>();

        int i = 0;
        int maxSlots = TablistUtil.getPossibleSlots(player);
        List<Player> playerList = new ArrayList<Player>(Bukkit.getOnlinePlayers()).subList(0,
                Math.min(Bukkit.getOnlinePlayers().size(), maxSlots));

        for (Player target : playerList) {
            int x = i / (maxSlots / 20) + 1; 
                                             
            int y = i % (maxSlots / 20);

            
            String format = Config.FANCY_TABLIST_FORMAT.toString().replace("{player-name}", target.getName());
            
            format = Eden.INSTANCE.getPlaceholder().translate(target, format);
            
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                format = PlaceholderAPI.setPlaceholders(target, format);
            }
            
            String text = CC.translate(format);

            objects.add(new BufferedTabObject()
                    .slot(x)
                    .column(TabColumn.getFromOrdinal(y))
                    .text(text)
                    .ping(target.spigot().getPing())
                    .skin(Skin.fromPlayer(target)));

            i++;
        }

        return objects;
    }

    @Override
    public String getHeader(Player player) {
        List<String> headerLines = Eden.INSTANCE.getLanguageFile().getConfiguration().getStringList("tablist.header");

        
        if (headerLines == null || headerLines.isEmpty()) {
            return null;
        }

        
        StringBuilder header = new StringBuilder();
        for (int i = 0; i < headerLines.size(); i++) {
            String line = headerLines.get(i);
            
            line = Eden.INSTANCE.getPlaceholder().translate(player, line);
            
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                line = PlaceholderAPI.setPlaceholders(player, line);
            }
            
            header.append(line);
            if (i < headerLines.size() - 1) {
                header.append("\n");
            }
        }

        return header.toString();
    }

    @Override
    public String getFooter(Player player) {
        List<String> footerLines = Eden.INSTANCE.getLanguageFile().getConfiguration().getStringList("tablist.footer");

        
        if (footerLines == null || footerLines.isEmpty()) {
            return null;
        }

        
        StringBuilder footer = new StringBuilder();
        for (int i = 0; i < footerLines.size(); i++) {
            String line = footerLines.get(i);
            
            line = Eden.INSTANCE.getPlaceholder().translate(player, line);
            
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                line = PlaceholderAPI.setPlaceholders(player, line);
            }
            
            footer.append(line);
            if (i < footerLines.size() - 1) {
                footer.append("\n");
            }
        }

        return footer.toString();
    }
}
