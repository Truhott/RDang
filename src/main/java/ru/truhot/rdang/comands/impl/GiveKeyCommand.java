package ru.truhot.rdang.comands.impl;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.util.MessageUtil;

public class GiveKeyCommand implements CommandExecutor {
    private final ConfigManager configManager;

    public GiveKeyCommand(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageUtil.colorize(getMessage("givekey.usage")));
            return true;
        }

        if (args.length > 3) {
            sender.sendMessage(MessageUtil.colorize(getMessage("givekey.usage")));
            return true;
        }

        String targetName = args[1];
        if (targetName == null || targetName.trim().isEmpty()) {
            sender.sendMessage(MessageUtil.colorize(getMessage("givekey.nickplayer")));
            return true;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage(MessageUtil.colorize(getMessage("givekey.noplayer")));
            return true;
        }

        ItemStack key = configManager.getItemManager().getKey();
        if (key == null) {
            sender.sendMessage(MessageUtil.colorize(getMessage("givekey.keynull")));
            return true;
        }

        int amount = 1;
        if (args.length == 3) {
            try {
                amount = Integer.parseInt(args[2]);
                if (amount < 1) {
                    sender.sendMessage(MessageUtil.colorize(getMessage("givekey.minamount")));
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(MessageUtil.colorize(getMessage("givekey.invalidamount")));
                return true;
            }
        }

        int maxStackSize = key.getMaxStackSize();
        int given = 0;

        while (given < amount) {
            int stackAmount = Math.min(maxStackSize, amount - given);
            ItemStack stack = key.clone();
            stack.setAmount(stackAmount);

            if (target.getInventory().addItem(stack).isEmpty()) {
                given += stackAmount;
            } else {
                target.getWorld().dropItemNaturally(target.getLocation(), stack);
                given += stackAmount;
            }
        }

        String giveMsg = getMessage("givekey.give")
                .replace("{key}", String.valueOf(given))
                .replace("{player}", target.getName());
        sender.sendMessage(MessageUtil.colorize(giveMsg));
        return true;
    }

    private String getMessage(String path) {
        return configManager.getMessages().getString("messages." + path, "&cСообщение не найдено: " + path);
    }
}