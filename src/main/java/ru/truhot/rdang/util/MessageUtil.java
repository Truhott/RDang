package ru.truhot.rdang.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MessageUtil {

    private static final Pattern LEGACY_TOKEN =
            Pattern.compile("&(#([A-Fa-f0-9]{6})|[0-9a-fk-orA-FK-OR])");

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final LegacyComponentSerializer LEGACY_OUT =
            LegacyComponentSerializer.legacySection();

    private MessageUtil() {
    }

    public static @NotNull Component parseText(String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        String normalized = text.replace('§', '&');
        String miniMessageSource = convertLegacyToMiniMessage(normalized);
        return MINI_MESSAGE.deserialize(miniMessageSource);
    }

    public static @NotNull String colorize(String message) {
        if (message == null || message.isEmpty()) return "";
        return LEGACY_OUT.serialize(parseText(message));
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
        if (!text.contains("&")) return text;

        Matcher matcher = LEGACY_TOKEN.matcher(text);
        StringBuilder out = new StringBuilder();
        Deque<String> openFormats = new ArrayDeque<>();
        String[] currentColorTag = {null};
        int last = 0;

        while (matcher.find()) {
            out.append(text, last, matcher.start());
            last = matcher.end();

            String hex = matcher.group(2);
            char code = hex != null ? 0 : Character.toLowerCase(matcher.group(1).charAt(0));

            if (hex != null || isColorCode(code)) {
                closeFormats(out, openFormats);
                if (currentColorTag[0] != null) out.append("</").append(currentColorTag[0]).append(">");
                String tag = hex != null ? ("#" + hex) : colorName(code);
                out.append('<').append(tag).append('>');
                currentColorTag[0] = tag;
            } else if (code == 'r') {
                closeFormats(out, openFormats);
                if (currentColorTag[0] != null) {
                    out.append("</").append(currentColorTag[0]).append(">");
                    currentColorTag[0] = null;
                }
            } else {
                String tag = formatName(code);
                if (tag != null) {
                    out.append('<').append(tag).append('>');
                    openFormats.push(tag);
                }
            }
        }
        out.append(text.substring(last));

        closeFormats(out, openFormats);
        if (currentColorTag[0] != null) out.append("</").append(currentColorTag[0]).append(">");

        return out.toString();
    }

    private static void closeFormats(StringBuilder out, @NotNull Deque<String> openFormats) {
        while (!openFormats.isEmpty()) {
            out.append("</").append(openFormats.pop()).append(">");
        }
    }

    private static boolean isColorCode(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f');
    }

    private static @NotNull String colorName(char c) {
        return switch (c) {
            case '0' -> "black";
            case '1' -> "dark_blue";
            case '2' -> "dark_green";
            case '3' -> "dark_aqua";
            case '4' -> "dark_red";
            case '5' -> "dark_purple";
            case '6' -> "gold";
            case '7' -> "gray";
            case '8' -> "dark_gray";
            case '9' -> "blue";
            case 'a' -> "green";
            case 'b' -> "aqua";
            case 'c' -> "red";
            case 'd' -> "light_purple";
            case 'e' -> "yellow";
            case 'f' -> "white";
            default -> "white";
        };
    }

    @Contract(pure = true)
    private static @Nullable String formatName(char c) {
        return switch (c) {
            case 'k' -> "obfuscated";
            case 'l' -> "bold";
            case 'm' -> "strikethrough";
            case 'n' -> "underlined";
            case 'o' -> "italic";
            default -> null;
        };
    }
}
