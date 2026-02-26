package ru.truhot.rdang.util;

import org.bukkit.ChatColor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtil {
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();

    public static String colorize(String message) {
        if (message == null || message.isEmpty()) return "";
        return CACHE.computeIfAbsent(message, msg -> {
            Matcher matcher = HEX_PATTERN.matcher(msg);
            StringBuffer buffer = new StringBuffer();
            while (matcher.find()) {
                String hex = matcher.group(1);
                StringBuilder replacement = new StringBuilder("§x");
                for (char c : hex.toCharArray()) replacement.append('§').append(c);
                matcher.appendReplacement(buffer, replacement.toString());
            }
            matcher.appendTail(buffer);
            return ChatColor.translateAlternateColorCodes('&', buffer.toString());
        });
    }

    public static String getFormatted(String raw, Object... args) {
        if (raw == null) return "";
        String template = raw.contains("{") ? raw.replaceAll("\\{[^}]+\\}", "%s") : raw;
        return args.length == 0 ? colorize(template) : String.format(colorize(template), args);
    }

    public static List<String> colorize(List<String> messages) {
        if (messages == null) return Collections.emptyList();
        List<String> result = new ArrayList<>(messages.size());
        for (String m : messages) result.add(colorize(m));
        return result;
    }

    public static void clearCache() {
        CACHE.clear();
    }
}