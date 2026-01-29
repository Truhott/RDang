package ru.truhot.rdang.util;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public static String colorize(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append('§').append(c);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);
        String result = buffer.toString();
        return ChatColor.translateAlternateColorCodes('&', result);
    }
    public static List<String> colorize(List<String> messages) {
        if (messages == null) return new ArrayList<>();
        List<String> colored = new ArrayList<>();
        for (String message : messages) {
            colored.add(colorize(message));
        }
        return colored;
    }
}