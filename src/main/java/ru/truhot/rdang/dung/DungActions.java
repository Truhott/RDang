package ru.truhot.rdang.dung;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.jetbrains.annotations.NotNull;
import ru.truhot.rdang.addshulkers.AddShulkers;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.data.DangData;
import ru.truhot.rdang.schem.SchemAction;
import ru.truhot.rdang.util.UndoUtil;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;

@AllArgsConstructor
@Getter
public class DungActions {
    private final SchemAction schemAction;
    private final AddShulkers addShulkers;
    private final ConfigManager configManager;
    private final UndoUtil undoUtil;

    public void spawn(@NotNull Location loc) {
        final World world = loc.getWorld();
        final List<DangData> dangDataList = configManager.getDangManager().getDangs();
        int freeId = findFreeRegionId();
        String nameFormat = configManager.getRegion().getString("region.name_format", "dang_{id}");
        String regionName = nameFormat.replace("{id}", String.valueOf(freeId));
        for (int i = 0; i < 20; i++) {
            DangData dangData = dangDataList.get(new Random().nextInt(dangDataList.size()));
            Biome currentBiome = world.getBiome(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            if (dangData.getWorld().equals(world.getName()) && dangData.getBiome().contains(currentBiome)) {
                int radiusX = configManager.getRegion().getInt("region.size.x", 12);
                int radiusZ = configManager.getRegion().getInt("region.size.z", 12);
                int minY = configManager.getRegion().getInt("region.height.min", 0);
                BlockVector3 minPoint = BlockVector3.at(loc.getBlockX() - radiusX, minY, loc.getBlockZ() - radiusZ);
                schemAction.createBackup(loc, regionName);
                undoUtil.saveDungeonData(regionName, world, minPoint);
                schemAction.spawnSchem(loc, dangData.getFileName());
                int maxY = configManager.getRegion().getInt("region.height.max", 255);
                addShulkers.addShulkersInRegion(loc, radiusX, radiusZ, minY, maxY);
                createRegionWithId(loc.getBlockX(), loc.getBlockZ(), world, freeId);
                return;
            }
        }
    }

    public String createRegion(int x, int z, World worldBukkit) {
        int freeId = findFreeRegionId();
        return createRegionWithId(x, z, worldBukkit, freeId);
    }

    public String createRegionWithId(int x, int z, World worldBukkit, int id) {
        final RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        int radiusX = configManager.getRegion().getInt("region.size.x", 12);
        int radiusZ = configManager.getRegion().getInt("region.size.z", 12);
        int minY = configManager.getRegion().getInt("region.height.min", 0);
        int maxY = configManager.getRegion().getInt("region.height.max", 255);
        String nameFormat = configManager.getRegion().getString("region.name_format", "dang_{id}");
        String regionName = nameFormat.replace("{id}", String.valueOf(id));
        if (container != null) {
            final RegionManager regionManager = container.get(BukkitAdapter.adapt(worldBukkit));
            if (regionManager != null) {
                final BlockVector3 minPoint = BlockVector3.at(x - radiusX, minY, z - radiusZ);
                final BlockVector3 maxPoint = BlockVector3.at(x + radiusX, maxY, z + radiusZ);
                final ProtectedCuboidRegion region = new ProtectedCuboidRegion(regionName, minPoint, maxPoint);
                setFlagsFromConfig(region);
                regionManager.addRegion(region);
            }
        }
        return regionName;
    }

    public int findFreeRegionId() {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        if (container == null) return 1;
        String nameFormat = configManager.getRegion().getString("region.name_format", "dang_{id}");
        int id = 1;
        while (true) {
            String regionName = nameFormat.replace("{id}", String.valueOf(id));
            boolean isFree = true;
            for (World world : Bukkit.getWorlds()) {
                RegionManager regionManager = container.get(BukkitAdapter.adapt(world));
                if (regionManager != null && regionManager.hasRegion(regionName)) {
                    isFree = false;
                    break;
                }
            }
            if (isFree) return id;
            id++;
        }
    }

    private void setFlagsFromConfig(ProtectedCuboidRegion region) {
        var flagsSection = configManager.getRegion().getConfigurationSection("region.flags");
        if (flagsSection == null) return;
        for (String flagName : flagsSection.getKeys(false)) {
            String flagValue = flagsSection.getString(flagName, "").toLowerCase();
            if (flagValue.isEmpty()) continue;
            StateFlag.State state = flagValue.equals("allow") ? StateFlag.State.ALLOW : StateFlag.State.DENY;
            try {
                String normalizedFlagName = flagName.toUpperCase().replace("-", "_").replace(" ", "_");
                Field flagField = Flags.class.getField(normalizedFlagName);
                Flag<?> flag = (Flag<?>) flagField.get(null);
                if (flag instanceof StateFlag) {
                    region.setFlag((StateFlag) flag, state);
                }
            } catch (Exception ignored) {}
        }
    }

    private boolean isTooCloseToOtherDang(Location loc, int minDist) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        if (container == null) return false;
        RegionManager manager = container.get(BukkitAdapter.adapt(loc.getWorld()));
        if (manager == null) return false;
        String prefix = configManager.getRegion().getString("region.name_format", "dang_").replace("{id}", "");
        return manager.getRegions().values().stream()
                .filter(r -> r.getId().startsWith(prefix))
                .anyMatch(r -> {
                    BlockVector3 min = r.getMinimumPoint();
                    BlockVector3 max = r.getMaximumPoint();
                    double centerX = (min.getX() + max.getX()) / 2.0;
                    double centerZ = (min.getZ() + max.getZ()) / 2.0;
                    return Math.sqrt(Math.pow(loc.getX() - centerX, 2) + Math.pow(loc.getZ() - centerZ, 2)) < minDist;
                });
    }

    private boolean isInOtherRegion(Location location) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        if (container == null) return false;
        RegionManager manager = container.get(BukkitAdapter.adapt(location.getWorld()));
        if (manager == null) return false;
        String prefix = configManager.getRegion().getString("region.name_format", "dang_").replace("{id}", "");
        BlockVector3 vector = BukkitAdapter.asBlockVector(location);
        return manager.getApplicableRegions(vector).getRegions().stream()
                .anyMatch(r -> !r.getId().startsWith(prefix));
    }
}