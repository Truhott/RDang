package ru.truhot.rdang.schem;

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
import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.truhot.rdang.RDang;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.util.logger.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.util.function.Consumer;

@AllArgsConstructor
public class SchemAction {
    private final RDang plugin;
    private final ConfigManager configManager;

    public void spawnSchem(@NotNull Location location, @NotNull String fileName) {
        spawnSchem(location, fileName, null);
    }

    public void spawnSchem(@NotNull Location location, @NotNull String fileName, @Nullable Consumer<Boolean> onComplete) {
        File schemFile = new File(plugin.getDataFolder() + "/schem/" + fileName);
        if (!schemFile.exists()) {
            var fawe = Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit");
            if (fawe != null) {
                File faweFolder = new File(fawe.getDataFolder(), "schematics");
                File alternativeFile = new File(faweFolder, fileName);
                if (alternativeFile.exists()) {
                    schemFile = alternativeFile;
                }
            }

            if (!schemFile.exists()) {
                var we = Bukkit.getPluginManager().getPlugin("WorldEdit");
                if (we != null) {
                    File weFolder = new File(we.getDataFolder(), "schematics");
                    File alternativeFile = new File(weFolder, fileName);
                    if (alternativeFile.exists()) {
                        schemFile = alternativeFile;
                    }
                }
            }
        }
        if (!schemFile.exists()) {
            Logger.warn("Схема не найдена: " + fileName);
            runComplete(onComplete, false);
            return;
        }
        final File finalFile = schemFile;
        ClipboardFormat format = ClipboardFormats.findByFile(finalFile);
        if (format == null) {
            Logger.error("Неизвестный формат схемы: " + finalFile.getName());
            runComplete(onComplete, false);
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                try (FileInputStream fis = new FileInputStream(finalFile);
                     ClipboardReader reader = format.getReader(fis)) {
                    Clipboard clipboard = reader.read();
                    boolean ignoreAir = configManager.getSchem().getBoolean("ignore-air-blocks");
                    ConfigurationSection offsetSection = configManager.getSchem().getConfigurationSection("schem-offset");
                    double ox = offsetSection != null ? offsetSection.getDouble("x") : 0;
                    double oy = offsetSection != null ? offsetSection.getDouble("y") : 0;
                    double oz = offsetSection != null ? offsetSection.getDouble("z") : 0;
                    Location targetLoc = location.clone().add(ox, oy, oz);
                    BlockVector3 targetOrigin = BlockVector3.at(targetLoc.getX(), targetLoc.getY(), targetLoc.getZ());
                    BlockVector3 offset = targetOrigin.subtract(clipboard.getOrigin());
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            boolean pasted = false;
                            try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(targetLoc.getWorld()))) {
                                ForwardExtentCopy copy = new ForwardExtentCopy(
                                        clipboard, clipboard.getRegion(), editSession, clipboard.getMinimumPoint().add(offset)
                                );
                                copy.setCopyingEntities(true);

                                if (ignoreAir) {
                                    copy.setSourceMask(com.sk89q.worldedit.function.mask.Masks.negate(
                                            new com.sk89q.worldedit.function.mask.BlockTypeMask(clipboard, com.sk89q.worldedit.world.block.BlockTypes.AIR)
                                    ));
                                }
                                Operations.complete(copy);
                                pasted = true;
                            } catch (Exception e) {
                                Logger.error("Не удалось вставить схему: " + fileName);
                            }
                            runComplete(onComplete, pasted);
                        }
                    }.runTask(plugin);
                } catch (Exception e) {
                    Logger.error("Ошибка при чтении схемы: " + fileName);
                    runComplete(onComplete, false);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private void runComplete(@Nullable Consumer<Boolean> onComplete, boolean ok) {
        if (onComplete == null) return;
        new BukkitRunnable() {
            @Override
            public void run() {
                onComplete.accept(ok);
            }
        }.runTask(plugin);
    }
}
