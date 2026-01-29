package ru.truhot.rdang.сore;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.storage.Storage;
import ru.truhot.rdang.сore.managers.EventManager;
import ru.truhot.rdang.сore.managers.ItemChecker;
import ru.truhot.rdang.сore.managers.LootManager;
import ru.truhot.rdang.сore.managers.ShulkerManager;

@Getter
@RequiredArgsConstructor
public class MainCore implements Listener {
    private final Storage items;
    private final Storage shulkers;
    private final ConfigManager configManager;
    private final LootManager lootManager;
    private final ShulkerManager shulkerManager;
    private final ItemChecker itemChecker;
    private final EventManager eventHandler;

    public void fillInventoryWithRandomLoot(Inventory inventory) {
        lootManager.fillInventoryWithRandomLoot(inventory);
    }

    public void addShulker(Location location) {
        shulkerManager.addShulker(location);
    }

    public void addShulkerConfig(String id, Location location, boolean opened) {
        shulkerManager.addShulkerConfig(id, location, opened);
    }

    public void addItem(String id, ItemStack item, int chance, int minAmount, int maxAmount) {
        lootManager.addItem(id, item, chance, minAmount, maxAmount);
    }

    public boolean isShulker(Block placedBlock) {
        return shulkerManager.isShulker(placedBlock);
    }

    public boolean isValidKey(ItemStack item) {
        return itemChecker.isValidKey(item);
    }

    public boolean isKeyItem(ItemStack item) {
        return itemChecker.isKeyItem(item);
    }

    public boolean isCompassItem(ItemStack item) {
        return itemChecker.isCompassItem(item);
    }

    public int getRandomNumber(int min, int max) {
        return lootManager.getRandomNumber(min, max);
    }
}