package rip.diamond.practice.config;

import org.bukkit.configuration.file.YamlConfiguration;
import rip.diamond.practice.util.BasicConfigFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ConfigUtil {

    public static boolean addMissingKeys(BasicConfigFile configFile, Enum<?>[] values) {
        File file = configFile.getFile();
        List<String> lines;
        try {
            lines = new ArrayList<>(Files.readAllLines(file.toPath(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        boolean changed = false;
        YamlConfiguration config = configFile.getConfiguration();

        for (Enum<?> enumVal : values) {
            try {
                String path = (String) enumVal.getClass().getMethod("getPath").invoke(enumVal);
                Object defaultValue = enumVal.getClass().getMethod("getDefaultValue").invoke(enumVal);

                if (!config.contains(path)) {
                    addKeyToLines(lines, path, defaultValue);
                    changed = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (changed) {
            try {
                Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return changed;
    }

    private static void addKeyToLines(List<String> lines, String path, Object value) {
        String[] parts = path.split("\\.");
        int currentIndentation = 0;
        int searchStartIndex = 0;

        for (int i = 0; i < parts.length - 1; i++) {
            String key = parts[i];
            int lineIndex = findKeyLine(lines, key, currentIndentation, searchStartIndex);

            if (lineIndex == -1) {
                insertKeyHierarchy(lines, parts, i, value, currentIndentation, searchStartIndex);
                return;
            }

            searchStartIndex = lineIndex + 1;
            currentIndentation += 2;
        }

        String finalKey = parts[parts.length - 1];
        int insertIndex = findEndOfBlock(lines, searchStartIndex, currentIndentation);

        String indentStr = getIndentString(currentIndentation);
        List<String> formattedValueLines = formatValue(value, currentIndentation);

        if (formattedValueLines.size() == 1 && !formattedValueLines.get(0).trim().startsWith("-")) {
            lines.add(insertIndex, indentStr + finalKey + ": " + formattedValueLines.get(0).trim());
        } else {
            lines.add(insertIndex++, indentStr + finalKey + ":");
            for (String line : formattedValueLines) {
                lines.add(insertIndex++, line);
            }
        }
    }

    private static int findKeyLine(List<String> lines, String key, int indentation, int startIndex) {
        Pattern pattern = Pattern.compile("^\\s{" + indentation + "}" + Pattern.quote(key) + ":.*");
        for (int i = startIndex; i < lines.size(); i++) {
            String line = lines.get(i);
            if (isMeaningfulLine(line) && getIndentation(line) < indentation) {
                return -1;
            }
            if (pattern.matcher(line).matches()) {
                return i;
            }
        }
        return -1;
    }

    private static int findEndOfBlock(List<String> lines, int startIndex, int indentation) {
        for (int i = startIndex; i < lines.size(); i++) {
            String line = lines.get(i);
            if (isMeaningfulLine(line) && getIndentation(line) < indentation) {
                return i;
            }
        }
        return lines.size();
    }

    private static void insertKeyHierarchy(List<String> lines, String[] parts, int startIndex, Object value,
            int currentIndentation, int insertLineIndex) {
        int insertIndex = findEndOfBlock(lines, insertLineIndex, currentIndentation);

        for (int i = startIndex; i < parts.length; i++) {
            String key = parts[i];
            String indentStr = getIndentString(currentIndentation);

            if (i == parts.length - 1) {
                List<String> formattedValueLines = formatValue(value, currentIndentation);
                if (formattedValueLines.size() == 1 && !formattedValueLines.get(0).trim().startsWith("-")) {
                    lines.add(insertIndex++, indentStr + key + ": " + formattedValueLines.get(0).trim());
                } else {
                    lines.add(insertIndex++, indentStr + key + ":");
                    for (String line : formattedValueLines) {
                        lines.add(insertIndex++, line);
                    }
                }
            } else {
                lines.add(insertIndex++, indentStr + key + ":");
                currentIndentation += 2;
            }
        }
    }

    private static List<String> formatValue(Object value, int indentation) {
        YamlConfiguration tempConfig = new YamlConfiguration();
        tempConfig.set("temp", value);
        String saved = tempConfig.saveToString();

        List<String> result = new ArrayList<>();

        if (saved.startsWith("temp: ")) {
            String val = saved.substring(6).trim();
            result.add(val);
            return result;
        }

        String[] lines = saved.split("\n");

        // Skip the first line "temp:"
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            // Indent the line
            result.add(getIndentString(indentation) + line);
        }

        if (result.isEmpty()) {
            result.add(String.valueOf(value));
        }

        return result;
    }

    private static String getIndentString(int indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++)
            sb.append(" ");
        return sb.toString();
    }

    private static boolean isMeaningfulLine(String line) {
        String trimmed = line.trim();
        return !trimmed.isEmpty() && !trimmed.startsWith("#");
    }

    private static int getIndentation(String line) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ')
                count++;
            else
                break;
        }
        return count;
    }
}
