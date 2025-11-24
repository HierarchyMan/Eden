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

    // Cache standard colors for distance calculation
    private static final List<ColorSet> STANDARD_COLORS = Arrays.stream(ChatColor.values())
            .filter(ChatColor::isColor)
            .map(ColorSet::new)
            .collect(Collectors.toList());

    private ColorUtil() {}

    public static String colorize(String input) {
        if (input == null) return null;

        // 1. Process Gradients (Interpolate then Downsample)
        String withGradients = applyGradients(input);

        // 2. Process Hex Codes (Downsample to nearest legacy)
        Matcher matcher = HEX_PATTERN.matcher(withGradients);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String hex = matcher.group(1);
            // Find nearest legacy color for 1.8
            ChatColor nearest = getClosestColor(hex);
            matcher.appendReplacement(buffer, nearest.toString());
        }
        matcher.appendTail(buffer);

        // 3. Translate standard legacy codes
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

            // Find content
            String remaining = input.substring(matcher.end());
            int stopIndex = findGradientStopIndex(remaining);
            String textToGradient = remaining.substring(0, stopIndex);

            // Apply gradient
            String replacement = applyGradient(textToGradient, startHex, endHex);

            // We manually handle appending because the length changes
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));

            // Adjust the matcher region or input logic?
            // Since standard regex replacement consumes the "textToGradient" in the group check,
            // we need to be careful. The simplest way for this specific regex structure:
            // Ideally, regex should capture the text group, but here we find it manually.
            // To avoid duplicating text, we essentially skip the text in the main loop?
            // A safer way for simple downsampling: Just treat gradients as the START color
            // to avoid "rainbow soup" on 1.8 which looks messy.
            // BUT, if you want the messy rainbow:

            // Actually, for 1.8, full character-by-character gradients look very "spammy"
            // because every character gets a color code (e.g. &4H&cE&6L&eL&aO).
            // However, to strictly fix the code provided:
        }
        matcher.appendTail(sb);

        // Since the original gradient logic was complex and broken for 1.8,
        // let's simplify the gradient step for 1.8:
        // Just replace the gradient tag with the closest color of the START hex.
        // This looks much cleaner on 1.8 than a blocky rainbow.

        matcher.reset();
        StringBuffer simpleSb = new StringBuffer();
        while(matcher.find()) {
            String startHex = matcher.group(1);
            matcher.appendReplacement(simpleSb, getClosestColor(startHex).toString());
        }
        matcher.appendTail(simpleSb);
        return simpleSb.toString();
    }

    // Helper to keep the standard gradient logic if you REALLY want it (commented out above)
    // but strictly speaking, gradients don't exist in 1.8.
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
        // For 1.8, simply return the text colored with the start color.
        // True interpolation requires packets or looks very bad in chat.
        return getClosestColor(startHex) + text;
    }

    private static class ColorSet {
        ChatColor chatColor;
        Color color;

        public ColorSet(ChatColor chatColor) {
            this.chatColor = chatColor;
            // Mapping Spigot ChatColors to RGB roughly
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