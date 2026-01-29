package ru.truhot.rdang.comands.impl;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.util.MessageUtil;
import ru.truhot.rdang.сore.MainCore;

import java.util.UUID;

public class AddItemCommand implements CommandExecutor {
    private final MainCore mainCore;
    private final ConfigManager configManager;

    public AddItemCommand(MainCore mainCore, ConfigManager configManager) {
        this.mainCore = mainCore;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.colorize(getMessage("only-player")));
            return true;
        }

        if (args.length != 4) {
            player.sendMessage(MessageUtil.colorize(getMessage("additem.usage")));
            return true;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() == Material.AIR) {
            player.sendMessage(MessageUtil.colorize(getMessage("additem.empty-hand")));
            return true;
        }

        try {
            int chance = Integer.parseInt(args[1]);
            int min = Integer.parseInt(args[2]);
            int max = Integer.parseInt(args[3]);

            if (chance < 1 || chance > 100) {
                player.sendMessage(MessageUtil.colorize(getMessage("additem.chance-range")));
                return true;
            }
            if (min < 1 || max < 1) {
                player.sendMessage(MessageUtil.colorize(getMessage("additem.amount-positive")));
                return true;
            }
            if (min > max) {
                player.sendMessage(MessageUtil.colorize(getMessage("additem.min-max")));
                return true;
            }

            String uuid = UUID.randomUUID().toString();
            mainCore.addItem(uuid, hand, chance, min, max);

            player.sendMessage(MessageUtil.colorize(getMessage("additem.success")));
            return true;

        } catch (NumberFormatException e) {
            player.sendMessage(MessageUtil.colorize(getMessage("additem.invalid-numbers")));
            return true;
        }
    }

    private String getMessage(String path) {
        return configManager.getMessages().getString("messages." + path, "&cСообщение не найдено: " + path);
    }
}