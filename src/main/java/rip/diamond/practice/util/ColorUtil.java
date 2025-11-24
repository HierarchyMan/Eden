package rip.diamond.practice.util;

import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("(?i)&#([0-9A-F]{6})");
    private static final Pattern GRADIENT_PATTERN = Pattern.compile("(?i)&<#([0-9A-F]{6}):#([0-9A-F]{6})>");

    private ColorUtil() {}

    /**
     * Translates legacy color codes using '&', hex colors in the form of &#RRGGBB,
     * and gradients in the form of &<#RRGGBB:#RRGGBB>text
     * into Spigot-compatible section sign sequences.
     *
     * @param input the raw string from config
     * @return colored string or null if input is null
     */
    public static String colorize(String input) {
        if (input == null) return null;

        // First, process gradients
        String withGradients = applyGradients(input);

        // Then translate legacy color codes
        String withLegacy = ChatColor.translateAlternateColorCodes('&', withGradients);

        // Finally, process remaining hex codes (&#RRGGBB)
        Matcher matcher = HEX_PATTERN.matcher(withLegacy);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append('§').append(c);
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    /**
     * Applies gradient coloring to text using the format &<#RRGGBB:#RRGGBB>text
     * Gradient stops when encountering actual color codes (&0-9a-f, &#RRGGBB) or another gradient tag
     */
    private static String applyGradients(String input) {
        Matcher matcher = GRADIENT_PATTERN.matcher(input);
        StringBuilder result = new StringBuilder();
        int lastEnd = 0;

        while (matcher.find()) {
            // Append text before this gradient
            result.append(input, lastEnd, matcher.start());

            String startHex = matcher.group(1);
            String endHex = matcher.group(2);

            // Find the text after the gradient tag
            int gradientStart = matcher.end();
            String remaining = input.substring(gradientStart);

            // Find where gradient should stop (at color code, new gradient, or end of string)
            int stopIndex = findGradientStopIndex(remaining);
            String textToGradient = remaining.substring(0, stopIndex);

            // Apply gradient to the text
            String gradientedText = applyGradient(textToGradient, startHex, endHex);
            result.append(gradientedText);

            // Update position
            lastEnd = gradientStart + stopIndex;
        }

        // Append remaining text
        result.append(input.substring(lastEnd));
        return result.toString();
    }

    /**
     * Finds where the gradient should stop applying
     * Stops at: actual color codes (&0-9a-f, &#RRGGBB), new gradient (&<), or end of string
     * Ignores formatting codes like &l, &n, &o, &m, &k, &r
     */
    private static int findGradientStopIndex(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            // Check if this is a color code (not formatting code)
            if (c == '&' && i + 1 < text.length()) {
                char next = text.charAt(i + 1);

                // Stop at new gradient tag (&<)
                if (next == '<') {
                    return i;
                }

                // Color codes: 0-9, a-f, A-F, #
                // Formatting codes to ignore: l, n, o, m, k, r (and their uppercase)
                if ((next >= '0' && next <= '9') ||
                    (next >= 'a' && next <= 'f') ||
                    (next >= 'A' && next <= 'F') ||
                    next == '#') {
                    return i;
                }
            }
        }
        return text.length();
    }

    /**
     * Applies a gradient from startHex to endHex across the given text
     * Formatting codes (&l, &n, etc.) are preserved and don't count toward gradient progression
     */
    private static String applyGradient(String text, String startHex, String endHex) {
        if (text.isEmpty()) return text;

        // Parse RGB values
        int startR = Integer.parseInt(startHex.substring(0, 2), 16);
        int startG = Integer.parseInt(startHex.substring(2, 4), 16);
        int startB = Integer.parseInt(startHex.substring(4, 6), 16);

        int endR = Integer.parseInt(endHex.substring(0, 2), 16);
        int endG = Integer.parseInt(endHex.substring(2, 4), 16);
        int endB = Integer.parseInt(endHex.substring(4, 6), 16);

        // First pass: count actual visible characters (excluding formatting codes)
        int visibleChars = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&' && i + 1 < text.length()) {
                char next = text.charAt(i + 1);
                // Skip formatting codes: l, n, o, m, k, r
                if (next == 'l' || next == 'L' || next == 'n' || next == 'N' ||
                    next == 'o' || next == 'O' || next == 'm' || next == 'M' ||
                    next == 'k' || next == 'K' || next == 'r' || next == 'R') {
                    i++; // Skip the next character too
                    continue;
                }
            }
            visibleChars++;
        }

        // Second pass: apply gradient to visible characters
        StringBuilder result = new StringBuilder();
        int currentVisibleIndex = 0;
        StringBuilder activeFormatting = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            // Check if this is a formatting code
            if (c == '&' && i + 1 < text.length()) {
                char next = text.charAt(i + 1);
                // Formatting codes: l, n, o, m, k, r
                if (next == 'l' || next == 'L' || next == 'n' || next == 'N' ||
                    next == 'o' || next == 'O' || next == 'm' || next == 'M' ||
                    next == 'k' || next == 'K' || next == 'r' || next == 'R') {
                    // Add to active formatting (will be applied after every color)
                    activeFormatting.append(c).append(next);
                    i++; // Skip next character
                    continue;
                }
            }

            // Calculate interpolation factor for this visible character
            double factor = visibleChars == 1 ? 0.0 : (double) currentVisibleIndex / (visibleChars - 1);

            // Interpolate RGB values
            int r = (int) (startR + (endR - startR) * factor);
            int g = (int) (startG + (endG - startG) * factor);
            int b = (int) (startB + (endB - startB) * factor);

            // Convert to hex and apply - color first, then active formatting, then character
            String hex = String.format("%02X%02X%02X", r, g, b);
            result.append("&#").append(hex);

            // Apply all active formatting codes after EVERY color
            // This is needed because color codes reset formatting in Minecraft
            if (activeFormatting.length() > 0) {
                result.append(activeFormatting);
            }

            result.append(c);

            currentVisibleIndex++;
        }

        return result.toString();
    }
}

