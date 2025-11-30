package rip.diamond.practice.util;

import org.bukkit.ChatColor;

import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("(?i)&#([0-9A-F]{6})");
    private static final Pattern GRADIENT_PATTERN = Pattern.compile("(?i)&<#([0-9A-F]{6}):#([0-9A-F]{6})>");

    
    private static final List<ColorSet> STANDARD_COLORS = Arrays.stream(ChatColor.values())
            .filter(ChatColor::isColor)
            .map(ColorSet::new)
            .collect(Collectors.toList());

    private ColorUtil() {}

    public static String colorize(String input) {
        if (input == null) return null;

        
        String withGradients = applyGradients(input);

        
        Matcher matcher = HEX_PATTERN.matcher(withGradients);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String hex = matcher.group(1);
            
            ChatColor nearest = getClosestColor(hex);
            matcher.appendReplacement(buffer, nearest.toString());
        }
        matcher.appendTail(buffer);

        
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    private static ChatColor getClosestColor(String hex) {
        Color color;
        try {
            color = Color.decode("#" + hex);
        } catch (NumberFormatException e) {
            return ChatColor.WHITE;
        }
        return getClosestColor(color);
    }

    private static ChatColor getClosestColor(Color color) {
        ColorSet nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (ColorSet colorSet : STANDARD_COLORS) {
            double distance = colorDistance(color, colorSet.color);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = colorSet;
            }
        }
        return nearest != null ? nearest.chatColor : ChatColor.WHITE;
    }

    private static double colorDistance(Color c1, Color c2) {
        int red1 = c1.getRed();
        int red2 = c2.getRed();
        int rmean = (red1 + red2) >> 1;
        int r = red1 - red2;
        int g = c1.getGreen() - c2.getGreen();
        int b = c1.getBlue() - c2.getBlue();
        return Math.sqrt((((512 + rmean) * r * r) >> 8) + 4 * g * g + (((767 - rmean) * b * b) >> 8));
    }

    private static String applyGradients(String input) {
        Matcher matcher = GRADIENT_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String startHex = matcher.group(1);
            String endHex = matcher.group(2);

            
            String remaining = input.substring(matcher.end());
            int stopIndex = findGradientStopIndex(remaining);
            String textToGradient = remaining.substring(0, stopIndex);

            
            String replacement = applyGradient(textToGradient, startHex, endHex);

            
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));

            
            
            
            
            
            
            
            

            
            
            
        }
        matcher.appendTail(sb);

        
        
        
        

        matcher.reset();
        StringBuffer simpleSb = new StringBuffer();
        while(matcher.find()) {
            String startHex = matcher.group(1);
            matcher.appendReplacement(simpleSb, getClosestColor(startHex).toString());
        }
        matcher.appendTail(simpleSb);
        return simpleSb.toString();
    }

    
    
    private static int findGradientStopIndex(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '&' && i + 1 < text.length()) {
                return i;
            }
        }
        return text.length();
    }

    private static String applyGradient(String text, String startHex, String endHex) {
        
        
        return getClosestColor(startHex) + text;
    }

    private static class ColorSet {
        ChatColor chatColor;
        Color color;

        public ColorSet(ChatColor chatColor) {
            this.chatColor = chatColor;
            
            switch (chatColor) {
                case BLACK: this.color = new Color(0, 0, 0); break;
                case DARK_BLUE: this.color = new Color(0, 0, 170); break;
                case DARK_GREEN: this.color = new Color(0, 170, 0); break;
                case DARK_AQUA: this.color = new Color(0, 170, 170); break;
                case DARK_RED: this.color = new Color(170, 0, 0); break;
                case DARK_PURPLE: this.color = new Color(170, 0, 170); break;
                case GOLD: this.color = new Color(255, 170, 0); break;
                case GRAY: this.color = new Color(170, 170, 170); break;
                case DARK_GRAY: this.color = new Color(85, 85, 85); break;
                case BLUE: this.color = new Color(85, 85, 255); break;
                case GREEN: this.color = new Color(85, 255, 85); break;
                case AQUA: this.color = new Color(85, 255, 255); break;
                case RED: this.color = new Color(255, 85, 85); break;
                case LIGHT_PURPLE: this.color = new Color(255, 85, 255); break;
                case YELLOW: this.color = new Color(255, 255, 85); break;
                case WHITE: this.color = new Color(255, 255, 255); break;
                default: this.color = new Color(255, 255, 255); break;
            }
        }
    }
}
