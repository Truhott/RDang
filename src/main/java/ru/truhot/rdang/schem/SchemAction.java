package ru.truhot.rdang.schem;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.*;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import lombok.AllArgsConstructor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import ru.truhot.rdang.RDang;
import ru.truhot.rdang.config.ConfigManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

@AllArgsConstructor
public class SchemAction {
    private final RDang plugin;
    private final ConfigManager configManager;

    public void spawnSchemLayered(@NotNull Location location, @NotNull String fileName, Runnable onComplete) {
        File schemFile = new File(plugin.getDataFolder() + "/schem/" + fileName);
        ClipboardFormat format = ClipboardFormats.findByFile(schemFile);
        if (format == null) return;
        new BukkitRunnable() {
            @Override
            public void run() {
                try (FileInputStream fis = new FileInputStream(schemFile);
                     ClipboardReader reader = format.getReader(fis)) {
                    Clipboard clipboard = reader.read();
                    BlockVector3 clipMin = clipboard.getMinimumPoint();
                    BlockVector3 dimensions = clipboard.getDimensions();
                    boolean copyEntities = configManager.getSchem().getBoolean("entities", true);
                    boolean copyBiomes = configManager.getSchem().getBoolean("biomes", false);
                    ConfigurationSection offsetSection = configManager.getSchem().getConfigurationSection("schem-offset");
                    double offX = offsetSection != null ? offsetSection.getDouble("x") : 0;
                    double offY = offsetSection != null ? offsetSection.getDouble("y") : 0;
                    double offZ = offsetSection != null ? offsetSection.getDouble("z") : 0;
                    BlockVector3 targetPos = BlockVector3.at(
                            location.getX() + offX,
                            location.getY() + offY,
                            location.getZ() + offZ
                    );
                    BlockVector3 offset = targetPos.subtract(clipMin);
                    new BukkitRunnable() {
                        int currentY = 0;
                        @Override
                        public void run() {
                            if (currentY >= dimensions.getY()) {
                                if (onComplete != null) onComplete.run();
                                this.cancel();
                                return;
                            }
                            try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(location.getWorld()))) {
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
                                copy.setCopyingEntities(copyEntities);
                                copy.setCopyingBiomes(copyBiomes);
                                Operations.complete(copy);
                            } catch (WorldEditException e) {
                                System.out.println("[RDang] Ошибка WorldEdit при послойном спавне: " + e.getMessage());
                                this.cancel();
                            }
                            currentY++;
                        }
                    }.runTaskTimer(plugin, 1L, 1L);
                } catch (Exception e) {
                    System.out.println("[RDang] Ошибка при чтении схемы для послойного спавна: " + fileName);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public void spawnSchem(@NotNull Location location, @NotNull String fileName) {
        File schemFile = new File(plugin.getDataFolder() + "/schem/" + fileName);
        ClipboardFormat format = ClipboardFormats.findByFile(schemFile);
        if (format == null) return;
        try (ClipboardReader reader = format.getReader(new FileInputStream(schemFile))) {
            final Clipboard clipboard = reader.read();
            boolean ignoreAirBlocks = configManager.getSchem().getBoolean("ignore-air-blocks", true);
            int rotation = configManager.getSchem().getInt("rotation", 0);
            boolean mirror = configManager.getSchem().getBoolean("mirror", false);
            boolean copyEntities = configManager.getSchem().getBoolean("entities", true);
            boolean copyBiomes = configManager.getSchem().getBoolean("biomes", false);
            ConfigurationSection offsetSection = configManager.getSchem().getConfigurationSection("schem-offset");
            double offsetX = offsetSection != null ? offsetSection.getDouble("x") : 0;
            double offsetY = offsetSection != null ? offsetSection.getDouble("y") : 0;
            double offsetZ = offsetSection != null ? offsetSection.getDouble("z") : 0;
            Location adjustedLocation = location.clone().add(offsetX, offsetY, offsetZ);
            final BlockVector3 coordinates = BlockVector3.at(adjustedLocation.getX(), adjustedLocation.getY(), adjustedLocation.getZ());
            try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(adjustedLocation.getWorld()))) {
                ClipboardHolder clipboardHolder = new ClipboardHolder(clipboard);
                if (rotation != 0 || mirror) {
                    AffineTransform transform = new AffineTransform();
                    if (rotation != 0) transform = transform.rotateY(rotation);
                    if (mirror) transform = transform.scale(-1, 1, 1);
                    clipboardHolder.setTransform(transform);
                }
                Operation operation = clipboardHolder.createPaste(editSession)
                        .to(coordinates)
                        .ignoreAirBlocks(ignoreAirBlocks)
                        .copyEntities(copyEntities)
                        .copyBiomes(copyBiomes)
                        .build();
                Operations.complete(operation);
            }
        } catch (IOException | WorldEditException exception) {
            System.out.println("[RDang] Не удалось вставить схему: " + fileName);
        }
    }

    public void createBackup(@NotNull Location location, String regionName) {
        int radiusX = configManager.getRegion().getInt("region.size.x", 12);
        int radiusZ = configManager.getRegion().getInt("region.size.z", 12);
        int minY = configManager.getRegion().getInt("region.height.min", 0);
        int maxY = configManager.getRegion().getInt("region.height.max", 255);
        BlockVector3 min = BlockVector3.at(location.getBlockX() - radiusX, minY, location.getBlockZ() - radiusZ);
        BlockVector3 max = BlockVector3.at(location.getBlockX() + radiusX, maxY, location.getBlockZ() + radiusZ);
        com.sk89q.worldedit.regions.CuboidRegion region = new com.sk89q.worldedit.regions.CuboidRegion(
                BukkitAdapter.adapt(location.getWorld()), min, max);
        File backupFile = new File(plugin.getDataFolder() + "/backups/" + regionName + ".schem");
        if (!backupFile.getParentFile().exists()) {
            backupFile.getParentFile().mkdirs();
        }
        ClipboardFormat format = BuiltInClipboardFormat.SPONGE_SCHEMATIC;
        try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(location.getWorld()))) {
            com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard clipboard = new com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard(region);
            clipboard.setOrigin(region.getMinimumPoint());
            ForwardExtentCopy copy = new ForwardExtentCopy(
                    editSession, region, clipboard, region.getMinimumPoint()
            );
            Operations.complete(copy);
            try (ClipboardWriter writer = format.getWriter(new FileOutputStream(backupFile))) {
                writer.write(clipboard);
            }
        } catch (Exception e) {
            System.out.println("[RDang] Ошибка при создании бекапа для региона: " + regionName);
        }
    }
}