package ru.truhot.rdang.сore.managers;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.truhot.rdang.storage.Storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LootManager {
    private final Storage items;
    private final Random random = new Random();

    public LootManager(Storage items) {
        this.items = items;
    }

    public void fillInventoryWithRandomLoot(Inventory inventory) {
        ConfigurationSection itemsSection = items.getConfig().getConfigurationSection("items");
        if (itemsSection != null) {
            List<Integer> availableSlots = new ArrayList<>();
            for (int i = 0; i < inventory.getSize(); i++)
                availableSlots.add(i);
            for (String itemId : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemId);
                ItemStack item = itemSection.getItemStack("item");
                item.setAmount(getRandomNumber(itemSection.getInt("minAmount"), itemSection.getInt("maxAmount")));
                double chance = itemSection.getDouble("chance");
                if (Math.random() * 100.0D < chance && !availableSlots.isEmpty()) {
                    int randomSlotIndex = random.nextInt(availableSlots.size());
                    int slot = availableSlots.get(randomSlotIndex);
                    inventory.setItem(slot, item);
                    availableSlots.remove(randomSlotIndex);
                }
            }
        } else {
            items.getConfig().createSection("items");
        }
    }

    public void addItem(String id, ItemStack item, int chance, int minAmount, int maxAmount) {
        ConfigurationSection itemsSection = items.getConfig().getConfigurationSection("items");
        if (itemsSection == null) {
            items.getConfig().createSection("items");
            addItem(id, item, chance, minAmount, maxAmount);
        } else {
            itemsSection = itemsSection.createSection(String.valueOf(id));
            itemsSection.set("item", item);
            itemsSection.set("chance", chance);
            itemsSection.set("minAmount", minAmount);
            itemsSection.set("maxAmount", maxAmount);
            items.save();
        }
    }

    public int getRandomNumber(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);
    }
}