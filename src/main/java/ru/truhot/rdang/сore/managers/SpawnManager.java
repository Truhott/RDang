package ru.truhot.rdang.сore.managers;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import ru.truhot.rdang.config.ConfigManager;
import java.util.Random;

@Getter
public class SpawnManager {
    private int minX = -2000, maxX = 2000, minZ = -2000, maxZ = 2000;
    private ConfigManager configManager;

    public void load(ConfigurationSection section, ConfigManager configManager) {
        this.configManager = configManager;
        if (section == null) return;
        minX = section.getInt("minx", -2000);
        maxX = section.getInt("maxx", 2000);
        minZ = section.getInt("minz", -2000);
        maxZ = section.getInt("maxz", 2000);
    }

    public Location findDungLocation(World world, Random random) {
        for (int attempt = 0; attempt < 100; attempt++) {
            int x = random.nextInt(maxX - minX + 1) + minX;
            int z = random.nextInt(maxZ - minZ + 1) + minZ;
            int y = getSuitableHeight(world, x, z, random);
            if (y == Integer.MIN_VALUE) continue;
            Location loc = new Location(world, x, y, z);
            if (isLocationSafe(loc) && hasSuitableBiome(loc)) {
                return loc;
            }
        }
        return null;
    }

    public int findSurfaceY(World world, int x, int z) {
        if (configManager == null || configManager.getHeightManager() == null) {
            return world.getHighestBlockYAt(x, z);
        }
        WorldHeightManager.WorldHeightConfig hConfig = configManager.getHeightManager().getWorldHeightConfig(world);
        Environment env = world.getEnvironment();
        if (env == Environment.NETHER) {
            return getNetherHeight(world, x, z, hConfig);
        }
        if (env == Environment.THE_END) {
            return getEndHeight(world, x, z, hConfig);
        }
        int y = getSurfaceHeight(world, x, z, hConfig);
        return y != Integer.MIN_VALUE ? y : world.getHighestBlockYAt(x, z);
    }

    public int getSuitableHeight(World world, int x, int z, Random random) {
        if (configManager == null || configManager.getHeightManager() == null)
            return world.getHighestBlockYAt(x, z);
        WorldHeightManager.WorldHeightConfig hConfig = configManager.getHeightManager().getWorldHeightConfig(world);
        if (!hConfig.isUseDefaultAlgorithm()) {
            int minY = Math.max(hConfig.getMinY(), world.getMinHeight());
            int maxY = Math.min(hConfig.getMaxY(), world.getMaxHeight());
            return minY >= maxY ? minY : random.nextInt(maxY - minY + 1) + minY;
        }
        Environment env = world.getEnvironment();
        if (env == Environment.NETHER) return getNetherHeight(world, x, z, hConfig);
        if (env == Environment.THE_END) return getEndHeight(world, x, z, hConfig);
        return getSurfaceHeight(world, x, z, hConfig);
    }

    private int getSurfaceHeight(World world, int x, int z, WorldHeightManager.WorldHeightConfig config) {
        int minY = Math.max(config.getMinY(), world.getMinHeight());
        int maxY = Math.min(config.getMaxY(), world.getMaxHeight());
        for (int y = maxY; y >= minY; y--) {
            if (isSolidGround(world.getBlockAt(x, y - 1, z).getType()) && isReplaceable(world.getBlockAt(x, y, z).getType())) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    private int getNetherHeight(World world, int x, int z, WorldHeightManager.WorldHeightConfig config) {
        int minY = Math.max(config.getMinY(), world.getMinHeight());
        int maxY = Math.min(config.getMaxY(), world.getMaxHeight());
        for (int y = minY; y <= maxY; y++) {
            if (isSolidGround(world.getBlockAt(x, y - 1, z).getType()) && isReplaceable(world.getBlockAt(x, y, z).getType()) && isReplaceable(world.getBlockAt(x, y + 1, z).getType())) return y;
        }
        return (minY + maxY) / 2;
    }

    private int getEndHeight(World world, int x, int z, WorldHeightManager.WorldHeightConfig config) {
        int minY = Math.max(config.getMinY(), world.getMinHeight());
        int maxY = Math.min(config.getMaxY(), world.getMaxHeight());
        for (int y = maxY; y >= minY; y--) {
            Material type = world.getBlockAt(x, y, z).getType();
            if ((type == Material.END_STONE || type == Material.OBSIDIAN) && isReplaceable(world.getBlockAt(x, y + 1, z).getType())) return y + 1;
        }
        return (minY + maxY) / 2;
    }

    private boolean hasSuitableBiome(Location location) {
        if (configManager == null || configManager.getDangManager() == null) return true;
        var dangs = configManager.getDangManager().getDangs();
        if (dangs.isEmpty()) return true;
        Biome locationBiome = location.getWorld().getBiome(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        for (var dangData : dangs) {
            if (!dangData.getWorld().equalsIgnoreCase(location.getWorld().getName())) continue;
            for (Biome allowedBiome : dangData.getBiome()) {
                if (allowedBiome == locationBiome) return true;
            }
        }
        return false;
    }

    private boolean isSolidGround(Material m) {
        if (m.isAir() || m == Material.WATER || m == Material.LAVA || m == Material.CAVE_AIR) return false;
        String n = m.name();
        if (n.endsWith("_LEAVES") || n.contains("LOG") || n.contains("WOOD") || n.contains("FUNGUS") || n.contains("MUSHROOM") || n.contains("GRASS") || n.contains("VINE") || n.contains("CORAL")) return false;
        return m.isSolid() && m.isBlock();
    }

    private boolean isReplaceable(Material m) {
        if (m.isAir()) return true;
        String n = m.name();
        return n.endsWith("_LEAVES") || n.contains("GRASS") || n.contains("FERN") || m == Material.SNOW || !m.isSolid();
    }

    private boolean isLocationSafe(Location loc) {
        if (loc.getY() <= loc.getWorld().getMinHeight() || loc.getY() >= loc.getWorld().getMaxHeight()) return false;
        if (!isSolidGround(loc.clone().subtract(0, 1, 0).getBlock().getType())) return false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Material m = loc.clone().add(dx, dy, dz).getBlock().getType();
                    if (m == Material.LAVA || m == Material.WATER) return false;
                }
            }
        }
        return isReplaceable(loc.getBlock().getType());
    }
}