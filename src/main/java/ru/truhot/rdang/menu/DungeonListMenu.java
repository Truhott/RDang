package ru.truhot.rdang.menu;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
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
import ru.truhot.rdang.util.*;

import java.util.*;

public class DungeonListMenu extends AbstractMenu {
    private static final int ITEMS_PER_PAGE = 45, INVENTORY_SIZE = 54, NEXT_PAGE_SLOT = 50, PREV_PAGE_SLOT = 48;
    private final TeleportUtil teleportUtil;
    private final Storage shulkers, blockStorage;
    private final String prefix;
    private final ItemStack guiGlass = createPane(Material.BLACK_STAINED_GLASS_PANE, " ");
    private final ItemStack infoItem, nextBtn, prevBtn;
    private final Map<Material, ItemStack> dungeonTemplates = new EnumMap<>(Material.class);

    public DungeonListMenu(ConfigManager configManager, Storage shulkers, Storage blockStorage, RDang plugin) {
        super(configManager, plugin);
        this.shulkers = shulkers;
        this.blockStorage = blockStorage;
        this.teleportUtil = new TeleportUtil(configManager);
        String format = configManager.getRegion().getString("region.name_format", "dang_{id}");
        this.prefix = format.contains("{id}") ? format.split("\\{id\\}")[0].toLowerCase() : format.toLowerCase();
        this.infoItem = createInfoItem();
        this.nextBtn = createNextBtn();
        this.prevBtn = createPrevBtn();
        preCacheDungeonTemplates();
    }

    private void preCacheDungeonTemplates() {
        for (Material m : List.of(Material.DIRT, Material.NETHERRACK, Material.END_STONE)) {
            ItemStack item = new ItemStack(m);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(MessageUtil.colorize("&fДанж &8[%d]"));
                meta.setLore(MessageUtil.colorize(List.of("", " &fМир: &#557c93%s", " &fКорды: &#FEF06A%s", "", " &fПри нажатии &#557c93ПКМ&f телепортирует", " &fПри нажатии &#FE6A6AЛКМ&f удаляет")));
                item.setItemMeta(meta);
            }
            dungeonTemplates.put(m, item);
        }
    }

    @Override
    public void openMenu(Player player, int page) {
        List<String> allIds = getAllRegionIds();
        if (allIds.isEmpty() && page == 0) {
            player.sendMessage(MessageUtil.colorize(configManager.getMessages().getString("messages.list.no-dungeons")));
            player.closeInventory();
            return;
        }
        open(player, page);
    }

    @Override
    protected Inventory createInventory(Player player, int page) {
        List<String> allIds = getAllRegionIds();
        int maxPages = Math.max(1, (int) Math.ceil((double) allIds.size() / ITEMS_PER_PAGE));
        int curPage = Math.max(0, Math.min(page, maxPages - 1));
        Inventory inv = Bukkit.createInventory(new MenuHolder(getMenuId(), curPage), INVENTORY_SIZE, MessageUtil.getFormatted("§fСписок Данжей &7(%d/%d)", curPage + 1, maxPages));
        int start = curPage * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE && (start + i) < allIds.size(); i++) inv.setItem(i, getDungeonItemStack(allIds.get(start + i)));
        for (int s = 45; s < 54; s++) inv.setItem(s, guiGlass);
        inv.setItem(49, infoItem);
        if (curPage > 0) inv.setItem(PREV_PAGE_SLOT, prevBtn);
        if (curPage < maxPages - 1) inv.setItem(NEXT_PAGE_SLOT, nextBtn);
        return inv;
    }

    private ItemStack getDungeonItemStack(String regionId) {
        World world = getWorldForRegion(regionId);
        Material m = Material.DIRT;
        if (world != null) {
            String name = world.getName().toLowerCase();
            if (name.contains("nether")) m = Material.NETHERRACK;
            else if (name.contains("end")) m = Material.END_STONE;
        }
        ItemStack item = dungeonTemplates.get(m).clone();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(String.format(meta.getDisplayName(), extractRegionNumber(regionId)));
            List<String> lore = meta.getLore();
            if (lore != null) {
                lore.set(1, String.format(lore.get(1), world != null ? world.getName() : "Unknown"));
                lore.set(2, String.format(lore.get(2), getCoordinates(world, regionId)));
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    protected void handleMenuClick(Player player, InventoryClickEvent e) {
        e.setCancelled(true);
        ItemStack item = e.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;
        MenuHolder holder = (MenuHolder) e.getInventory().getHolder();
        int slot = e.getRawSlot();
        int currentPage = holder.getPage();
        if (slot == NEXT_PAGE_SLOT) {
            openMenu(player, currentPage + 1);
            return;
        }
        if (slot == PREV_PAGE_SLOT) {
            if (currentPage > 0) openMenu(player, currentPage - 1);
            return;
        }
        if (slot >= ITEMS_PER_PAGE) return;
        if (dungeonTemplates.containsKey(item.getType())) {
            List<String> ids = getAllRegionIds();
            int index = (currentPage * ITEMS_PER_PAGE) + slot;
            if (index < ids.size()) {
                String rId = ids.get(index);
                if (e.isRightClick()) {
                    player.closeInventory();
                    teleportUtil.teleportToDungeon(player, rId);
                } else if (e.isLeftClick()) {
                    UndoUtil.UndoResult res = new UndoUtil(configManager, shulkers, blockStorage, plugin).performUndo(rId);
                    String msg = configManager.getMessages().getString(res.found ? "messages.undo.region-deleted" : "messages.undo.region-not-found");
                    player.sendMessage(MessageUtil.getFormatted(msg, extractRegionNumber(rId), rId, res.worldName));
                    if (res.found) openMenu(player, currentPage);
                }
            }
        }
    }

    private List<String> getAllRegionIds() {
        List<String> ids = new ArrayList<>();
        for (World w : Bukkit.getWorlds()) {
            RegionManager rm = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(w));
            if (rm != null) { for (String n : rm.getRegions().keySet()) if (n.toLowerCase().startsWith(prefix)) ids.add(n); }
        }
        ids.sort(Comparator.comparingInt(this::extractRegionNumber));
        return ids;
    }

    private String getCoordinates(World w, String id) {
        if (w == null) return "N/A";
        try {
            ProtectedRegion r = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(w)).getRegion(id);
            if (r == null) return "N/A";
            BlockVector3 min = r.getMinimumPoint(), max = r.getMaximumPoint();
            return String.format("%d, %d, %d", (min.getBlockX()+max.getBlockX())/2, (min.getBlockY()+max.getBlockY())/2, (min.getBlockZ()+max.getBlockZ())/2);
        } catch (Exception e) { return "N/A"; }
    }

    private int extractRegionNumber(String id) {
        String n = id.replaceAll("[^0-9]", "");
        return n.isEmpty() ? 0 : Integer.parseInt(n);
    }

    private ItemStack createPane(Material m, String n) {
        ItemStack i = new ItemStack(m);
        ItemMeta mt = i.getItemMeta();
        if (mt != null) { mt.setDisplayName(n); i.setItemMeta(mt); }
        return i;
    }

    private ItemStack createNextBtn() {
        ItemStack i = HeadUtil.createSkullFromBase64("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDRiZThhZWVjMTE4NDk2OTdhZGM2ZmQxZjE4OWIxNjY0MmRmZjE5ZjI5NTVjMDVkZWFiYTY4YzlkZmYxYmUifX19", "menu");
        ItemMeta m = i.getItemMeta();
        if (m != null) m.setDisplayName(MessageUtil.colorize("§6Следующая страница §f[ §7→ §f]"));
        i.setItemMeta(m);
        return i;
    }

    private ItemStack createPrevBtn() {
        ItemStack i = HeadUtil.createSkullFromBase64("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzYyNTkwMmIzODllZDZjMTQ3NTc0ZTQyMmRhOGY4ZjM2MWM4ZWI1N2U3NjMxNjc2YTcyNzc3ZTdiMWQifX19", "menu");
        ItemMeta m = i.getItemMeta();
        if (m != null) m.setDisplayName(MessageUtil.colorize("§f[ §7← §f] §6Предыдущая страница"));
        i.setItemMeta(m);
        return i;
    }

    private ItemStack createInfoItem() {
        ItemStack i = HeadUtil.createSkullFromBase64("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTM1OWQ5MTI3NzI0MmZjMDFjMzA5YWNjYjg3YjUzM2YxOTI5YmUxNzZlY2JhMmNkZTYzYmY2MzVlMDVlNjk5YiJ9fX0=", "menu");
        ItemMeta m = i.getItemMeta();
        if (m != null) { m.setDisplayName(MessageUtil.colorize("§7[&#FEF06A₪§7] §fИнформация")); m.setLore(List.of("", MessageUtil.colorize(" &fСписок всех активных данжей"), "")); i.setItemMeta(m); }
        return i;
    }

    private World getWorldForRegion(String id) {
        for (World w : Bukkit.getWorlds()) {
            RegionManager rm = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(w));
            if (rm != null && rm.hasRegion(id)) return w;
        }
        return null;
    }

    @Override protected String getMenuId() { return "dungeon_list"; }
}