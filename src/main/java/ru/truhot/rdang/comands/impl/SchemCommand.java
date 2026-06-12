package ru.truhot.rdang.comands.impl;

import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import lombok.RequiredArgsConstructor;
import ru.truhot.rdang.RDang;
import ru.truhot.rdang.permission.Permissions;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.dung.DungActions;
import ru.truhot.rdang.storage.Storage;
import ru.truhot.rdang.util.MessageUtil;
import ru.truhot.rdang.util.UndoUtil;
import java.io.File;

@RequiredArgsConstructor
public class SchemCommand implements CommandExecutor {
    private final DungActions dungActions;
    private final RDang plugin;
    private final ConfigManager configManager;
    private final Storage shulkers;
    private final UndoUtil undoUtil;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!Permissions.has(sender, Permissions.SCHEM)) {
            sender.sendMessage(MessageUtil.colorize(getMessage("no_permission")));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.colorize(getMessage("only_player")));
            return true;
        }

        if (args.length != 2) {
            player.sendMessage(MessageUtil.colorize(getMessage("schem.usage")));
            return true;
        }

        String schemName = args[1];
        File schemFile = findFile(schemName);

        if (schemFile == null || !schemFile.exists()) {
            player.sendMessage(MessageUtil.colorize(getMessage("schem.not_found").replace("{schem}", schemName)));
            return true;
        }

        String fileNameOnly = schemFile.getName();
        if (!isRegistered(fileNameOnly)) {
            player.sendMessage(MessageUtil.colorize(getMessage("schem.not_registered").replace("{schem}", fileNameOnly)));
            return true;
        }

        Location spawnLocation = player.getLocation().add(player.getLocation().getDirection().multiply(3));
        spawnLocation.setY(player.getWorld().getHighestBlockYAt(spawnLocation.getBlockX(), spawnLocation.getBlockZ()));

        try {
            int freeId = dungActions.getFreeId();
            String regionName = configManager.getRegion().getString("region.name_format", "dang_{id}").replace("{id}", String.valueOf(freeId));

            int rx = configManager.getRegion().getInt("region.size.x", 12);
            int rz = configManager.getRegion().getInt("region.size.z", 12);
            int minY = configManager.getRegion().getInt("region.height.min", -10);
            int maxY = configManager.getRegion().getInt("region.height.max", 10);

            BlockVector3 minPoint = BlockVector3.at(spawnLocation.getBlockX() - rx, minY, spawnLocation.getBlockZ() - rz);
            undoUtil.captureTerrainAndSave(regionName, spawnLocation, minPoint, fileNameOnly, terrainOk -> {
                if (terrainOk == null || !terrainOk) {
                    player.sendMessage(MessageUtil.colorize(getMessage("schem.error").replace("{schem}", fileNameOnly)));
                    return;
                }
                dungActions.getSchemAction().spawnSchem(spawnLocation, fileNameOnly, ok -> {
                    if (!ok) {
                        player.sendMessage(MessageUtil.colorize(getMessage("schem.error").replace("{schem}", fileNameOnly)));
                        return;
                    }
                    dungActions.getAddShulkers().addShulkers(spawnLocation, rx, rz, minY, maxY);
                    dungActions.buildRegion(spawnLocation.getBlockX(), spawnLocation.getBlockZ(), spawnLocation.getWorld(), freeId);
                    player.sendMessage(MessageUtil.colorize(getMessage("schem.success")
                            .replace("{schem}", fileNameOnly)
                            .replace("{x}", String.valueOf(spawnLocation.getBlockX()))
                            .replace("{y}", String.valueOf(spawnLocation.getBlockY()))
                            .replace("{z}", String.valueOf(spawnLocation.getBlockZ()))));
                });
            });
            return true;
        } catch (Exception e) {
            player.sendMessage(MessageUtil.colorize(getMessage("schem.error").replace("{schem}", fileNameOnly)));
            return true;
        }
    }

    private File findFile(String name) {
        File internalFolder = new File(plugin.getDataFolder(), "schem");
        File file = checkFolder(internalFolder, name);
        if (file != null && file.exists()) return file;

        var fawe = Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit");
        if (fawe != null) {
            file = checkFolder(new File(fawe.getDataFolder(), "schematics"), name);
            if (file != null && file.exists()) return file;
        }

        var we = Bukkit.getPluginManager().getPlugin("WorldEdit");
        if (we != null) {
            file = checkFolder(new File(we.getDataFolder(), "schematics"), name);
            if (file != null && file.exists()) return file;
        }

        return null;
    }

    private File checkFolder(File folder, String name) {
        if (!folder.exists()) return null;
        if (name.toLowerCase().endsWith(".schem") || name.toLowerCase().endsWith(".schematic")) return new File(folder, name);

        File f1 = new File(folder, name + ".schem");
        if (f1.exists()) return f1;

        File f2 = new File(folder, name + ".schematic");
        return f2.exists() ? f2 : null;
    }

    private boolean isRegistered(String fileName) {
        return configManager.getDangManager().getDangs().stream().anyMatch(d -> d.getFileName().equalsIgnoreCase(fileName));
    }

    private String getMessage(String path) {
        return configManager.getMessages().getString("messages." + path, "&cСообщение не найдено: " + path);
    }
}