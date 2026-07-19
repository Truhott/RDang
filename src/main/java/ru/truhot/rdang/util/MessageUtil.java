package ru.truhot.rdang.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MessageUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private MessageUtil() {
    }

    public static @NotNull Component parseText(String text) {
        if (text == null || text.isEmpty()) return Component.empty();

        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "<#" + matcher.group(1) + ">");
        }
        matcher.appendTail(sb);

        return MINI_MESSAGE.deserialize(convertLegacyToMiniMessage(sb.toString()));
    }

    public static @NotNull String colorize(String message) {
        if (message == null || message.isEmpty()) return "";
        return LEGACY.serialize(parseText(message));
    }

    public static @NotNull String getFormatted(String raw, @NotNull Object... args) {
        if (raw == null) return "";
        String template = raw.contains("{") ? raw.replaceAll("\\{[^}]+\\}", "%s") : raw;
        return args.length == 0 ? colorize(template) : String.format(colorize(template), args);
    }

    public static @NotNull List<String> colorize(List<String> messages) {
        if (messages == null) return Collections.emptyList();
        List<String> result = new ArrayList<>(messages.size());
        for (String m : messages) result.add(colorize(m));
        return result;
    }

    private static @NotNull String convertLegacyToMiniMessage(@NotNull String text) {
        if (!text.contains("&") && !text.contains("§")) return text;

        text = text.replace('§', '&');
        return text
                .replace("&0", "<black>")
                .replace("&1", "<dark_blue>")
                .replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>")
                .replace("&4", "<dark_red>")
                .replace("&5", "<dark_purple>")
                .replace("&6", "<gold>")
                .replace("&7", "<gray>")
                .replace("&8", "<dark_gray>")
                .replace("&9", "<blue>")
                .replace("&a", "<green>").replace("&A", "<green>")
                .replace("&b", "<aqua>").replace("&B", "<aqua>")
                .replace("&c", "<red>").replace("&C", "<red>")
                .replace("&d", "<light_purple>").replace("&D", "<light_purple>")
                .replace("&e", "<yellow>").replace("&E", "<yellow>")
                .replace("&f", "<white>").replace("&F", "<white>")
                .replace("&k", "<obfuscated>").replace("&K", "<obfuscated>")
                .replace("&l", "<bold>").replace("&L", "<bold>")
                .replace("&m", "<strikethrough>").replace("&M", "<strikethrough>")
                .replace("&n", "<underlined>").replace("&N", "<underlined>")
                .replace("&o", "<italic>").replace("&O", "<italic>")
                .replace("&r", "<reset>").replace("&R", "<reset>");
    }
}
