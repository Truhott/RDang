package ru.truhot.rdang.comands.impl;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Biome;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.dung.DungActions;
import ru.truhot.rdang.util.MessageUtil;
import ru.truhot.rdang.сore.managers.WorldHeightManager;

import java.util.Random;

public class SpawnCommand implements CommandExecutor {
    private final DungActions dungActions;
    private final ConfigManager configManager;
    private final Random random = new Random();

    public SpawnCommand(DungActions dungActions, ConfigManager configManager) {
        this.dungActions = dungActions;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.colorize(getMessage("only-player")));
            return true;
        }

        if (args.length != 2) {
            player.sendMessage(MessageUtil.colorize(getMessage("spawn.usage")));
            return true;
        }

        World world = player.getWorld();

        try {
            int amount = Integer.parseInt(args[1]);
            if (amount <= 0) {
                player.sendMessage(MessageUtil.colorize(getMessage("spawn.amount-positive")));
                return true;
            }

            int spawnedCount = 0;

            for (int i = 0; i < amount; i++) {
                Location loc = findSuitableDungLocation(world);

                if (loc != null) {
                    dungActions.spawn(loc);
                    String spawnedMsg = getMessage("spawn.spawned")
                            .replace("{x}", String.valueOf(loc.getBlockX()))
                            .replace("{y}", String.valueOf(loc.getBlockY()))
                            .replace("{z}", String.valueOf(loc.getBlockZ()));
                    player.sendMessage(MessageUtil.colorize(spawnedMsg));
                    spawnedCount++;
                }
            }

            if (spawnedCount == 0) {
                player.sendMessage(MessageUtil.colorize(getMessage("spawn.none-spawned")));
            }

            return true;

        } catch (NumberFormatException e) {
            player.sendMessage(MessageUtil.colorize(getMessage("spawn.invalid-number")));
            return true;
        }
    }

    private String getMessage(String path) {
        return configManager.getMessages().getString("messages." + path, "&cСообщение не найдено: " + path);
    }

    private Location findSuitableDungLocation(World world) {
        int minX = configManager.getSpawnManager().getMinX();
        int maxX = configManager.getSpawnManager().getMaxX();
        int minZ = configManager.getSpawnManager().getMinZ();
        int maxZ = configManager.getSpawnManager().getMaxZ();
        for (int attempt = 0; attempt < 100; attempt++) {
            int x = random.nextInt(maxX - minX + 1) + minX;
            int z = random.nextInt(maxZ - minZ + 1) + minZ;
            int y = getSuitableHeight(world, x, z);
            if (y == Integer.MIN_VALUE) {
                continue;
            }
            Location loc = new Location(world, x, y, z);
            if (isLocationSafe(loc) && hasSuitableBiome(loc)) {
                return loc;
            }
        }
        return null;
    }

    private int getSurfaceHeight(World world, int x, int z) {
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

    private int getNetherHeight(World world, int x, int z) {
        for (int y = 32; y <= 120; y++) {
            Material type = world.getBlockAt(x, y, z).getType();
            Material typeAbove = world.getBlockAt(x, y + 1, z).getType();
            Material typeBelow = world.getBlockAt(x, y - 1, z).getType();
            if (isSolidGround(typeBelow) &&
                    isAirOrReplaceable(type) &&
                    isAirOrReplaceable(typeAbove)) {
                return y;
            }
        }
        int y = world.getHighestBlockYAt(x, z);
        if (y > 120) {
            y = 80;
        }
        return y;
    }

    private int getEndHeight(World world, int x, int z) {
        int mainIslandHeight = 65;
        for (int y = mainIslandHeight; y >= world.getMinHeight(); y--) {
            Material type = world.getBlockAt(x, y, z).getType();
            Material typeAbove = world.getBlockAt(x, y + 1, z).getType();
            if (type != Material.AIR && type != Material.END_STONE && type != Material.OBSIDIAN) {
                continue;
            }
            if ((type == Material.END_STONE || type == Material.OBSIDIAN) &&
                    isAirOrReplaceable(typeAbove)) {
                return y + 1;
            }
        }
        return mainIslandHeight + 1;
    }

    private boolean isSolidGround(Material material) {
        return !material.isAir() &&
                material.isSolid() &&
                material != Material.WATER &&
                material != Material.LAVA &&
                !material.name().toUpperCase().endsWith("_LEAVES") &&
                !material.name().toUpperCase().endsWith("_LOG") &&
                !material.name().toUpperCase().endsWith("_WOOD");
    }

    private boolean isAirOrReplaceable(Material material) {
        return material.isAir() ||
                !material.isSolid() ||
                material.name().toUpperCase().endsWith("_LEAVES") ||
                material == Material.TALL_GRASS ||
                material == Material.GRASS ||
                material == Material.FERN ||
                material == Material.LARGE_FERN;
    }

    private boolean hasSuitableBiome(Location location) {
        var dangs = configManager.getDangManager().getDangs();
        if (dangs.isEmpty()) return true;
        Biome locationBiome = location.getWorld().getBiome(
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        );
        for (var dangData : dangs) {
            if (!dangData.getWorld().equalsIgnoreCase(location.getWorld().getName())) {
                continue;
            }
            for (Biome allowedBiome : dangData.getBiome()) {
                if (allowedBiome == locationBiome) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isLocationSafe(Location loc) {
        Material type = loc.getBlock().getType();
        Material typeBelow = loc.clone().subtract(0, 1, 0).getBlock().getType();
        return type.isAir() &&
                isSolidGround(typeBelow) &&
                type != Material.WATER &&
                type != Material.LAVA &&
                loc.getBlockY() > loc.getWorld().getMinHeight() &&
                loc.getBlockY() < loc.getWorld().getMaxHeight();
    }

    private int getSuitableHeight(World world, int x, int z) {
        WorldHeightManager.WorldHeightConfig heightConfig =
                configManager.getWorldHeightManager().getHeightConfigForWorld(world);
        if (!heightConfig.isUseDefaultAlgorithm()) {
            return random.nextInt(heightConfig.getMaxY() - heightConfig.getMinY() + 1)
                    + heightConfig.getMinY();
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

    private int getSurfaceHeight(World world, int x, int z,
                                 WorldHeightManager.WorldHeightConfig config) {
        int minY = config.getMinY();
        int maxY = config.getMaxY();
        for (int y = maxY; y >= minY; y--) {
            Material type = world.getBlockAt(x, y, z).getType();
            Material typeBelow = world.getBlockAt(x, y - 1, z).getType();
            if (isSolidGround(typeBelow) && isAirOrReplaceable(type)) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    private int getNetherHeight(World world, int x, int z,
                                WorldHeightManager.WorldHeightConfig config) {
        int minY = config.getMinY();
        int maxY = config.getMaxY();
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
        return (minY + maxY) / 2;
    }

    private int getEndHeight(World world, int x, int z,
                             WorldHeightManager.WorldHeightConfig config) {
        int minY = config.getMinY();
        int maxY = config.getMaxY();
        for (int y = maxY; y >= minY; y--) {
            Material type = world.getBlockAt(x, y, z).getType();
            Material typeAbove = world.getBlockAt(x, y + 1, z).getType();
            if ((type == Material.END_STONE || type == Material.OBSIDIAN) &&
                    isAirOrReplaceable(typeAbove)) {
                return y + 1;
            }
        }
        return (minY + maxY) / 2;
    }
}