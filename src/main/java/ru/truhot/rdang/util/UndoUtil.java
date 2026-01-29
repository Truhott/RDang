package ru.truhot.rdang.util;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
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
import org.bukkit.scheduler.BukkitRunnable;
import ru.truhot.rdang.RDang;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.storage.Storage;

import java.io.File;
import java.io.FileInputStream;
import java.util.Objects;

public class UndoUtil {

    private final ConfigManager configManager;
    private final Storage shulkers;
    private final Storage blockStorage;
    private final RDang plugin;

    public static class UndoResult {
        public final int shulkerCount;
        public final String worldName;
        public final boolean found;

        public UndoResult(int shulkerCount, String worldName, boolean found) {
            this.shulkerCount = shulkerCount;
            this.worldName = worldName;
            this.found = found;
        }
    }

    public UndoUtil(ConfigManager configManager, Storage shulkers, Storage blockStorage, RDang plugin) {
        this.configManager = configManager;
        this.shulkers = shulkers;
        this.blockStorage = blockStorage;
        this.plugin = plugin;
    }

    public void saveDungeonData(String regionName, World world, BlockVector3 minPoint) {
        String path = "history." + regionName;
        blockStorage.getConfig().set(path + ".world", world.getName());
        blockStorage.getConfig().set(path + ".x", minPoint.getX());
        blockStorage.getConfig().set(path + ".y", minPoint.getY());
        blockStorage.getConfig().set(path + ".z", minPoint.getZ());
        blockStorage.save();
    }

    public UndoResult performUndo(String regionName) {
        String path = "history." + regionName;
        ConfigurationSection data = blockStorage.getConfig().getConfigurationSection(path);
        if (data == null) {
            return new UndoResult(0, "Unknown", false);
        }
        String worldName = data.getString("world");
        World world = Bukkit.getWorld(Objects.requireNonNull(worldName));
        if (world == null) return new UndoResult(0, worldName, false);
        BlockVector3 minPoint = BlockVector3.at(data.getInt("x"), data.getInt("y"), data.getInt("z"));
        restoreLandscapeLayered(regionName, world, minPoint);
        int removedCount = 0;
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager manager = container.get(BukkitAdapter.adapt(world));
        if (manager != null && manager.hasRegion(regionName)) {
            ProtectedRegion region = manager.getRegion(regionName);
            removedCount = removeShulkersInRegion(getRegionCenter(region, world), world);
            manager.removeRegion(regionName);
        }
        blockStorage.getConfig().set(path, null);
        blockStorage.save();
        if (removedCount > 0) {
            shulkers.save();
        }
        return new UndoResult(removedCount, worldName, true);
    }

    private void restoreLandscapeLayered(String regionName, World world, BlockVector3 minPoint) {
        File backupFile = new File(plugin.getDataFolder(), "backups/" + regionName + ".schem");
        if (!backupFile.exists()) return;
        ClipboardFormat format = ClipboardFormats.findByFile(backupFile);
        if (format == null) return;
        new BukkitRunnable() {
            @Override
            public void run() {
                try (FileInputStream fis = new FileInputStream(backupFile);
                     ClipboardReader reader = format.getReader(fis)) {
                    Clipboard clipboard = reader.read();
                    BlockVector3 dimensions = clipboard.getDimensions();
                    BlockVector3 clipMin = clipboard.getMinimumPoint();
                    BlockVector3 offset = minPoint.subtract(clipMin);
                    new BukkitRunnable() {
                        int currentY = 0;
                        @Override
                        public void run() {
                            if (currentY >= dimensions.getY()) {
                                if (backupFile.exists()) {
                                    backupFile.delete();
                                }
                                this.cancel();
                                return;
                            }
                            try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(world))) {
                                BlockVector3 layerMin = clipMin.add(0, currentY, 0);
                                BlockVector3 layerMax = BlockVector3.at(
                                        clipboard.getMaximumPoint().getX(),
                                        clipMin.getY() + currentY,
                                        clipboard.getMaximumPoint().getZ()
                                );
                                CuboidRegion layerRegion = new CuboidRegion(layerMin, layerMax);
                                ForwardExtentCopy copy = new ForwardExtentCopy(
                                        clipboard, layerRegion, editSession, layerMin.add(offset)
                                );
                                copy.setCopyingEntities(false);
                                Operations.complete(copy);
                            } catch (Exception ignored) {
                            }

                            currentY++;
                        }
                    }.runTaskTimer(plugin, 1L, 1L);

                } catch (Exception ignored) {
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public void scheduleAutoUndoWithActionBar(String regionName, org.bukkit.World world,
                                              com.sk89q.worldguard.protection.regions.ProtectedRegion region) {
        String timeStr = configManager.getAuto().getString("auto.time");
        long seconds = ru.truhot.rdang.util.TimeUtil.parseTimeString(timeStr);
        new org.bukkit.scheduler.BukkitRunnable() {
            private long timeLeft = seconds;
            @Override
            public void run() {
                if (timeLeft <= 0) {
                    performUndo(regionName);
                    this.cancel();
                    return;
                }
                String rawMsg = configManager.getMessages().getString("messages.actionbar-timer");
                String formattedTime = ru.truhot.rdang.util.TimeUtil.formatTime(timeLeft);
                String finalMsg = ru.truhot.rdang.util.MessageUtil.colorize(
                        rawMsg.replace("{time}", formattedTime)
                );
                for (org.bukkit.entity.Player player : world.getPlayers()) {
                    boolean isInRegion = region.contains(
                            com.sk89q.worldedit.bukkit.BukkitAdapter.asBlockVector(
                                    player.getLocation()
                            )
                    );
                    if (isInRegion) {
                        player.spigot().sendMessage(
                                net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                                new net.md_5.bungee.api.chat.TextComponent(finalMsg)
                        );
                    }
                }
                timeLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private int removeShulkersInRegion(Location center, World world) {
        ConfigurationSection locs = shulkers.getConfig().getConfigurationSection("locs");
        if (locs == null) return 0;
        int radiusX = configManager.getRegion().getInt("region.size.x", 12);
        int radiusZ = configManager.getRegion().getInt("region.size.z", 12);
        return (int) locs.getKeys(false).stream()
                .filter(key -> {
                    Location loc = locs.getLocation(key + ".location");
                    return loc != null &&
                            loc.getWorld().getName().equals(world.getName()) &&
                            Math.abs(loc.getBlockX() - center.getBlockX()) <= radiusX &&
                            Math.abs(loc.getBlockZ() - center.getBlockZ()) <= radiusZ;
                })
                .peek(key -> locs.set(key, null))
                .count();
    }

    private Location getRegionCenter(ProtectedRegion region, World world) {
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        return new Location(world, (min.getX() + max.getX()) / 2.0, (min.getY() + max.getY()) / 2.0, (min.getZ() + max.getZ()) / 2.0);
    }
}