package ru.truhot.rdang.util;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import ru.truhot.rdang.RDang;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.storage.BlockStore;
import ru.truhot.rdang.storage.Storage;
import ru.truhot.rdang.util.logger.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class UndoUtil {
    private final ConfigManager configManager;
    private final Storage shulkers;
    private final BlockStore blockStore;
    private final RDang plugin;
    private volatile BiConsumer<World, String> respawnHandler;
    private final ConcurrentHashMap<String, Integer> activeAutoUndoTasks = new ConcurrentHashMap<>();

    public static class UndoResult {
        public final int shulkerCount;
        public final String worldName;
        public final boolean found;
        public final String schematic;

        public UndoResult(int shulkerCount, String worldName, boolean found, String schematic) {
            this.shulkerCount = shulkerCount;
            this.worldName = worldName;
            this.found = found;
            this.schematic = schematic;
        }
    }

    public UndoUtil(ConfigManager configManager, Storage shulkers, BlockStore blockStore, RDang plugin) {
        this.configManager = configManager;
        this.shulkers = shulkers;
        this.blockStore = blockStore;
        this.plugin = plugin;
    }

    public void setRespawnHandler(BiConsumer<World, String> respawnHandler) {
        this.respawnHandler = respawnHandler;
    }

    public void captureTerrainAndSave(String regionName, Location location, BlockVector3 minPoint, String schematic, Consumer<Boolean> onComplete) {
        CuboidRegion region = buildTerrainRegion(location);
        new BukkitRunnable() {
            @Override
            public void run() {
                BlockArrayClipboard clipboard;
                try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(location.getWorld()))) {
                    clipboard = new BlockArrayClipboard(region);
                    clipboard.setOrigin(region.getMinimumPoint());
                    ForwardExtentCopy copy = new ForwardExtentCopy(editSession, region, clipboard, region.getMinimumPoint());
                    copy.setCopyingEntities(false);
                    Operations.complete(copy);
                } catch (Exception e) {
                    Logger.error("Ошибка чтения территории: " + regionName);
                    runCallback(onComplete, false);
                    return;
                }
                BlockArrayClipboard finalClipboard = clipboard;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        try {
                            ByteArrayOutputStream output = new ByteArrayOutputStream();
                            try (ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(output)) {
                                writer.write(finalClipboard);
                            }
                            blockStore.put(
                                    regionName,
                                    location.getWorld().getName(),
                                    minPoint,
                                    schematic,
                                    output.toByteArray()
                            );
                            runCallback(onComplete, true);
                        } catch (Exception e) {
                            Logger.error("block.db: не удалось сохранить " + regionName);
                            runCallback(onComplete, false);
                        }
                    }
                }.runTaskAsynchronously(plugin);
            }
        }.runTask(plugin);
    }

    public UndoResult performUndo(String regionId) {
        return performUndo(regionId, true);
    }

    public int performUndoAll(List<String> regionIds) {
        if (regionIds == null || regionIds.isEmpty()) {
            return 0;
        }
        int done = 0;
        int shulkersRemoved = 0;
        for (String regionId : regionIds) {
            UndoResult result = performUndo(regionId, false);
            if (result.found) {
                done++;
                shulkersRemoved += result.shulkerCount;
            }
        }
        if (shulkersRemoved > 0) {
            shulkers.save();
        }
        return done;
    }

    private UndoResult performUndo(String regionId, boolean saveShulkersNow) {
        cancelAutoUndo(regionId);
        Optional<BlockStore.Snapshot> snapshot = blockStore.get(regionId);
        if (snapshot.isEmpty()) {
            return new UndoResult(0, "Неизвестно", false, null);
        }
        BlockStore.Snapshot data = snapshot.get();
        World world = Bukkit.getWorld(data.world);
        if (world == null) {
            return new UndoResult(0, data.world, false, data.schematic);
        }

        blockStore.remove(regionId);

        int removedShulkers = 0;
        RegionManager manager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
        if (manager != null && manager.hasRegion(regionId)) {
            ProtectedRegion region = manager.getRegion(regionId);
            removedShulkers = removeShulkersInRegion(region, world);
            manager.removeRegion(regionId);
        }

        if (removedShulkers > 0 && saveShulkersNow) {
            shulkers.save();
        }

        pasteTerrainAsync(regionId, world, data);
        return new UndoResult(removedShulkers, data.world, true, data.schematic);
    }

    private void pasteTerrainAsync(String regionId, World world, BlockStore.Snapshot snapshot) {
        BlockVector3 anchor = snapshot.anchor();
        new BukkitRunnable() {
            @Override
            public void run() {
                Clipboard clipboard;
                BlockVector3 offset;
                try {
                    byte[] raw = snapshot.terrainBytes();
                    try (ByteArrayInputStream input = new ByteArrayInputStream(raw);
                         ClipboardReader reader = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getReader(input)) {
                        clipboard = reader.read();
                    }
                    offset = anchor.subtract(clipboard.getMinimumPoint());
                } catch (Exception e) {
                    Logger.error("block.db: не удалось прочитать " + regionId);
                    return;
                }
                Clipboard ready = clipboard;
                BlockVector3 pasteAt = clipboard.getMinimumPoint().add(offset);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        try (EditSession session = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(world))) {
                            ForwardExtentCopy copy = new ForwardExtentCopy(
                                    ready, ready.getRegion(), session, pasteAt
                            );
                            copy.setCopyingEntities(false);
                            Operations.complete(copy);
                        } catch (Exception e) {
                            Logger.error("block.db: не удалось восстановить " + regionId);
                        }
                    }
                }.runTask(plugin);
            }
        }.runTaskAsynchronously(plugin);
    }

    private void cancelAutoUndo(String regionName) {
        Integer taskId = activeAutoUndoTasks.remove(regionName);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    public void scheduleAutoUndo(String regionName, World world, ProtectedRegion region) {
        String timeStr = configManager.getAuto().getString("auto.time");
        long seconds = TimeUtil.parse(timeStr);
        String rawMsg = configManager.getAuto().getString("auto.actionbar.timer");
        boolean actionbarEnabled = configManager.getAuto().getBoolean("auto.actionbar.enabled", true);
        if (activeAutoUndoTasks.containsKey(regionName)) {
            return;
        }

        BukkitRunnable task = new BukkitRunnable() {
            private long timeLeft = seconds;
            @Override
            public void run() {
                if (timeLeft <= 0) {
                    UndoResult res = performUndo(regionName);
                    BiConsumer<World, String> handler = respawnHandler;
                    if (res.found
                            && handler != null
                            && configManager.getAuto().getBoolean("auto.respawn.enabled", true)
                            && (!configManager.getAuto().getBoolean("auto.respawn.only-players", false) || !world.getPlayers().isEmpty())) {
                        long delayTicks = Math.max(0L, configManager.getAuto().getLong("auto.respawn.delay", 20L));
                        if (delayTicks == 0L) {
                            handler.accept(world, res.schematic);
                        } else {
                            Bukkit.getScheduler().runTaskLater(plugin, () -> handler.accept(world, res.schematic), delayTicks);
                        }
                    }
                    this.cancel();
                    return;
                }
                if (actionbarEnabled && rawMsg != null && !rawMsg.isEmpty()) {
                    String formattedTime = TimeUtil.format(timeLeft);
                    String finalMsg = MessageUtil.colorize(rawMsg.replace("{time}", formattedTime));
                    for (Player player : world.getPlayers()) {
                        if (region.contains(BukkitAdapter.asBlockVector(player.getLocation()))) {
                            player.spigot().sendMessage(
                                    net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                                    new net.md_5.bungee.api.chat.TextComponent(finalMsg)
                            );
                        }
                    }
                }
                timeLeft--;
            }
        };
        int taskId = task.runTaskTimer(plugin, 0L, 20L).getTaskId();
        activeAutoUndoTasks.put(regionName, taskId);
    }

    private int removeShulkersInRegion(ProtectedRegion region, World world) {
        ConfigurationSection locs = shulkers.getConfig().getConfigurationSection("locs");
        if (locs == null) {
            return 0;
        }
        List<String> keys = new ArrayList<>(locs.getKeys(false));
        int removed = 0;
        String worldName = world.getName();
        for (String key : keys) {
            Location loc = locs.getLocation(key + ".location");
            if (loc == null || loc.getWorld() == null || !loc.getWorld().getName().equals(worldName)) {
                continue;
            }
            if (!region.contains(BukkitAdapter.asBlockVector(loc))) {
                continue;
            }
            locs.set(key, null);
            removed++;
        }
        return removed;
    }

    private CuboidRegion buildTerrainRegion(Location location) {
        int radiusX = configManager.getRegion().getInt("region.size.x", 12);
        int radiusZ = configManager.getRegion().getInt("region.size.z", 12);
        int minY = configManager.getRegion().getInt("region.height.min", 0);
        int maxY = configManager.getRegion().getInt("region.height.max", 255);
        BlockVector3 min = BlockVector3.at(location.getBlockX() - radiusX, minY, location.getBlockZ() - radiusZ);
        BlockVector3 max = BlockVector3.at(location.getBlockX() + radiusX, maxY, location.getBlockZ() + radiusZ);
        return new CuboidRegion(BukkitAdapter.adapt(location.getWorld()), min, max);
    }

    private void runCallback(Consumer<Boolean> onComplete, boolean ok) {
        if (onComplete == null) {
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                onComplete.accept(ok);
            }
        }.runTask(plugin);
    }
}
