package ru.truhot.rdang.сore.managers;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
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

    public Location getRandomSafeLocation(World world, Random random) {
        if (configManager == null) {
            System.out.println("[Rdang] ERROR: SpawnManager not initialized!");
            return null;
        }
        for (int i = 0; i < 50; i++) {
            int x = random.nextInt(maxX - minX + 1) + minX;
            int z = random.nextInt(maxZ - minZ + 1) + minZ;
            int y = getSuitableHeight(world, x, z, random);
            if (y == Integer.MIN_VALUE) continue;
            Location loc = new Location(world, x, y, z);
            if (isLocationSafe(loc)) return loc;
        }
        return null;
    }

    public int getSuitableHeight(World world, int x, int z, Random random) {
        if (configManager == null || configManager.getWorldHeightManager() == null) return world.getHighestBlockYAt(x, z);
        WorldHeightManager.WorldHeightConfig hConfig = configManager.getWorldHeightManager().getHeightConfigForWorld(world);

        if (!hConfig.isUseDefaultAlgorithm()) {
            int minY = Math.max(hConfig.getMinY(), world.getMinHeight());
            int maxY = Math.min(hConfig.getMaxY(), world.getMaxHeight());
            return minY >= maxY ? minY : random.nextInt(maxY - minY + 1) + minY;
        }

        Environment env = world.getEnvironment();
        if (env == Environment.NETHER) return getNetherHeight(world, x, z, hConfig);
        if (env == Environment.THE_END) return getEndHeight(world, x, z, hConfig);

        int y = world.getHighestBlockYAt(x, z);
        return isSolidGround(world.getBlockAt(x, y - 1, z).getType()) ? y : Integer.MIN_VALUE;
    }

    private int getNetherHeight(World world, int x, int z, WorldHeightManager.WorldHeightConfig config) {
        int minY = Math.max(config.getMinY(), world.getMinHeight());
        int maxY = Math.min(config.getMaxY(), world.getMaxHeight());
        for (int y = minY; y <= maxY; y++) {
            if (isSolidGround(world.getBlockAt(x, y - 1, z).getType()) && isAirOrReplaceable(world.getBlockAt(x, y, z).getType()) && isAirOrReplaceable(world.getBlockAt(x, y + 1, z).getType())) return y;
        }
        return (minY + maxY) / 2;
    }

    private int getEndHeight(World world, int x, int z, WorldHeightManager.WorldHeightConfig config) {
        int minY = Math.max(config.getMinY(), world.getMinHeight());
        int maxY = Math.min(config.getMaxY(), world.getMaxHeight());
        for (int y = maxY; y >= minY; y--) {
            Material type = world.getBlockAt(x, y, z).getType();
            if ((type == Material.END_STONE || type == Material.OBSIDIAN) && isAirOrReplaceable(world.getBlockAt(x, y + 1, z).getType())) return y + 1;
        }
        return (minY + maxY) / 2;
    }

    private boolean isSolidGround(Material m) {
        if (m.isAir() || m == Material.WATER || m == Material.LAVA || m == Material.CAVE_AIR) return false;
        String n = m.name();
        if (n.endsWith("_LEAVES") || n.contains("LOG") || n.contains("WOOD") || n.contains("FUNGUS") || n.contains("MUSHROOM") || n.contains("GRASS") || n.contains("VINE") || n.contains("CORAL")) return false;
        return m.isSolid() && m.isBlock() && !m.isTransparent();
    }

    private boolean isAirOrReplaceable(Material m) {
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
        return isAirOrReplaceable(loc.getBlock().getType());
    }

    public boolean isWithinBounds(int x, int z) { return x >= minX && x <= maxX && z >= minZ && z <= maxZ; }

    public Location getCenter(World world) {
        int cx = (minX + maxX) / 2, cz = (minZ + maxZ) / 2;
        return new Location(world, cx, world.getHighestBlockYAt(cx, cz), cz);
    }

    public Location getRandomPoint(World world, Random random) {
        int x = random.nextInt(maxX - minX + 1) + minX, z = random.nextInt(maxZ - minZ + 1) + minZ;
        return new Location(world, x, world.getHighestBlockYAt(x, z), z);
    }
}