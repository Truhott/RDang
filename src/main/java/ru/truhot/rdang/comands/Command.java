package ru.truhot.rdang.comands;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import ru.truhot.rdang.RDang;
import ru.truhot.rdang.comands.impl.*;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.dung.DungActions;
import ru.truhot.rdang.menu.MenuManager;
import ru.truhot.rdang.storage.Storage;
import ru.truhot.rdang.permission.Permissions;
import ru.truhot.rdang.util.MessageUtil;
import ru.truhot.rdang.util.UndoUtil;
import ru.truhot.rdang.util.UpdateUtil;
import ru.truhot.rdang.сore.MainCore;
import ru.truhot.rdang.сore.managers.ShulkerManager;

import java.util.List;

public class Command implements CommandExecutor {
    private final RDang plugin;
    private final ConfigManager configManager;
    private final SpawnCommand spawnCommand;
    private final GiveCommand giveCommand;
    private final ReloadCommand reloadCommand;
    private final SchemCommand schemCommand;
    private final UndoCommand undoCommand;
    private final MenuCommand menuCommand;
    private final UndoUtil undoUtil;
    private final AdminsCommand adminsCommand;
    private final UpdateCommand updateCommand;
    private final MigrateCommand migrateCommand;

    public Command(MainCore mainCore, DungActions dungActions, RDang plugin,
                   Storage items, Storage shulkers,
                   ConfigManager configManager, MenuManager menuManager, UndoUtil undoUtil,
                   ShulkerManager shulkerManager, UpdateUtil updateUtil) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.undoUtil = undoUtil;
        this.adminsCommand = new AdminsCommand(shulkerManager, mainCore.getLootManager(), shulkers, configManager);
        this.spawnCommand = new SpawnCommand(dungActions, configManager);
        this.giveCommand = new GiveCommand(configManager);
        this.reloadCommand = new ReloadCommand(configManager, items, shulkers);
        this.schemCommand = new SchemCommand(dungActions, plugin, configManager, shulkers, undoUtil);
        this.undoCommand = new UndoCommand(configManager, undoUtil);
        this.menuCommand = new MenuCommand(configManager, menuManager);
        this.updateCommand = new UpdateCommand(plugin, configManager, updateUtil);
        this.migrateCommand = new MigrateCommand(plugin, configManager);
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command,
                             String label, String[] args) {
        if (!Permissions.has(sender, Permissions.USE)) {
            sender.sendMessage(MessageUtil.colorize(getMessage("no_permission")));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "spawn":
                return spawnCommand.onCommand(sender, command, label, args);
            case "schem":
                return schemCommand.onCommand(sender, command, label, args);
            case "give":
                return giveCommand.onCommand(sender, command, label, args);
            case "reload":
                return reloadCommand.onCommand(sender, command, label, args);
            case "undo":
                return undoCommand.onCommand(sender, command, label, args);
            case "menu":
                return menuCommand.onCommand(sender, command, label, args);
            case "admins":
                return adminsCommand.onCommand(sender, command, label, args);
            case "update":
                return updateCommand.onCommand(sender, command, label, args);
            case "migrate":
                return migrateCommand.onCommand(sender, command, label, args);
            default:
                String unknownMsg = getMessage("unknown_command").replace("{command}", subCommand);
                sender.sendMessage(MessageUtil.colorize(unknownMsg));
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        List<String> helpMessages = configManager.getMessages().getStringList("messages.help");
        for (String msg : helpMessages) {
            sender.sendMessage(MessageUtil.colorize(msg));
        }
    }

    private String getMessage(String path) {
        return configManager.getMessages().getString("messages." + path, "&cСообщение не найдено: " + path);
    }
}
