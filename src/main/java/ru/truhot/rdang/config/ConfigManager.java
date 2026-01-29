package ru.truhot.rdang.config;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.truhot.rdang.сore.managers.*;

import java.io.File;

@Getter
public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration items;
    private FileConfiguration world;
    private FileConfiguration messages;
    private FileConfiguration shulker;
    private FileConfiguration region;
    private FileConfiguration schem;
    private FileConfiguration config;
    private FileConfiguration auto;

    private final ItemManager itemManager;
    private final SpawnManager spawnManager;
    private final MessageManager messageManager;
    private final DangManager dangManager;
    private final WorldHeightManager worldHeightManager;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.itemManager = new ItemManager();
        this.spawnManager = new SpawnManager();
        this.messageManager = new MessageManager();
        this.dangManager = new DangManager();
        this.worldHeightManager = new WorldHeightManager();

        loadAllConfigs();
    }

    public void loadAllConfigs() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        config = loadConfig("config.yml", true);
        items = loadConfig("items.yml", true);
        world = loadConfig("world.yml", true);
        messages = loadConfig("messages.yml", true);
        shulker = loadConfig("shulker.yml", true);
        region = loadConfig("region.yml", true);
        schem = loadConfig("schem.yml", true);
        auto = loadConfig("auto.yml", true);

        itemManager.load(items.getConfigurationSection("items"));
        spawnManager.load(world.getConfigurationSection("spawn"), this);
        messageManager.load(messages.getConfigurationSection("messages"));
        dangManager.load(world.getConfigurationSection("dang"));
        worldHeightManager.load(schem.getConfigurationSection("world-heights"));
    }

    public void reloadAll() {
        loadAllConfigs();
        plugin.getLogger().info("Все конфигурации перезагружены!");
    }


    private FileConfiguration loadConfig(String fileName, boolean saveDefault) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            if (saveDefault) {
                plugin.saveResource(fileName, false);
                plugin.getLogger().info(fileName + " успешно загружен");
            }
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getAuto() {
        return auto;
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getShulker() {
        return shulker;
    }

    public FileConfiguration getRegion() {
        return region;
    }

    public FileConfiguration getSchem() {
        return schem;
    }

    public WorldHeightManager getWorldHeightManager() {
        return worldHeightManager;
    }
}