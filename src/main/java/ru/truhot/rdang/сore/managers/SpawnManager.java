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

    private int minX = -2000;
    private int maxX = 2000;
    private int minZ = -2000;
    private int maxZ = 2000;
    private ConfigManager configManager;

    public void load(ConfigurationSection section, ConfigManager configManager) {
        this.configManager = configManager;

        if (section == null) {
            setDefaultValues();
            return;
        }

        minX = section.getInt("minx", -2000);
        maxX = section.getInt("maxx", 2000);
        minZ = section.getInt("minz", -2000);
        maxZ = section.getInt("maxz", 2000);
    }

    private void setDefaultValues() {
        minX = -2000;
        maxX = 2000;
        minZ = -2000;
        maxZ = 2000;
    }

    public Location getRandomSafeLocation(World world, Random random) {
        if (configManager == null) {
            throw new IllegalStateException("ConfigManager не инициализирован. Вызовите load() с ConfigManager.");
        }

        for (int i = 0; i < 50; i++) {
            int x = random.nextInt(maxX - minX + 1) + minX;
            int z = random.nextInt(maxZ - minZ + 1) + minZ;
            int y = getSuitableHeight(world, x, z, random);

            if (y == Integer.MIN_VALUE) {
                continue;
            }

            Location location = new Location(world, x, y, z);

            if (isLocationSafe(location)) {
                return location;
            }
        }
        return null;
    }

    public int getSuitableHeight(World world, int x, int z, Random random) {
        if (configManager == null || configManager.getWorldHeightManager() == null) {
            return getSuitableHeightLegacy(world, x, z);
        }
        WorldHeightManager.WorldHeightConfig heightConfig =
                configManager.getWorldHeightManager().getHeightConfigForWorld(world);
        if (!heightConfig.isUseDefaultAlgorithm()) {
            int minY = Math.max(heightConfig.getMinY(), world.getMinHeight());
            int maxY = Math.min(heightConfig.getMaxY(), world.getMaxHeight());

            if (minY >= maxY) {
                return minY;
            }
            return random.nextInt(maxY - minY + 1) + minY;
        }
        Environment env = world.getEnvironment();
        switch (env) {
            case NORMAL:
                return getSurfaceHeight(world, x, z, heightConfig);
            case NETHER:
                return getNetherHeight(world, x, z, heightConfig);
            case THE_END:
                return getEndHeight(world, x, z, heightConfig);
            default:
                return getSurfaceHeight(world, x, z, heightConfig);
        }
    }

    private int getSuitableHeightLegacy(World world, int x, int z) {
        Environment env = world.getEnvironment();
        switch (env) {
            case NORMAL:
                return getSurfaceHeightLegacy(world, x, z);
            case NETHER:
                return getNetherHeightLegacy(world, x, z);
            case THE_END:
                return getEndHeightLegacy(world, x, z);
            default:
                return getSurfaceHeightLegacy(world, x, z);
        }
    }

    private int getSurfaceHeight(World world, int x, int z, WorldHeightManager.WorldHeightConfig config) {
        int minY = Math.max(config.getMinY(), world.getMinHeight());
        int maxY = Math.min(config.getMaxY(), world.getMaxHeight());
        for (int y = maxY; y >= minY; y--) {
            Material type = world.getBlockAt(x, y, z).getType();
            Material typeBelow = world.getBlockAt(x, y - 1, z).getType();
            if (isSolidGround(typeBelow) && isAirOrReplaceable(type)) {
                return y;
            }
        }
        for (int y = maxY; y >= minY; y--) {
            Material type = world.getBlockAt(x, y, z).getType();
            Material typeBelow = world.getBlockAt(x, y - 1, z).getType();
            if (isSolidGround(typeBelow) && type.isAir()) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    private int getSurfaceHeightLegacy(World world, int x, int z) {
        int y = world.getHighestBlockYAt(x, z);
        while (y > world.getMinHeight()) {
            Material type = world.getBlockAt(x, y, z).getType();
            Material typeBelow = world.getBlockAt(x, y - 1, z).getType();
            if (isSolidGround(typeBelow) && isAirOrReplaceable(type)) {
                return y;
            }
            y--;
        }

        return Integer.MIN_VALUE;
    }

    private int getNetherHeight(World world, int x, int z, WorldHeightManager.WorldHeightConfig config) {
        int minY = Math.max(config.getMinY(), world.getMinHeight());
        int maxY = Math.min(config.getMaxY(), world.getMaxHeight());
        for (int y = minY; y <= maxY; y++) {
            Material type = world.getBlockAt(x, y, z).getType();
            Material typeAbove = world.getBlockAt(x, y + 1, z).getType();
            Material typeBelow = world.getBlockAt(x, y - 1, z).getType();
            if (isSolidGround(typeBelow) &&
                    isAirOrReplaceable(type) &&
                    isAirOrReplaceable(typeAbove)) {
                return y;
            }
        }
        for (int y = minY; y <= maxY; y++) {
            Material type = world.getBlockAt(x, y, z).getType();
            Material typeBelow = world.getBlockAt(x, y - 1, z).getType();
            if (isSolidGround(typeBelow) && type.isAir()) {
                return y;
            }
        }
        return (minY + maxY) / 2;
    }

    private int getNetherHeightLegacy(World world, int x, int z) {
        for (int y = 32; y <= 100; y++) {
            Material type = world.getBlockAt(x, y, z).getType();
            Material typeAbove = world.getBlockAt(x, y + 1, z).getType();
            Material typeBelow = world.getBlockAt(x, y - 1, z).getType();
            if (isSolidGround(typeBelow) &&
                    isAirOrReplaceable(type) &&
                    isAirOrReplaceable(typeAbove)) {
                return y;
            }
        }
        return 64;
    }

    private int getEndHeight(World world, int x, int z, WorldHeightManager.WorldHeightConfig config) {
        int minY = Math.max(config.getMinY(), world.getMinHeight());
        int maxY = Math.min(config.getMaxY(), world.getMaxHeight());
        for (int y = maxY; y >= minY; y--) {
            Material type = world.getBlockAt(x, y, z).getType();
            Material typeAbove = world.getBlockAt(x, y + 1, z).getType();
            if ((type == Material.END_STONE || type == Material.OBSIDIAN) &&
                    isAirOrReplaceable(typeAbove)) {
                return y + 1;
            }
        }
        for (int y = maxY; y >= minY; y--) {
            Material type = world.getBlockAt(x, y, z).getType();
            Material typeBelow = world.getBlockAt(x, y - 1, z).getType();
            if (isSolidGround(typeBelow) && type.isAir()) {
                return y;
            }
        }
        return (minY + maxY) / 2;
    }

    private int getEndHeightLegacy(World world, int x, int z) {
        int mainHeight = 65;
        for (int y = mainHeight; y >= world.getMinHeight(); y--) {
            Material type = world.getBlockAt(x, y, z).getType();
            Material typeAbove = world.getBlockAt(x, y + 1, z).getType();
            if ((type == Material.END_STONE || type == Material.OBSIDIAN) &&
                    isAirOrReplaceable(typeAbove)) {
                return y + 1;
            }
        }
        return mainHeight + 1;
    }

    private boolean isSolidGround(Material material) {
        if (material.isAir() ||
                material == Material.WATER ||
                material == Material.LAVA ||
                material == Material.CAVE_AIR ||
                material == Material.VOID_AIR) {
            return false;
        }
        String materialName = material.name().toUpperCase();
        boolean isVegetationOrDecor =
                materialName.endsWith("_LEAVES") ||
                        materialName.endsWith("_LOG") ||
                        materialName.endsWith("_WOOD") ||
                        materialName.endsWith("_PLANKS") ||
                        materialName.endsWith("_SAPLING") ||
                        materialName.endsWith("_FUNGUS") ||
                        materialName.contains("MUSHROOM") ||
                        materialName.contains("VINE") ||
                        materialName.contains("CORAL") ||
                        materialName.contains("KELP") ||
                        materialName.contains("SEAGRASS") ||
                        material == Material.GRASS ||
                        material == Material.TALL_GRASS ||
                        material == Material.FERN ||
                        material == Material.LARGE_FERN ||
                        material == Material.DEAD_BUSH ||
                        material == Material.SUGAR_CANE ||
                        material == Material.BAMBOO ||
                        material == Material.SWEET_BERRY_BUSH;
        if (isVegetationOrDecor) {
            return false;
        }
        return material.isSolid() &&
                material.isBlock() &&
                !material.isTransparent() &&
                material.getBlastResistance() > 0;
    }

    private boolean isAirOrReplaceable(Material material) {
        if (material.isAir()) {
            return true;
        }
        String materialName = material.name().toUpperCase();
        return materialName.endsWith("_LEAVES") ||
                material == Material.TALL_GRASS ||
                material == Material.GRASS ||
                material == Material.FERN ||
                material == Material.LARGE_FERN ||
                material == Material.DEAD_BUSH ||
                material == Material.SUGAR_CANE ||
                material == Material.BAMBOO ||
                material == Material.SNOW ||
                material == Material.VINE ||
                !material.isSolid();
    }

    private boolean isLocationSafe(Location location) {
        if (location.getY() <= location.getWorld().getMinHeight() ||
                location.getY() >= location.getWorld().getMaxHeight()) {
            return false;
        }
        Material type = location.getBlock().getType();
        Material typeBelow = location.clone().subtract(0, 1, 0).getBlock().getType();
        boolean isSpawnPointSafe = type.isAir() ||
                type == Material.CAVE_AIR ||
                type == Material.VOID_AIR ||
                isAirOrReplaceable(type);
        boolean isGroundSolid = isSolidGround(typeBelow);
        boolean noLiquidAbove = true;
        boolean noLavaNearby = true;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    Material nearby = location.clone().add(dx, dy, dz).getBlock().getType();
                    if (nearby == Material.LAVA || nearby == Material.WATER) {
                        noLavaNearby = false;
                        break;
                    }
                }
            }
        }
        return isSpawnPointSafe &&
                isGroundSolid &&
                noLiquidAbove &&
                noLavaNearby;
    }

    public boolean isWithinBounds(int x, int z) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    public Location getCenter(World world) {
        int centerX = (minX + maxX) / 2;
        int centerZ = (minZ + maxZ) / 2;
        int y = world.getHighestBlockYAt(centerX, centerZ);
        return new Location(world, centerX, y, centerZ);
    }

    public Location getRandomPoint(World world, Random random) {
        int x = random.nextInt(maxX - minX + 1) + minX;
        int z = random.nextInt(maxZ - minZ + 1) + minZ;
        int y = world.getHighestBlockYAt(x, z);
        return new Location(world, x, y, z);
    }
}