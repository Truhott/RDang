package ru.truhot.rdang.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import ru.truhot.rdang.RDang;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.util.MessageUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class AbstractMenu {

    protected final ConfigManager configManager;
    protected final RDang plugin;
    protected final Map<UUID, Inventory> playerInventories = new HashMap<>();

    public AbstractMenu(ConfigManager configManager, RDang plugin) {
        this.configManager = configManager;
        this.plugin = plugin;
    }

    public abstract void openMenu(Player player, int page);
    protected abstract String getMenuId();
    protected abstract Inventory createInventory(Player player, int page);
    protected abstract void handleMenuClick(Player player, InventoryClickEvent e);

    public void open(Player player, int page) {
        Inventory inventory = createInventory(player, page);
        if (inventory == null) {
            player.sendMessage(MessageUtil.colorize("&cОшибка при создании меню!"));
            return;
        }

        player.openInventory(inventory);
        playerInventories.put(player.getUniqueId(), inventory);
    }

    public void close(Player player, InventoryCloseEvent e) {
        playerInventories.remove(player.getUniqueId());
    }

    public void onClick(Player player, InventoryClickEvent e) {
        if (!isValidClick(e)) return;
        handleMenuClick(player, e);
    }

    protected boolean isValidClick(InventoryClickEvent e) {
        return e.getWhoClicked() instanceof Player &&
                e.getInventory().getHolder() instanceof MenuHolder &&
                ((MenuHolder) e.getInventory().getHolder()).getMenuId().equals(getMenuId());
    }
}