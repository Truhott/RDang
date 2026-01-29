package ru.truhot.rdang.util;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import ru.truhot.rdang.config.ConfigManager;

public class TeleportUtil {

    private final ConfigManager configManager;

    public TeleportUtil(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public boolean teleportToDungeon(Player player, String regionId) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        for (World world : Bukkit.getWorlds()) {
            RegionManager manager = container.get(BukkitAdapter.adapt(world));
            if (manager != null && manager.hasRegion(regionId)) {
                ProtectedRegion region = manager.getRegion(regionId);
                if (region != null) {
                    int x = (region.getMinimumPoint().getBlockX() + region.getMaximumPoint().getBlockX()) / 2;
                    int z = (region.getMinimumPoint().getBlockZ() + region.getMaximumPoint().getBlockZ()) / 2;
                    int y = world.getHighestBlockYAt(x, z) + 1;
                    Location loc = new Location(world, x + 0.5, y, z + 0.5);
                    player.teleport(loc);
                    String regionNameFormat = configManager.getRegion().getString("region.name_format", "dang_{id}");
                    String prefix = regionNameFormat.replace("{id}", "");
                    String dungeonNumber = regionId.startsWith(prefix) ? regionId.substring(prefix.length()) : "?";
                    String message = configManager.getMessages().getString("messages.list.teleported")
                            .replace("{id}", dungeonNumber);
                    player.sendMessage(MessageUtil.colorize(message));

                    return true;
                }
            }
        }

        String errorMessage = configManager.getMessages().getString("messages.list.region-not-found");
        player.sendMessage(MessageUtil.colorize(errorMessage));

        return false;
    }
}