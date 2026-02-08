package ru.truhot.rdang.сore.managers;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import ru.truhot.rdang.util.HeadUtil;
import ru.truhot.rdang.util.MessageUtil;
import ru.truhot.rdang.util.TimeUtil;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class ItemManager {

    private ItemStack key;
    private ItemStack compass;
    private int spawnChance;
    private int saveChance;
    private boolean hideEnchantments;
    private long compassCooldown;
    private String compassSound;

    public void load(ConfigurationSection section) {
        if (section == null) return;

        ConfigurationSection keySection = section.getConfigurationSection("key");
        if (keySection != null) {
            this.key = loadItemFromConfig(keySection, "key");
            if (!keySection.contains("chanceSpawn")) System.out.println("[Error] нету chanceSpawn в секции key");
            this.spawnChance = keySection.getInt("chanceSpawn");
            if (!keySection.contains("saveChance")) System.out.println("[Error] нету saveChance в секции key");
            this.saveChance = keySection.getInt("saveChance");
        }

        ConfigurationSection compassSection = section.getConfigurationSection("compass");
        if (compassSection != null) {
            this.compass = loadItemFromConfig(compassSection, "compass");
            this.compassSound = compassSection.getString("sounds");
            if (this.compassSound == null) {
                System.out.println("[Error] нету sounds в секции compass");
            } else {
                try {
                    Sound.valueOf(this.compassSound.toUpperCase());
                } catch (IllegalArgumentException e) {
                    System.out.println("[Error] Неверный звук '" + this.compassSound + "' в секции compass");
                }
            }

            String cooldownStr = compassSection.getString("сooldown");
            if (cooldownStr == null) {
                System.out.println("[Error] нету сooldown в секции compass");
            } else {
                this.compassCooldown = TimeUtil.parseTimeString(cooldownStr);
            }
        }
    }

    public Sound getCompassSoundEnum() {
        if (this.compassSound == null) return null;
        try {
            return Sound.valueOf(this.compassSound.toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }

    private ItemStack loadItemFromConfig(ConfigurationSection section, String type) {
        String materialName = section.getString("material");
        ItemStack item = null;
        if (materialName == null) {
            System.out.println("[Error] нету material в секции " + section.getName());
        } else if (HeadUtil.isBase64Head(materialName)) {
            item = HeadUtil.createSkullFromPrefixedString(materialName, section.getName());
        } else {
            Material material = getMaterialFromString(materialName);
            if (material == null) {
                System.out.println("[Error] Неверный material '" + materialName + "' в секции " + section.getName());
            } else {
                item = new ItemStack(material);
            }
        }

        ItemMeta meta = (item != null) ? item.getItemMeta() : null;
        applyItemMeta(meta, section);
        applyNBT(meta, type);
        applyEnchants(meta, section);
        if (item != null && meta != null) {
            item.setItemMeta(meta);
        }
        return item;
    }

    private void applyNBT(ItemMeta meta, String type) {
        if (meta == null) return;
        if (type.equals("key")) {
            meta.getPersistentDataContainer().set(new NamespacedKey("rdang", "key"), PersistentDataType.STRING, "holyworld");
        } else if (type.equals("compass")) {
            meta.getPersistentDataContainer().set(new NamespacedKey("rdang", "compass"), PersistentDataType.STRING, "true");
        }
    }

    private void applyEnchants(ItemMeta meta, ConfigurationSection section) {
        if (!section.contains("hideEnchantments")) {
            System.out.println("[Error] нету hideEnchantments в секции " + section.getName());
        }
        if (meta == null) return;
        if (!section.getBoolean("hideEnchantments")) {
            meta.addEnchant(Enchantment.LUCK, 1, true);
        } else {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
    }

    private void applyItemMeta(ItemMeta meta, ConfigurationSection section) {
        String name = section.getString("name");
        if (name == null) {
            System.out.println("[Error] нету name в секции " + section.getName());
        } else if (meta != null) {
            meta.setDisplayName(MessageUtil.colorize(name));
        }

        List<String> lore = section.getStringList("lore");
        if (lore.isEmpty()) {
            System.out.println("[Error] нету lore в секции " + section.getName());
        } else if (meta != null) {
            meta.setLore(lore.stream()
                    .map(MessageUtil::colorize)
                    .collect(Collectors.toList()));
        }
    }

    private Material getMaterialFromString(String materialName) {
        if (materialName == null || materialName.isEmpty()) return null;
        Material material = Material.getMaterial(materialName.toUpperCase());
        if (material != null) return material;
        return Material.getMaterial(materialName.toUpperCase().replace(' ', '_'));
    }
}