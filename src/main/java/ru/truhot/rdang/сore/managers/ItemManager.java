package ru.truhot.rdang.сore.managers;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import ru.truhot.rdang.util.HeadUtil;
import ru.truhot.rdang.util.MessageUtil;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class ItemManager {

    private ItemStack key;
    private ItemStack compass;
    private int spawnChance = 10;
    private int saveChance = 10;
    private boolean hideEnchantments = false;

    public void load(ConfigurationSection section) {
        if (section == null) {
            createDefaultKey();
            createDefaultCompass();
            return;
        }

        ConfigurationSection keySection = section.getConfigurationSection("key");
        if (keySection != null) {
            key = loadItemFromConfig(keySection, Material.PRISMARINE_SHARD,
                    "Ключ для хранилища");
            spawnChance = keySection.getInt("chanceSpawn", 10);
            saveChance = keySection.getInt("saveChance", 10);
            hideEnchantments = keySection.getBoolean("hideEnchantments", false);
        } else {
            createDefaultKey();
        }

        ConfigurationSection compassSection = section.getConfigurationSection("compass");
        if (compassSection != null) {
            compass = loadItemFromConfig(compassSection, Material.COMPASS,
                    "&bКомпас");
            if (compass != null) {
                ItemMeta meta = compass.getItemMeta();
                if (meta != null) {
                    NamespacedKey compassKey = new NamespacedKey("rdang", "compass");
                    meta.getPersistentDataContainer().set(compassKey,
                            PersistentDataType.STRING, "true");
                    compass.setItemMeta(meta);
                }
            }
        } else {
            createDefaultCompass();
        }
    }

    private ItemStack loadItemFromConfig(ConfigurationSection section,
                                         Material defaultMaterial,
                                         String defaultName) {
        String materialName = section.getString("material", defaultMaterial.name());
        if (HeadUtil.isBase64Head(materialName)) {
            return createSkullFromBase64(materialName, section, defaultName);
        }
        Material material = getMaterialFromString(materialName);
        if (material == null) material = defaultMaterial;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            applyItemMeta(meta, section, defaultName);
            if (defaultMaterial == Material.PRISMARINE_SHARD) {
                NamespacedKey nbtKey = new NamespacedKey("rdang", "key");
                meta.getPersistentDataContainer().set(nbtKey,
                        PersistentDataType.STRING, "holyworld");
            }
            if (defaultMaterial == Material.COMPASS) {
                NamespacedKey compassKey = new NamespacedKey("rdang", "compass");
                meta.getPersistentDataContainer().set(compassKey,
                        PersistentDataType.STRING, "true");
            }
            boolean itemHideEnchantments = section.getBoolean("hideEnchantments", hideEnchantments);
            if (!itemHideEnchantments) {
                meta.addEnchant(Enchantment.LUCK, 1, true);
            }
            if (itemHideEnchantments) {
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSkullFromBase64(String base64WithPrefix,
                                            ConfigurationSection section,
                                            String defaultName) {
        ItemStack skull = HeadUtil.createSkullFromPrefixedString(base64WithPrefix);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta == null) return skull;
        applyItemMeta(meta, section, defaultName);
        if (defaultName.equals("Ключ для хранилища")) {
            NamespacedKey nbtKey = new NamespacedKey("rdang", "key");
            meta.getPersistentDataContainer().set(nbtKey,
                    PersistentDataType.STRING, "holyworld");
        }
        if (defaultName.equals("&bКомпас")) {
            NamespacedKey compassKey = new NamespacedKey("rdang", "compass");
            meta.getPersistentDataContainer().set(compassKey,
                    PersistentDataType.STRING, "true");
        }
        boolean itemHideEnchantments = section.getBoolean("hideEnchantments", hideEnchantments);
        if (!itemHideEnchantments) {
            meta.addEnchant(Enchantment.LUCK, 1, true);
        }
        if (itemHideEnchantments) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        skull.setItemMeta(meta);
        return skull;
    }

    private void applyItemMeta(ItemMeta meta, ConfigurationSection section, String defaultName) {
        String name = section.getString("name", defaultName);
        meta.setDisplayName(MessageUtil.colorize(name));

        List<String> lore = section.getStringList("lore");
        if (lore.isEmpty()) {
            if (defaultName.equals("&bКомпас")) {
                lore = List.of("", "§fdbekasov11 го на рухайпиксиль?", "");
            } else {
                lore = List.of("", "§fdbekasov11 го на рухайпиксиль?", "");
            }
        }
        meta.setLore(lore.stream()
                .map(MessageUtil::colorize)
                .collect(Collectors.toList()));
    }

    private void createDefaultKey() {
        ItemStack item = new ItemStack(Material.PRISMARINE_SHARD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtil.colorize("Ключ для хранилища"));
            meta.setLore(MessageUtil.colorize(List.of("", "§fdbekasov11 го на рухайпиксиль?", "")));
            NamespacedKey nbtKey = new NamespacedKey("rdang", "key");
            meta.getPersistentDataContainer().set(nbtKey,
                    PersistentDataType.STRING, "holyworld");
            if (!hideEnchantments) {
                meta.addEnchant(Enchantment.LUCK, 1, true);
            }
            if (hideEnchantments) {
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
        }
        this.key = item;
        this.spawnChance = 10;
        this.saveChance = 10;
    }

    private void createDefaultCompass() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtil.colorize("&bКомпас"));
            meta.setLore(MessageUtil.colorize(List.of("", "§fdbekasov11 го на рухайпиксиль?", "")));
            NamespacedKey compassKey = new NamespacedKey("rdang", "compass");
            meta.getPersistentDataContainer().set(compassKey,
                    PersistentDataType.STRING, "true");
            if (!hideEnchantments) {
                meta.addEnchant(Enchantment.LUCK, 1, true);
            }

            if (hideEnchantments) {
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
        }
        this.compass = item;
    }

    private Material getMaterialFromString(String materialName) {
        if (materialName == null || materialName.isEmpty()) {
            return null;
        }

        Material material = Material.getMaterial(materialName.toUpperCase());
        if (material != null) {
            return material;
        }

        String formattedName = materialName.toUpperCase().replace(' ', '_');
        return Material.getMaterial(formattedName);
    }
}