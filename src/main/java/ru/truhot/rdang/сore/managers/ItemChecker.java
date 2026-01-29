package ru.truhot.rdang.сore.managers;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import ru.truhot.rdang.config.ConfigManager;

public class ItemChecker {
    private final ConfigManager configManager;

    public ItemChecker(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public boolean isValidKey(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        NamespacedKey nbtKey = new NamespacedKey("rdang", "key");
        if (meta.getPersistentDataContainer().has(nbtKey, PersistentDataType.STRING)) {
            String value = meta.getPersistentDataContainer().get(nbtKey, PersistentDataType.STRING);
            return "holyworld".equals(value);
        }
        ItemStack configKey = configManager.getItemManager().getKey();
        if (configKey != null) {
            return item.isSimilar(configKey);
        }
        return false;
    }

    public boolean isKeyItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        NamespacedKey correctKey = new NamespacedKey("rdang", "key");
        if (meta.getPersistentDataContainer().has(correctKey, PersistentDataType.STRING)) {
            return true;
        }
        ItemStack configKey = configManager.getItemManager().getKey();
        return item.isSimilar(configKey);
    }

    public boolean isCompassItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        NamespacedKey compassKey = new NamespacedKey("rdang", "compass");
        return meta.getPersistentDataContainer().has(compassKey, PersistentDataType.STRING);
    }
}