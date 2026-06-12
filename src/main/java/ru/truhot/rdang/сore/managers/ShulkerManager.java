package ru.truhot.rdang.сore.managers;

import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.ConfigurationSection;
import ru.truhot.rdang.storage.Storage;
import ru.truhot.rdang.util.logger.Logger;

import java.util.Collection;
import java.util.UUID;

@RequiredArgsConstructor
public class ShulkerManager {
    private final Storage shulkers;
    private final LootManager lootManager;

    public void addShulker(Location location) {
        addShulkers(java.util.Collections.singletonList(location));
    }

    public int addShulkers(Collection<Location> locations) {
        if (locations == null || locations.isEmpty()) {
            return 0;
        }
        ConfigurationSection locsSection = shulkers.getConfig().getConfigurationSection("locs");
        if (locsSection == null) {
            locsSection = shulkers.getConfig().createSection("locs");
        }
        int added = 0;
        for (Location location : locations) {
            if (location == null || location.getWorld() == null) {
                continue;
            }
            if (!(location.getBlock().getState() instanceof ShulkerBox shulkerBox)) {
                continue;
            }
            lootManager.fillRandomLoot(shulkerBox.getInventory());
            ConfigurationSection entry = locsSection.createSection(UUID.randomUUID().toString());
            entry.set("location", location);
            entry.set("opened", false);
            Logger.info("Добавление шалкера в " + location);
            added++;
        }
        if (added > 0) {
            shulkers.save();
        }
        return added;
    }

    public void addShulkerConfig(String id, Location location, boolean opened) {
        ConfigurationSection itemsSection = shulkers.getConfig().getConfigurationSection("locs");
        if (itemsSection == null) {
            shulkers.getConfig().createSection("locs");
            addShulkerConfig(id, location, opened);
            return;
        }
        ConfigurationSection entry = itemsSection.createSection(String.valueOf(id));
        entry.set("location", location);
        entry.set("opened", opened);
        shulkers.save();
    }

    public boolean isShulker(Block placedBlock) {
        return isShulkerMaterial(placedBlock.getType());
    }

    public static boolean isShulkerMaterial(Material material) {
        return Tag.SHULKER_BOXES.isTagged(material);
    }
}