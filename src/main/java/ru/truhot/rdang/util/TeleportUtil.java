package ru.truhot.rdang.util;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.storage.Storage;

public class TeleportUtil {

    private final ConfigManager configManager;
    private final Storage shulkers;

    public TeleportUtil(ConfigManager configManager) {
        this(configManager, null);
    }

    public TeleportUtil(ConfigManager configManager, Storage shulkers) {
        this.configManager = configManager;
        this.shulkers = shulkers;
    }

    public boolean teleport(Player player, String regionId) {
        Location loc = resolveDungeonLocation(regionId);
        if (loc == null) {
            String errorMessage = configManager.getMessages().getString("messages.list.region_not_found");
            player.sendMessage(MessageUtil.colorize(errorMessage));
            return false;
        }
        player.teleport(loc);
        String regionNameFormat = configManager.getRegion().getString("region.name_format", "dang_{id}");
        String prefix = regionNameFormat.replace("{id}", "");
        String dungeonNumber = regionId.startsWith(prefix) ? regionId.substring(prefix.length()) : "?";
        String message = configManager.getMessages().getString("messages.list.teleport")
                .replace("{id}", dungeonNumber);
        player.sendMessage(MessageUtil.colorize(message));
        return true;
    }

    public String formatCoords(String regionId) {
        Location loc = resolveDungeonLocation(regionId);
        if (loc == null) return "N/A";
        return loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
    }

    public Location resolveDungeonLocationByRegion(String regionId, World world) {
        RegionManager manager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
        if (manager == null || !manager.hasRegion(regionId)) return null;
        ProtectedRegion region = manager.getRegion(regionId);
        if (region == null) return null;
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        int x = (min.getBlockX() + max.getBlockX()) / 2;
        int z = (min.getBlockZ() + max.getBlockZ()) / 2;
        int y = configManager.getSpawnManager().findSurfaceY(world, x, z);
        return new Location(world, x + 0.5, y, z + 0.5);
    }

    public Location resolveDungeonLocation(String regionId) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        for (World world : Bukkit.getWorlds()) {
            RegionManager manager = container.get(BukkitAdapter.adapt(world));
            if (manager == null || !manager.hasRegion(regionId)) continue;
            ProtectedRegion region = manager.getRegion(regionId);
            if (region == null) continue;
            Location shulkerLoc = findShulkerInRegion(world, region);
            if (shulkerLoc != null) {
                return shulkerLoc.clone().add(0.5, 1, 0.5);
            }
            return resolveDungeonLocationByRegion(regionId, world);
        }
        return null;
    }

    private Location findShulkerInRegion(World world, ProtectedRegion region) {
        if (shulkers == null) return null;
        ConfigurationSection locs = shulkers.getConfig().getConfigurationSection("locs");
        if (locs == null) return null;
        for (String key : locs.getKeys(false)) {
            ConfigurationSection entry = locs.getConfigurationSection(key);
            if (entry == null) continue;
            Location loc = entry.getLocation("location");
            if (loc == null || loc.getWorld() == null || !loc.getWorld().equals(world)) continue;
            if (region.contains(BukkitAdapter.asBlockVector(loc))) {
                return loc;
            }
        }
        return null;
    }
}
