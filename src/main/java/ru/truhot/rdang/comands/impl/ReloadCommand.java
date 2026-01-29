package ru.truhot.rdang.comands.impl;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.storage.Storage;
import ru.truhot.rdang.util.MessageUtil;

public class ReloadCommand implements CommandExecutor {
    private final ConfigManager configManager;
    private final Storage items;
    private final Storage shulkers;

    public ReloadCommand(ConfigManager configManager, Storage items, Storage shulkers) {
        this.configManager = configManager;
        this.items = items;
        this.shulkers = shulkers;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 1) {
            sender.sendMessage(MessageUtil.colorize(getMessage("reload.usage")));
            return true;
        }

        configManager.reloadAll();
        items.reloadConfig();
        shulkers.reloadConfig();

        sender.sendMessage(MessageUtil.colorize(getMessage("reload.success")));
        return true;
    }

    private String getMessage(String path) {
        return configManager.getMessages().getString("messages." + path, "&cСообщение не найдено: " + path);
    }
}