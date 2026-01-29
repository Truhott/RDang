package ru.truhot.rdang.comands.impl;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import ru.truhot.rdang.RDang;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.storage.Storage;
import ru.truhot.rdang.util.MessageUtil;
import ru.truhot.rdang.util.UndoUtil;

public class UndoCommand implements CommandExecutor {
    private final ConfigManager configManager;
    private final UndoUtil undoUtil;

    public UndoCommand(ConfigManager configManager, Storage shulkers, Storage blockStorage, RDang plugin) {
        this.configManager = configManager;
        this.undoUtil = new UndoUtil(configManager, shulkers, blockStorage, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageUtil.colorize(getMessage("undo.usage")));
            return true;
        }
        String dungeonId = args[1];
        String regionName = configManager.getRegion().getString("region.name_format", "dang_{id}")
                .replace("{id}", dungeonId);
        UndoUtil.UndoResult result = undoUtil.performUndo(regionName);
        if (!result.found) {
            sender.sendMessage(MessageUtil.colorize(getMessage("undo.region-not-found")
                    .replace("{id}", dungeonId)
                    .replace("{region}", regionName)));
            return true;
        }
        sender.sendMessage(MessageUtil.colorize(getMessage("undo.region-deleted")
                .replace("{id}", dungeonId)
                .replace("{region}", regionName)
                .replace("{world}", result.worldName)));
        sender.sendMessage(MessageUtil.colorize(getMessage("undo.shulkers-deleted")
                .replace("{shulker}", String.valueOf(result.shulkerCount))));
        return true;
    }

    private String getMessage(String path) {
        return configManager.getMessages().getString("messages." + path, "&cСообщение не найдено: " + path);
    }
}