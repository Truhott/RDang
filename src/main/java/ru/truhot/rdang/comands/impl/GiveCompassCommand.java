package ru.truhot.rdang.comands.impl;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.util.MessageUtil;

public class GiveCompassCommand {
    private final ConfigManager configManager;

    public GiveCompassCommand(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            String helpMsg = configManager.getMessages().getString("givecompass.usage");
            if (helpMsg == null) return true;
            sender.sendMessage(MessageUtil.colorize(helpMsg));
            return true;
        }

        if (args[1].isEmpty()) {
            String nickMsg = configManager.getMessages().getString("givecompass.nickplayer");
            if (nickMsg == null) return true;
            sender.sendMessage(MessageUtil.colorize(nickMsg));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            String noPlayerMsg = configManager.getMessages().getString("givecompass.noplayer");
            if (noPlayerMsg == null) return true;
            sender.sendMessage(MessageUtil.colorize(noPlayerMsg));
            return true;
        }
        ItemStack compass = configManager.getItemManager().getCompass();
        if (compass == null) {
            String compassNullMsg = configManager.getMessages().getString("givecompass.compassnull");
            if (compassNullMsg == null) return true;
            sender.sendMessage(MessageUtil.colorize(compassNullMsg));
            return true;
        }

        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
                if (amount < 1) {
                    String minAmountMsg = configManager.getMessages().getString("givecompass.minamount");
                    if (minAmountMsg == null) return true;
                    sender.sendMessage(MessageUtil.colorize(minAmountMsg));
                    return true;
                }
            } catch (NumberFormatException e) {
                String invalidAmountMsg = configManager.getMessages().getString("givecompass.invalidamount");
                if (invalidAmountMsg == null) return true;
                sender.sendMessage(MessageUtil.colorize(invalidAmountMsg));
                return true;
            }
        }
        int maxStackSize = compass.getMaxStackSize();
        int given = 0;
        while (given < amount) {
            int stackAmount = Math.min(maxStackSize, amount - given);
            ItemStack stack = compass.clone();
            stack.setAmount(stackAmount);
            if (target.getInventory().addItem(stack).isEmpty()) {
                given += stackAmount;
            } else {
                target.getWorld().dropItemNaturally(target.getLocation(), stack);
                given += stackAmount;
            }
        }
        String giveMsg = configManager.getMessages().getString("givecompass.give");
        if (giveMsg == null) return true;
        giveMsg = giveMsg
                .replace("{amount}", String.valueOf(given))
                .replace("{player}", target.getName());

        if (sender != target) {
            sender.sendMessage(MessageUtil.colorize(giveMsg));
        }
        target.sendMessage(MessageUtil.colorize(giveMsg));

        return true;
    }
}