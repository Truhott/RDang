package ru.truhot.rdang.сore.managers;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import ru.truhot.rdang.util.MessageUtil;

import java.util.ArrayList;
import java.util.List;

@Getter
public class MessageManager {

    private List<String> openDungMessages = new ArrayList<>();
    private String saveKeyMessage = "";
    private List<String> closedDungMessages = new ArrayList<>();

    public void load(ConfigurationSection section) {
        if (section == null) {
            setDefaultMessages();
            return;
        }
        openDungMessages = MessageUtil.colorize(section.getStringList("openDung"));
        saveKeyMessage = MessageUtil.colorize(section.getString("saveKey", "&aТы сохранил ключ"));
        closedDungMessages = MessageUtil.colorize(section.getStringList("closedDung"));
        if (openDungMessages.isEmpty()) {
            openDungMessages = MessageUtil.colorize(List.of("", "&eИгрок &6{player} &eоткрыл хранилище", ""));
        }
        if (closedDungMessages.isEmpty()) {
            closedDungMessages = MessageUtil.colorize(List.of("", "&cХранилище закрыто!!", ""));
        }
    }

    private void setDefaultMessages() {
        openDungMessages = MessageUtil.colorize(List.of("", "&eИгрок &6{player} &eоткрыл хранилище", ""));
        saveKeyMessage = MessageUtil.colorize("&aТы сохранил ключ");
        closedDungMessages = MessageUtil.colorize(List.of("", "&cХранилище закрыто!!", ""));
    }

    public List<String> getFormattedOpenDungMessages(String playerName) {
        List<String> formatted = new ArrayList<>();
        for (String message : openDungMessages) {
            formatted.add(message.replace("{player}", playerName));
        }
        return formatted;
    }
}