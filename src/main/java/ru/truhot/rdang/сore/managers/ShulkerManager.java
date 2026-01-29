package ru.truhot.rdang.сore.managers;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.ConfigurationSection;
import ru.truhot.rdang.storage.Storage;

import java.util.UUID;

public class ShulkerManager {
    private final Storage shulkers;
    private final LootManager lootManager;

    public ShulkerManager(Storage shulkers, LootManager lootManager) {
        this.shulkers = shulkers;
        this.lootManager = lootManager;
    }

    public void addShulker(Location location) {
        if (!(location.getBlock().getState() instanceof ShulkerBox))
            return;
        ShulkerBox shulkerBox = (ShulkerBox) location.getBlock().getState();
        lootManager.fillInventoryWithRandomLoot(shulkerBox.getInventory());
        String uuid = UUID.randomUUID().toString();
        addShulkerConfig(uuid, location, false);
    }

    public void addShulkerConfig(String id, Location location, boolean opened) {
        ConfigurationSection itemsSection = shulkers.getConfig().getConfigurationSection("locs");
        if (itemsSection == null) {
            shulkers.getConfig().createSection("locs");
            addShulkerConfig(id, location, opened);
        } else {
            itemsSection = itemsSection.createSection(String.valueOf(id));
            itemsSection.set("location", location);
            itemsSection.set("opened", opened);
            shulkers.save();
        }
    }

    public boolean isShulker(Block placedBlock) {
        if (placedBlock.getType() == Material.SHULKER_BOX || placedBlock.getType() == Material.BLACK_SHULKER_BOX || placedBlock.getType() == Material.WHITE_SHULKER_BOX || placedBlock.getType() == Material.BLUE_SHULKER_BOX || placedBlock.getType() == Material.CYAN_SHULKER_BOX || placedBlock.getType() == Material.BROWN_SHULKER_BOX || placedBlock.getType() == Material.YELLOW_SHULKER_BOX || placedBlock.getType() == Material.GREEN_SHULKER_BOX || placedBlock.getType() == Material.LIME_SHULKER_BOX || placedBlock.getType() == Material.RED_SHULKER_BOX || placedBlock.getType() == Material.LIGHT_BLUE_SHULKER_BOX || placedBlock.getType() == Material.LIGHT_GRAY_SHULKER_BOX || placedBlock.getType() == Material.MAGENTA_SHULKER_BOX || placedBlock.getType() == Material.PINK_SHULKER_BOX || placedBlock.getType() == Material.PURPLE_SHULKER_BOX || placedBlock.getType() == Material.ORANGE_SHULKER_BOX || placedBlock.getType() == Material.GRAY_SHULKER_BOX)
            return true;
        return false;
    }
}