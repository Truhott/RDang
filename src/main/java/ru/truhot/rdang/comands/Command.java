package ru.truhot.rdang.comands;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import ru.truhot.rdang.RDang;
import ru.truhot.rdang.comands.impl.*;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.dung.DungActions;
import ru.truhot.rdang.menu.MenuManager;
import ru.truhot.rdang.storage.Storage;
import ru.truhot.rdang.util.MessageUtil;
import ru.truhot.rdang.util.UndoUtil;
import ru.truhot.rdang.сore.MainCore;

public class Command implements CommandExecutor {
    private final RDang plugin;
    private final ConfigManager configManager;
    private final AddItemCommand addItemCommand;
    private final SpawnCommand spawnCommand;
    private final GiveKeyCommand giveKeyCommand;
    private final ReloadCommand reloadCommand;
    private final GiveCompassCommand giveCompassCommand;
    private final SchemCommand schemCommand;
    private final UndoCommand undoCommand;
    private final ListCommand listCommand;
    private final UndoUtil undoUtil;

    public Command(MainCore mainCore, DungActions dungActions, RDang plugin,
                   Storage items, Storage shulkers, Storage block,
                   ConfigManager configManager, MenuManager menuManager, UndoUtil undoUtil) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.undoUtil = undoUtil;
        this.addItemCommand = new AddItemCommand(mainCore, configManager);
        this.spawnCommand = new SpawnCommand(dungActions, configManager);
        this.giveKeyCommand = new GiveKeyCommand(configManager);
        this.reloadCommand = new ReloadCommand(configManager, items, shulkers);
        this.giveCompassCommand = new GiveCompassCommand(configManager);
        this.schemCommand = new SchemCommand(dungActions, plugin, configManager, shulkers, undoUtil);
        this.undoCommand = new UndoCommand(configManager, shulkers, block, plugin);
        this.listCommand = new ListCommand(configManager, menuManager);
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command,
                             String label, String[] args) {
        if (!sender.hasPermission("rdang.admin")) {
            sender.sendMessage(MessageUtil.colorize(getMessage("no-permission")));
            return true;
        }
        if (args.length == 0) {
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "additem":
                return addItemCommand.onCommand(sender, command, label, args);
            case "spawn":
                return spawnCommand.onCommand(sender, command, label, args);
            case "schem":
                return schemCommand.onCommand(sender, command, label, args);
            case "givekey":
                return giveKeyCommand.onCommand(sender, command, label, args);
            case "compass":
                return giveCompassCommand.onCommand(sender, command, label, args);
            case "reload":
                return reloadCommand.onCommand(sender, command, label, args);
            case "undo":
                return undoCommand.onCommand(sender, command, label, args);
            case "list":
                return listCommand.onCommand(sender, command, label, args);
            default:
                String unknownMsg = getMessage("unknown-command").replace("{command}", subCommand);
                sender.sendMessage(MessageUtil.colorize(unknownMsg));
                return true;
        }
    }

    private String getMessage(String path) {
        return configManager.getMessages().getString("messages." + path, "&cСообщение не найдено: " + path);
    }
}