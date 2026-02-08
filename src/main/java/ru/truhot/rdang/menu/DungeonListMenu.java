package ru.truhot.rdang.menu;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.truhot.rdang.RDang;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.storage.Storage;
import ru.truhot.rdang.util.HeadUtil;
import ru.truhot.rdang.util.MessageUtil;
import ru.truhot.rdang.util.TeleportUtil;
import ru.truhot.rdang.util.UndoUtil;

import java.util.ArrayList;
import java.util.List;

public class DungeonListMenu extends AbstractMenu {

    private static final int ITEMS_PER_PAGE = 45;
    private static final int INVENTORY_SIZE = 54;
    private static final int[] BOTTOM_BAR_SLOTS = {45, 46, 47, 48, 49, 50, 51, 52, 53};
    private static final int NEXT_PAGE_SLOT = 50;
    private static final int PREV_PAGE_SLOT = 48;

    private final TeleportUtil teleportUtil;
    private final String regionNameFormat;
    private final Storage shulkers;
    private final Storage blockStorage;

    public DungeonListMenu(ConfigManager configManager, Storage shulkers, Storage blockStorage, RDang plugin) {
        super(configManager, plugin);
        this.shulkers = shulkers;
        this.blockStorage = blockStorage;
        this.teleportUtil = new TeleportUtil(configManager);
        this.regionNameFormat = configManager.getRegion().getString("region.name_format", "dang_{id}");
    }

    @Override
    public void openMenu(Player player, int page) {
        List<String> regionIds = loadRegionIdsForPage(page);
        if (regionIds.isEmpty() && page == 0) {
            String message = configManager.getMessages().getString("messages.list.no-dungeons");
            player.sendMessage(MessageUtil.colorize(message));
            return;
        }
        open(player, page);
    }

    @Override
    protected String getMenuId() {
        return "dungeon_list";
    }

    @Override
    protected Inventory createInventory(Player player, int page) {
        List<String> allRegionIds = getAllRegionIds();
        int totalRegions = allRegionIds.size();
        int maxPages = Math.max(1, (int) Math.ceil((double) totalRegions / ITEMS_PER_PAGE));
        page = Math.max(0, Math.min(page, maxPages - 1));
        List<String> pageRegionIds = getPageRegionIds(allRegionIds, page);
        String title = MessageUtil.colorize("§fСписок Данжей &7(" + (page + 1) + "/" + maxPages + ")");
        MenuHolder holder = new MenuHolder(getMenuId(), page);
        Inventory inventory = Bukkit.createInventory(holder, INVENTORY_SIZE, title);
        holder.setInventory(inventory);
        addDungeonItems(inventory, pageRegionIds);
        fillBottomBar(inventory, page, maxPages);
        return inventory;
    }

    @Override
    protected void handleMenuClick(Player player, InventoryClickEvent e) {
        e.setCancelled(true);
        int slot = e.getRawSlot();
        ItemStack clickedItem = e.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR || slot >= ITEMS_PER_PAGE) return;
        if (clickedItem.getType() == Material.PLAYER_HEAD) {
            ItemMeta meta = clickedItem.getItemMeta();
            if (meta != null) {
                String name = meta.getDisplayName();
                if (name.contains("Следующая") || name.contains("Предыдущая")) {
                    MenuHolder holder = (MenuHolder) e.getInventory().getHolder();
                    int newPage = holder.getPage() + (name.contains("Следующая") ? 1 : -1);
                    openMenu(player, newPage);
                    return;
                }
            }
        }

        if (isDungeonItem(clickedItem)) {
            MenuHolder holder = (MenuHolder) e.getInventory().getHolder();
            List<String> currentIds = loadRegionIdsForPage(holder.getPage());
            if (slot >= currentIds.size()) return;
            String regionId = currentIds.get(slot);
            String dungeonDisplayId = String.valueOf(extractRegionNumber(regionId));
            if (e.isRightClick()) {
                player.closeInventory();
                teleportUtil.teleportToDungeon(player, regionId);
            }
            else if (e.isLeftClick()) {
                UndoUtil undoUtil = new UndoUtil(configManager, shulkers, blockStorage, plugin);
                UndoUtil.UndoResult result = undoUtil.performUndo(regionId);
                if (result.found) {
                    player.sendMessage(MessageUtil.colorize(configManager.getMessages().getString("messages.undo.region-deleted")
                            .replace("{id}", dungeonDisplayId)
                            .replace("{region}", regionId)
                            .replace("{world}", result.worldName)));

                    player.sendMessage(MessageUtil.colorize(configManager.getMessages().getString("messages.undo.shulkers-deleted")
                            .replace("{shulker}", String.valueOf(result.shulkerCount))));
                    openMenu(player, holder.getPage());
                } else {
                    player.sendMessage(MessageUtil.colorize(configManager.getMessages().getString("messages.undo.region-not-found")
                            .replace("{id}", dungeonDisplayId)
                            .replace("{region}", regionId)));
                }
            }
        }
    }

    private List<String> getAllRegionIds() {
        List<String> regionIds = new ArrayList<>();
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            if (container == null) return regionIds;
            String format = configManager.getRegion().getString("region.name_format");
            String prefix = format.contains("{id}") ? format.split("\\{id\\}")[0] : format;
            for (World world : Bukkit.getWorlds()) {
                RegionManager regionManager = container.get(BukkitAdapter.adapt(world));
                if (regionManager != null) {
                    for (String regionName : regionManager.getRegions().keySet()) {
                        if (regionName.toLowerCase().startsWith(prefix.toLowerCase())) {
                            regionIds.add(regionName);
                        }
                    }
                }
            }
            regionIds.sort((r1, r2) -> Integer.compare(extractRegionNumber(r1), extractRegionNumber(r2)));
        } catch (Exception ignored) {}
        return regionIds;
    }

    private int extractRegionNumber(String regionId) {
        try {
            String numberOnly = regionId.replaceAll("[^0-9]", "");
            return numberOnly.isEmpty() ? 0 : Integer.parseInt(numberOnly);
        } catch (Exception e) {
            return 0;
        }
    }

    private String extractDungeonNumber(String displayName) {
        try {
            String cleanName = displayName.replaceAll("§[0-9a-fk-orx]", "");
            int start = cleanName.indexOf("[") + 1;
            int end = cleanName.indexOf("]");
            return cleanName.substring(start, end).trim();
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> loadRegionIdsForPage(int page) {
        List<String> allRegionIds = getAllRegionIds();
        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, allRegionIds.size());
        return (start >= allRegionIds.size()) ? new ArrayList<>() : allRegionIds.subList(start, end);
    }

    private List<String> getPageRegionIds(List<String> allRegionIds, int page) {
        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, allRegionIds.size());
        return (start >= allRegionIds.size()) ? new ArrayList<>() : allRegionIds.subList(start, end);
    }

    private void addDungeonItems(Inventory inventory, List<String> regionIds) {
        for (int i = 0; i < regionIds.size() && i < ITEMS_PER_PAGE; i++) {
            inventory.setItem(i, createDungeonItem(regionIds.get(i)));
        }
    }

    private ItemStack createDungeonItem(String regionId) {
        int dungeonNumber = extractRegionNumber(regionId);
        World regionWorld = getWorldForRegion(regionId);
        Material material = Material.DIRT;
        if (regionWorld != null) {
            String worldName = regionWorld.getName().toLowerCase();
            if (worldName.contains("nether")) material = Material.NETHERRACK;
            else if (worldName.contains("end")) material = Material.END_STONE;
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtil.colorize("&fДанж &8[" + dungeonNumber + "]"));
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(MessageUtil.colorize(" &fМир: &#557c93" + (regionWorld != null ? regionWorld.getName() : "Неизвестен")));
            lore.add(MessageUtil.colorize(" &fКорды: &#FEF06A" + getCoordinates(regionWorld, regionId)));
            lore.add("");
            lore.add(MessageUtil.colorize(" &fПри нажатии &#557c93ПКМ&f телепортирует"));
            lore.add(MessageUtil.colorize(" &fПри нажатии &#FE6A6AЛКМ&f удаляет"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void fillBottomBar(Inventory inventory, int currentPage, int maxPages) {
        ItemStack blackPane = createPane(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int slot : BOTTOM_BAR_SLOTS) inventory.setItem(slot, blackPane);
        inventory.setItem(49, createInfoItem());
        if (currentPage > 0) inventory.setItem(PREV_PAGE_SLOT, createPreviousPageItem());
        if (currentPage < maxPages - 1) inventory.setItem(NEXT_PAGE_SLOT, createNextPageItem());
    }

    private ItemStack createPreviousPageItem() {
        ItemStack head = HeadUtil.createSkullFromBase64("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzYyNTkwMmIzODllZDZjMTQ3NTc0ZTQyMmRhOGY4ZjM2MWM4ZWI1N2U3NjMxNjc2YTcyNzc3ZTdiMWQifX19", "menu");
        ItemMeta meta = head.getItemMeta();
        if (meta != null) meta.setDisplayName(MessageUtil.colorize("§f[ §7← §f] §6Предыдущая страница"));
        head.setItemMeta(meta);
        return head;
    }

    private ItemStack createNextPageItem() {
        ItemStack head = HeadUtil.createSkullFromBase64("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDRiZThhZWVjMTE4NDk2OTdhZGM2ZmQxZjE4OWIxNjY0MmRmZjE5ZjI5NTVjMDVkZWFiYTY4YzlkZmYxYmUifX19", "menu");
        ItemMeta meta = head.getItemMeta();
        if (meta != null) meta.setDisplayName(MessageUtil.colorize("§6Следующая страница §f[ §7→ §f]"));
        head.setItemMeta(meta);
        return head;
    }

    private ItemStack createInfoItem() {
        ItemStack head = HeadUtil.createSkullFromBase64("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTM1OWQ5MTI3NzI0MmZjMDFjMzA5YWNjYjg3YjUzM2YxOTI5YmUxNzZlY2JhMmNkZTYzYmY2MzVlMDVlNjk5YiJ9fX0=", "menu");
        ItemMeta meta = head.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(MessageUtil.colorize("§7[&#FEF06A₪§7] §fИнформация"));
            meta.setLore(List.of("", MessageUtil.colorize(" &fСписок всех активных данжей"), ""));
            head.setItemMeta(meta);
        }
        return head;
    }

    private ItemStack createPane(Material material, String name) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) { meta.setDisplayName(name); pane.setItemMeta(meta); }
        return pane;
    }

    private World getWorldForRegion(String regionId) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        for (World world : Bukkit.getWorlds()) {
            RegionManager wgManager = container.get(BukkitAdapter.adapt(world));
            if (wgManager != null && wgManager.hasRegion(regionId)) return world;
        }
        return null;
    }

    private String getCoordinates(World world, String regionId) {
        if (world == null) return "Недоступно";
        try {
            RegionManager wgManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
            ProtectedRegion region = wgManager.getRegion(regionId);
            BlockVector3 min = region.getMinimumPoint();
            BlockVector3 max = region.getMaximumPoint();
            return String.format("X: %d Y: %d Z: %d", (min.getBlockX() + max.getBlockX()) / 2, (min.getBlockY() + max.getBlockY()) / 2, (min.getBlockZ() + max.getBlockZ()) / 2);
        } catch (Exception e) { return "Недоступно"; }
    }

    private boolean isDungeonItem(ItemStack item) {
        Material type = item.getType();
        return type == Material.DIRT || type == Material.NETHERRACK || type == Material.END_STONE;
    }
}