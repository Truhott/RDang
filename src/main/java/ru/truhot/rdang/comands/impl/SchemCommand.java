package ru.truhot.rdang.comands.impl;

import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.truhot.rdang.RDang;
import ru.truhot.rdang.config.ConfigManager;
import ru.truhot.rdang.dung.DungActions;
import ru.truhot.rdang.storage.Storage;
import ru.truhot.rdang.util.MessageUtil;
import ru.truhot.rdang.util.UndoUtil;
import java.io.File;

public class SchemCommand implements CommandExecutor {
    private final DungActions dungActions;
    private final RDang plugin;
    private final ConfigManager configManager;
    private final Storage shulkers;
    private final UndoUtil undoUtil;

    public SchemCommand(DungActions dungActions, RDang plugin, ConfigManager configManager, Storage shulkers, UndoUtil undoUtil) {
        this.dungActions = dungActions;
        this.plugin = plugin;
        this.configManager = configManager;
        this.shulkers = shulkers;
        this.undoUtil = undoUtil;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.colorize(getMessage("only-player")));
            return true;
        }
        if (args.length != 2) {
            player.sendMessage(MessageUtil.colorize(getMessage("schem.usage")));
            return true;
        }
        String schemName = args[1];
        File schemFile = findSchematicFile(schemName);
        if (schemFile == null || !schemFile.exists()) {
            player.sendMessage(MessageUtil.colorize(getMessage("schem.not-found").replace("{schem}", schemName)));
            return true;
        }
        String fileNameOnly = schemFile.getName();
        if (!isSchematicRegistered(fileNameOnly)) {
            player.sendMessage(MessageUtil.colorize(getMessage("schem.not-registered").replace("{schem}", fileNameOnly)));
            return true;
        }
        Location spawnLocation = player.getLocation().add(player.getLocation().getDirection().multiply(3));
        spawnLocation.setY(player.getWorld().getHighestBlockYAt(spawnLocation.getBlockX(), spawnLocation.getBlockZ()));
        try {
            int freeId = dungActions.findFreeRegionId();
            String regionName = configManager.getRegion().getString("region.name_format", "dang_{id}").replace("{id}", String.valueOf(freeId));
            dungActions.getSchemAction().createBackup(spawnLocation, regionName);
            dungActions.getSchemAction().spawnSchem(spawnLocation, fileNameOnly);
            int rx = configManager.getRegion().getInt("region.size.x", 12);
            int rz = configManager.getRegion().getInt("region.size.z", 12);
            int minY = configManager.getRegion().getInt("region.height.min", -10);
            int maxY = configManager.getRegion().getInt("region.height.max", 10);
            dungActions.getAddShulkers().addShulkersInRegion(spawnLocation, rx, rz, minY, maxY);
            dungActions.createRegionWithId(spawnLocation.getBlockX(), spawnLocation.getBlockZ(), spawnLocation.getWorld(), freeId);
            undoUtil.saveDungeonData(regionName, spawnLocation.getWorld(), BlockVector3.at(spawnLocation.getBlockX() - rx, minY, spawnLocation.getBlockZ() - rz));
            player.sendMessage(MessageUtil.colorize(getMessage("schem.success").replace("{schem}", fileNameOnly).replace("{x}", String.valueOf(spawnLocation.getBlockX())).replace("{y}", String.valueOf(spawnLocation.getBlockY())).replace("{z}", String.valueOf(spawnLocation.getBlockZ()))));
            return true;
        } catch (Exception e) {
            player.sendMessage(MessageUtil.colorize(getMessage("schem.error").replace("{schem}", fileNameOnly)));
            e.printStackTrace();
            return true;
        }
    }

    private File findSchematicFile(String name) {
        File folder = new File(plugin.getDataFolder(), "schem");
        if (name.toLowerCase().endsWith(".schem") || name.toLowerCase().endsWith(".schematic")) return new File(folder, name);
        File f1 = new File(folder, name + ".schem");
        return f1.exists() ? f1 : new File(folder, name + ".schematic");
    }

    private boolean isSchematicRegistered(String fileName) {
        return configManager.getDangManager().getDangs().stream().anyMatch(d -> d.getFileName().equalsIgnoreCase(fileName));
    }

    private String getMessage(String path) {
        return configManager.getMessages().getString("messages." + path, "&cСообщение не найдено: " + path);
    }
}