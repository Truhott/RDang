package ru.truhot.rdang.сore.managers;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import ru.truhot.rdang.util.MessageUtil;
import java.util.ArrayList;
import java.util.List;

@Getter
public class MessageManager {
    private List<String> openDungMessages;
    private String saveKeyMessage;
    private List<String> closedDungMessages;

    public void load(ConfigurationSection section) {
        openDungMessages = MessageUtil.colorize(section.getStringList("openDung"));
        if (openDungMessages == null || openDungMessages.isEmpty()) {
            System.out.println("[Rdang] В секции messages нет openDung или он пуст!");
            openDungMessages = new ArrayList<>();
        }

        saveKeyMessage = MessageUtil.colorize(section.getString("saveKey"));
        if (saveKeyMessage == null || saveKeyMessage.isEmpty()) {
            System.out.println("[Rdang] В секции messages нет saveKey или он пуст!");
        }

        closedDungMessages = MessageUtil.colorize(section.getStringList("closedDung"));
        if (closedDungMessages == null || closedDungMessages.isEmpty()) {
            System.out.println("[Rdang] В секции messages нет closedDung или он пуст!");
            closedDungMessages = new ArrayList<>();
        }
    }

    public List<String> getFormattedOpenDungMessages(String playerName) {
        if (openDungMessages == null) return new ArrayList<>();
        List<String> formatted = new ArrayList<>();
        for (String message : openDungMessages) {
            formatted.add(message.replace("{player}", playerName));
        }
        return formatted;
    }
}