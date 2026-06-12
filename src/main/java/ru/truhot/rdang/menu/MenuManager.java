package ru.truhot.rdang.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import ru.truhot.rdang.RDang;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.storage.Storage;
import ru.truhot.rdang.util.UndoUtil;
import ru.truhot.rdang.сore.managers.LootManager;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MenuManager implements Listener {

    private final Map<MenuType, AbstractMenu> menus = new EnumMap<>(MenuType.class);
    private final LootEditMenu lootEditMenu;
    private final Map<UUID, MenuType> CloseMessageFor = new ConcurrentHashMap<>();

    public MenuManager(ConfigManager configManager, Storage items, Storage shulkers,
                       RDang plugin, LootManager lootManager, UndoUtil undoUtil) {
        menus.put(MenuType.DUNGEON_LIST, new ListMenu(configManager, shulkers, plugin, undoUtil));
        menus.put(MenuType.MAIN, new MainMenu(configManager, plugin, this));
        menus.put(MenuType.LOOT_PAGES, new LootPagesMenu(configManager, plugin, this, lootManager));
        this.lootEditMenu = new LootEditMenu(configManager, plugin, this, lootManager);
        menus.put(MenuType.LOOT_EDIT, lootEditMenu);
        menus.put(MenuType.LOOT_CHANCE, new LootChanceMenu(configManager, plugin, this, lootManager));
    }

    public void open(MenuType type, Player player) {
        open(type, player, 0);
    }

    public void open(MenuType type, Player player, int page) {
        AbstractMenu menu = menus.get(type);
        if (menu != null) {
            menu.openMenu(player, page);
        }
    }

    public void navigateTo(Player player, MenuType from, MenuType to) {
        navigateTo(player, from, to, 0);
    }

    public void navigateTo(Player player, MenuType from, MenuType to, int page) {
        CloseMessageFor.put(player.getUniqueId(), from);
        player.closeInventory();
        open(to, player, page);
    }

    public boolean CloseMessage(Player player, MenuType closingMenu) {
        MenuType suppressed = CloseMessageFor.remove(player.getUniqueId());
        return suppressed != null && suppressed == closingMenu;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof MenuHolder holder)) return;

        AbstractMenu menu = menus.get(holder.getType());
        if (menu == null) return;

        menu.onClick(player, event);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof MenuHolder holder)) return;

        AbstractMenu menu = menus.get(holder.getType());
        if (menu != null) {
            menu.close(player, event);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        lootEditMenu.onDrag(event);
    }
}
