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
import ru.truhot.rdang.util.logger.Logger;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@AllArgsConstructor
@Getter
public class DungActions {
    private final SchemAction schemAction;
    private final AddShulkers addShulkers;
    private final ConfigManager configManager;
    private final UndoUtil undoUtil;
    private final Set<String> reservedRegionIds = ConcurrentHashMap.newKeySet();

    public void spawn(@NotNull Location loc) {
        spawn(loc, null);
    }

    public void spawn(@NotNull Location loc, String excludeSchematic) {
        int minDist = configManager.getRegion().getInt("check.distance-dangs");
        boolean checkOtherRegions = configManager.getRegion().getBoolean("check.check_other_regions");
        if (checkDistance(loc, minDist)) return;
        if (checkOtherRegions && checkInside(loc)) return;
        final World world = loc.getWorld();
        final List<DangData> dangDataList = configManager.getDangManager().getDangs();
        String nameFormat = configManager.getRegion().getString("region.name_format");
        int freeId = getFreeId();
        String regionName = reserveRegionName(nameFormat, freeId);
        if (regionName == null) {
            return;
        }
        Random random = new Random();
        for (int i = 0; i < 30; i++) {
            DangData dangData = dangDataList.get(random.nextInt(dangDataList.size()));
            Biome currentBiome = world.getBiome(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            if (excludeSchematic != null && dangData.getFileName() != null && dangData.getFileName().equalsIgnoreCase(excludeSchematic)) {
                continue;
            }
            if (dangData.getWorld().equals(world.getName()) && dangData.getBiome().contains(currentBiome)) {
                int radiusX = configManager.getRegion().getInt("region.size.x");
                int radiusZ = configManager.getRegion().getInt("region.size.z");
                int minY = configManager.getRegion().getInt("region.height.min");
                BlockVector3 minPoint = BlockVector3.at(loc.getBlockX() - radiusX, minY, loc.getBlockZ() - radiusZ);
                int maxY = configManager.getRegion().getInt("region.height.max");
                DangData selected = dangData;
                undoUtil.captureTerrainAndSave(regionName, loc, minPoint, selected.getFileName(), terrainOk -> {
                    if (terrainOk == null || !terrainOk) {
                        reservedRegionIds.remove(regionName.toLowerCase());
                        Logger.warn("Спавн данжа отменён: территория не сохранилась, region=" + regionName);
                        return;
                    }
                    schemAction.spawnSchem(loc, selected.getFileName(), (ok) -> {
                        reservedRegionIds.remove(regionName.toLowerCase());
                        if (!ok) {
                            Logger.warn("Спавн данжа отменён: схема не вставилась, region=" + regionName + ", file=" + selected.getFileName());
                            return;
                        }
                        addShulkers.addShulkers(loc, radiusX, radiusZ, minY, maxY);
                        buildRegion(loc.getBlockX(), loc.getBlockZ(), world, freeId);
                    });
                });
                return;
            }
        }

        reservedRegionIds.remove(regionName.toLowerCase());
    }

    private String reserveRegionName(String nameFormat, int startId) {
        int id = Math.max(1, startId);
        for (int tries = 0; tries < 10000; tries++) {
            String regionName = nameFormat.replace("{id}", String.valueOf(id));
            String key = regionName.toLowerCase();
            if (reservedRegionIds.add(key)) {
                if (isRegionNameTakenInAnyWorld(regionName)) {
                    reservedRegionIds.remove(key);
                } else {
                    return regionName;
                }
            }
            id++;
        }
        return null;
    }

    private boolean isRegionNameTakenInAnyWorld(String regionName) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        if (container == null) return false;
        for (World world : Bukkit.getWorlds()) {
            RegionManager regionManager = container.get(BukkitAdapter.adapt(world));
            if (regionManager != null && regionManager.hasRegion(regionName)) {
                return true;
            }
        }
        return false;
    }

    public String buildRegion(int x, int z, World worldBukkit, int id) {
        final RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        int radiusX = configManager.getRegion().getInt("region.size.x");
        int radiusZ = configManager.getRegion().getInt("region.size.z");
        int minY = configManager.getRegion().getInt("region.height.min");
        int maxY = configManager.getRegion().getInt("region.height.max");
        String nameFormat = configManager.getRegion().getString("region.name_format");
        String regionName = nameFormat.replace("{id}", String.valueOf(id));
        if (container != null) {
            final RegionManager regionManager = container.get(BukkitAdapter.adapt(worldBukkit));
            if (regionManager != null) {
                final BlockVector3 minPoint = BlockVector3.at(x - radiusX, minY, z - radiusZ);
                final BlockVector3 maxPoint = BlockVector3.at(x + radiusX, maxY, z + radiusZ);
                final ProtectedCuboidRegion region = new ProtectedCuboidRegion(regionName, minPoint, maxPoint);
                applyFlags(region);
                regionManager.addRegion(region);
            }
        }
        return regionName;
    }

    public int getFreeId() {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        if (container == null) return 1;
        String nameFormat = configManager.getRegion().getString("region.name_format");
        int id = 1;
        while (true) {
            String regionName = nameFormat.replace("{id}", String.valueOf(id));
            if (reservedRegionIds.contains(regionName.toLowerCase())) {
                id++;
                continue;
            }
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

    private void applyFlags(ProtectedCuboidRegion region) {
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

    private boolean checkDistance(Location loc, int minDist) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        if (container == null) return false;
        RegionManager manager = container.get(BukkitAdapter.adapt(loc.getWorld()));
        if (manager == null) return false;
        String rawFormat = configManager.getRegion().getString("region.name_format");
        String prefix = rawFormat.split("\\{")[0];
        return manager.getRegions().values().stream()
                .filter(r -> r.getId().startsWith(prefix))
                .anyMatch(r -> {
                    BlockVector3 min = r.getMinimumPoint();
                    BlockVector3 max = r.getMaximumPoint();
                    double centerX = (min.getX() + max.getX()) / 2.0;
                    double centerZ = (min.getZ() + max.getZ()) / 2.0;
                    return Math.hypot(loc.getX() - centerX, loc.getZ() - centerZ) < minDist;
                });
    }

    private boolean checkInside(Location location) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        if (container == null) return false;
        RegionManager manager = container.get(BukkitAdapter.adapt(location.getWorld()));
        if (manager == null) return false;
        String rawFormat = configManager.getRegion().getString("region.name_format");
        String prefix = rawFormat.split("\\{")[0];
        BlockVector3 vector = BukkitAdapter.asBlockVector(location);
        return manager.getApplicableRegions(vector).getRegions().stream()
                .anyMatch(r -> !r.getId().startsWith(prefix));
    }
}