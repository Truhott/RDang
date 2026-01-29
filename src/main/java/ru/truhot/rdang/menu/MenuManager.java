package ru.truhot.rdang.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import ru.truhot.rdang.RDang;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.storage.Storage;

import java.util.HashMap;
import java.util.Map;

public class MenuManager implements Listener {

    private final ConfigManager configManager;
    private final Storage shulkers;
    private final Storage blockStorage;
    private final RDang plugin;
    private final Map<String, AbstractMenu> menus = new HashMap<>();
    private DungeonListMenu dungeonListMenu;

    public MenuManager(ConfigManager configManager, Storage shulkers, Storage blockStorage, RDang plugin) {
        this.configManager = configManager;
        this.shulkers = shulkers;
        this.blockStorage = blockStorage;
        this.plugin = plugin;
        registerMenus();
    }

    private void registerMenus() {
        this.dungeonListMenu = new DungeonListMenu(configManager, shulkers, blockStorage, plugin);
        menus.put("dungeon_list", dungeonListMenu);
    }

    public void openDungeonList(Player player, int page) {
        if (dungeonListMenu != null) {
            dungeonListMenu.openMenu(player, page);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        for (AbstractMenu menu : menus.values()) {
            menu.onClick(player, e);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;
        for (AbstractMenu menu : menus.values()) {
            menu.close(player, e);
        }
    }
}