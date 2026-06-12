package ru.truhot.rdang.сore.managers;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import java.util.function.Predicate;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.storage.Storage;
import ru.truhot.rdang.util.MessageUtil;
import ru.truhot.rdang.util.UndoUtil;
import ru.truhot.rdang.util.TimeUtil;

public class EventManager implements Listener {
    private final Storage shulkers;
    private final ConfigManager configManager;
    private final ShulkerManager shulkerManager;
    private final ItemChecker itemChecker;
    private final Random random = new Random();
    private final UndoUtil undoUtil;

    public EventManager(Storage shulkers, ConfigManager configManager, ShulkerManager shulkerManager, ItemChecker itemChecker, UndoUtil undoUtil) {
        this.shulkers = shulkers;
        this.configManager = configManager;
        this.shulkerManager = shulkerManager;
        this.itemChecker = itemChecker;
        this.undoUtil = undoUtil;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ConfigurationSection locsSection = this.shulkers.getConfig().getConfigurationSection("locs");
        if (locsSection != null && event.getClickedBlock() != null) {
            if (this.shulkerManager.isShulker(event.getClickedBlock())) {
                for (String itemId : locsSection.getKeys(false)) {
                    ConfigurationSection shulker = locsSection.getConfigurationSection(itemId);
                    Location shulkerLocation = shulker.getLocation("location");
                    if (isSameLocation(event.getClickedBlock().getLocation(), shulkerLocation) && !shulker.getBoolean("opened")) {
                        ItemStack itemInHand = event.getPlayer().getItemInHand();
                        if (itemInHand != null && itemInHand.getType() != Material.AIR && this.itemChecker.isValidKey(itemInHand)) {
                            ShulkerOpen(event, shulker, shulkerLocation, itemInHand);
                            return;
                        }
                        ShulkerLocked(event, shulkerLocation);
                        return;
                    }
                }
            }
        }
    }

    private void ShulkerOpen(PlayerInteractEvent event, ConfigurationSection shulker, Location loc, ItemStack item) {
        spawnEffect(loc, "open");
        playEffectSound(loc, "open");
        shulker.set("opened", true);
        this.shulkers.save();
        this.checkCleanup(loc);
        for (Player p : Bukkit.getOnlinePlayers()) {
            this.configManager.getMessageManager().getOpenDungMessages(event.getPlayer().getName()).forEach(p::sendMessage);
        }
        int chance = this.configManager.getItemManager().getSaveChance();
        if (Math.random() * 100.0 < chance) {
            event.getPlayer().sendMessage(this.configManager.getMessageManager().getSaveKeyMessage());
        } else {
            if (item.getAmount() > 1) item.setAmount(item.getAmount() - 1);
            else event.getPlayer().setItemInHand(new ItemStack(Material.AIR));
        }
    }

    private void ShulkerLocked(PlayerInteractEvent event, Location loc) {
        event.setCancelled(true);
        spawnEffect(loc, "locked");
        playEffectSound(loc, "locked");
        this.configManager.getMessageManager().getClosedDungMessages().forEach(event.getPlayer()::sendMessage);
    }

    @EventHandler
    public void onLoot(LootGenerateEvent e) {
        if (this.random.nextInt(100) < this.configManager.getItemManager().getSpawnChance()) {
            e.getLoot().add(this.configManager.getItemManager().getKey());
            if (e.getEntity() instanceof Player player) {
                String rawMsg = this.configManager.getMessages().getString("messages.key_found");
                Bukkit.broadcastMessage(MessageUtil.colorize(rawMsg.replace("{player}", player.getName())));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        processPiston(e.getBlocks(), e);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        processPiston(e.getBlocks(), e);
    }

    private void processPiston(List<Block> blocks, BlockPistonEvent e) {
        for (Block b : blocks) {
            if (this.shulkerManager.isShulker(b)) {
                e.setCancelled(true);
                break;
            }
        }
    }

    @EventHandler
    public void onCraft(PrepareItemCraftEvent event) {
        if (event.getInventory().getMatrix() == null) return;
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (item != null && (this.itemChecker.isKeyItem(item) || this.itemChecker.isCompassItem(item))) {
                event.getInventory().setResult(new ItemStack(Material.AIR));
                break;
            }
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (this.shulkerManager.isShulker(e.getBlock())) {
            ConfigurationSection locs = this.shulkers.getConfig().getConfigurationSection("locs");
            if (locs == null) return;
            for (String id : locs.getKeys(false)) {
                if (isSameLocation(e.getBlock().getLocation(), locs.getConfigurationSection(id).getLocation("location"))) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onKey(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item != null && this.itemChecker.isKeyItem(item)) {
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCompass(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (item == null || !this.itemChecker.isCompassItem(item)) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (player.hasCooldown(item.getType())) {
            String msg = this.configManager.getMessages().getString("messages.give.compass.cooldown");
            if (msg != null) {
                player.sendMessage(MessageUtil.colorize(msg.replace("{time}", TimeUtil.format(player.getCooldown(item.getType()) / 20))));
            }
            return;
        }
        Location loc = this.getRandomLocation(player);
        if (loc == null) {
            String noDangs = this.configManager.getMessages().getString("messages.give.compass.no_dangs");
            if (noDangs != null) player.sendMessage(MessageUtil.colorize(noDangs));
            return;
        }
        player.setCompassTarget(loc);
        String showMsg = this.configManager.getMessages().getString("messages.give.compass.showing_location");
        if (showMsg != null) {
            player.sendMessage(MessageUtil.colorize(showMsg
                    .replace("{x}", String.valueOf(loc.getBlockX()))
                    .replace("{y}", String.valueOf(loc.getBlockY()))
                    .replace("{z}", String.valueOf(loc.getBlockZ()))));
        }
        Sound s = this.configManager.getItemManager().getCompassSound();
        if (s != null) player.playSound(player.getLocation(), s, 1.0F, 1.0F);
        player.setCooldown(item.getType(), Math.toIntExact(this.configManager.getItemManager().getCompassCooldown() * 20));
        if (item.getAmount() > 1) item.setAmount(item.getAmount() - 1);
        else player.getInventory().setItem(event.getHand(), null);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof ShulkerBox shulker) {
            if (this.shulkerManager.isShulker(shulker.getBlock())) {
                this.checkCleanup(shulker.getLocation());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (this.itemChecker.isKeyItem(event.getItemInHand())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack stack = event.getItemDrop().getItemStack();
        ItemManager items = configManager.getItemManager();
        boolean glow = false;
        if (itemChecker.isKeyItem(stack) && items.isKeyGlow()) {
            glow = true;
        } else if (itemChecker.isCompassItem(stack) && items.isCompassGlow()) {
            glow = true;
        }
        if (glow) {
            event.getItemDrop().setGlowing(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        ItemManager items = configManager.getItemManager();
        Location loc = player.getLocation();

        if (items.isKeyDrop()) {
            dropFromInventory(player, loc, itemChecker::isKeyItem, items.isKeyGlow());
        }
        if (items.isCompassDrop()) {
            dropFromInventory(player, loc, itemChecker::isCompassItem, items.isCompassGlow());
        }
    }

    private void dropFromInventory(Player player, Location loc, Predicate<ItemStack> matcher, boolean glow) {
        Inventory inv = player.getInventory();
        for (int slot = 0; slot < inv.getSize(); slot++) {
            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType() == Material.AIR || !matcher.test(item)) continue;
            Item dropped = player.getWorld().dropItemNaturally(loc, item.clone());
            if (glow) {
                dropped.setGlowing(true);
            }
            inv.setItem(slot, null);
        }
    }

    private void checkCleanup(Location loc) {
        if (!this.configManager.getAuto().getBoolean("auto.enabled")) return;
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager manager = container.get(BukkitAdapter.adapt(loc.getWorld()));
        if (manager == null) return;
        String format = this.configManager.getRegion().getString("region.name_format", "dang_").replace("{id}", "");
        manager.getApplicableRegions(BukkitAdapter.asBlockVector(loc)).getRegions().stream()
                .filter(r -> r.getId().startsWith(format)).findFirst().ifPresent(region -> {
                    ConfigurationSection locs = this.shulkers.getConfig().getConfigurationSection("locs");
                    if (locs == null) return;
                    boolean hasLoot = locs.getKeys(false).stream().anyMatch(k -> {
                        ConfigurationSection s = locs.getConfigurationSection(k);
                        Location sLoc = s.getLocation("location");
                        if (sLoc == null || !sLoc.getWorld().getName().equals(loc.getWorld().getName()) || !region.contains(BukkitAdapter.asBlockVector(sLoc))) return false;
                        if (!s.getBoolean("opened")) return true;
                        if (sLoc.getBlock().getState() instanceof ShulkerBox sb) {
                            for (ItemStack i : sb.getInventory().getContents()) if (i != null && i.getType() != Material.AIR) return true;
                        }
                        return false;
                    });
                    if (!hasLoot) this.undoUtil.scheduleAutoUndo(region.getId(), loc.getWorld(), region);
                });
    }

    private Location getRandomLocation(Player player) {
        ConfigurationSection sec = this.shulkers.getConfig().getConfigurationSection("locs");
        if (sec == null || sec.getKeys(false).isEmpty()) return null;
        String worldName = player.getWorld().getName();
        List<Location> inWorld = new ArrayList<>();
        for (String key : sec.getKeys(false)) {
            ConfigurationSection shulker = sec.getConfigurationSection(key);
            if (shulker == null) continue;
            Location shulkerLoc = shulker.getLocation("location");
            if (shulkerLoc != null && shulkerLoc.getWorld() != null && shulkerLoc.getWorld().getName().equals(worldName)) {
                inWorld.add(shulkerLoc);
            }
        }
        if (inWorld.isEmpty()) return null;
        return inWorld.get(this.random.nextInt(inWorld.size()));
    }

    private boolean isSameLocation(Location l1, Location l2) {
        return l1.getBlockX() == l2.getBlockX() && l1.getBlockY() == l2.getBlockY() && l1.getBlockZ() == l2.getBlockZ() && l1.getWorld().getName().equals(l2.getWorld().getName());
    }

    private void spawnEffect(Location loc, String path) {
        String type = this.configManager.getShulker().getString("particles." + path + ".type", "TOTEM");
        Particle p = Particle.TOTEM;
        try { p = Particle.valueOf(type.toUpperCase()); } catch (Exception ignored) {}
        int count = this.configManager.getShulker().getInt("particles." + path + ".count", 20);
        double ox = this.configManager.getShulker().getDouble("particles." + path + ".offsetX", 1.5);
        double oy = this.configManager.getShulker().getDouble("particles." + path + ".offsetY", 1.5);
        double oz = this.configManager.getShulker().getDouble("particles." + path + ".offsetZ", 1.5);
        double extra = this.configManager.getShulker().getDouble("particles." + path + ".extra", 0.1);
        loc.getWorld().spawnParticle(p, loc.clone().add(0.5, 1.0, 0.5), count, ox, oy, oz, extra);
    }

    private void playEffectSound(Location loc, String path) {
        String type = this.configManager.getShulker().getString("sounds." + path + ".type", "BLOCK_BARREL_CLOSE");
        try {
            Sound s = Sound.valueOf(type.toUpperCase());
            float v = (float) this.configManager.getShulker().getInt("sounds." + path + ".volume", 50) / 100.0F;
            float p = (float) this.configManager.getShulker().getDouble("sounds." + path + ".pitch", 1.0);
            loc.getWorld().playSound(loc, s, v, p);
        } catch (Exception ignored) {}
    }
}