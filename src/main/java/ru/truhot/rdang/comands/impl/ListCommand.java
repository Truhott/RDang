package ru.truhot.rdang.comands.impl;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.menu.MenuManager;
import ru.truhot.rdang.util.MessageUtil;

public class ListCommand implements CommandExecutor {

    private final ConfigManager configManager;
    private final MenuManager menuManager;

    public ListCommand(ConfigManager configManager, MenuManager menuManager) {
        this.configManager = configManager;
        this.menuManager = menuManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtil.colorize(getMessage("only-player")));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("rdang.admin")) {
            player.sendMessage(MessageUtil.colorize(getMessage("no-permission")));
            return true;
        }

        if (args.length > 1) {
            player.sendMessage(MessageUtil.colorize(getMessage("list.usage")));
            return true;
        }

        menuManager.openDungeonList(player, 0);
        return true;
    }

    private String getMessage(String path) {
        return configManager.getMessages().getString("messages." + path, "&cСообщение не найдено: " + path);
    }
}