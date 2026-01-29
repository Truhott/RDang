package ru.truhot.rdang.сore.managers;

import lombok.Getter;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

@Getter
public class WorldHeightManager {

    private final Map<String, WorldHeightConfig> worldHeights = new HashMap<>();
    private WorldHeightConfig defaultNormal;
    private WorldHeightConfig defaultNether;
    private WorldHeightConfig defaultEnd;

    public void load(ConfigurationSection section) {
        worldHeights.clear();
        if (section == null) {
            loadDefaults();
            return;
        }
        ConfigurationSection normalSection = section.getConfigurationSection("normal");
        if (normalSection != null) {
            defaultNormal = loadWorldHeightConfig(normalSection);
        } else {
            defaultNormal = new WorldHeightConfig("normal", 60, 256);
        }
        ConfigurationSection netherSection = section.getConfigurationSection("nether");
        if (netherSection != null) {
            defaultNether = loadWorldHeightConfig(netherSection);
        } else {
            defaultNether = new WorldHeightConfig("nether", 32, 100);
        }
        ConfigurationSection endSection = section.getConfigurationSection("end");
        if (endSection != null) {
            defaultEnd = loadWorldHeightConfig(endSection);
        } else {
            defaultEnd = new WorldHeightConfig("the_end", 40, 80);
        }
        ConfigurationSection customWorldsSection = section.getConfigurationSection("custom-worlds");
        if (customWorldsSection != null) {
            for (String worldName : customWorldsSection.getKeys(false)) {
                ConfigurationSection worldSection = customWorldsSection.getConfigurationSection(worldName);
                if (worldSection != null) {
                    WorldHeightConfig config = loadWorldHeightConfig(worldSection);
                    config.setWorldName(worldName);
                    worldHeights.put(worldName.toLowerCase(), config);
                }
            }
        }
    }

    private void loadDefaults() {
        defaultNormal = new WorldHeightConfig("normal", 60, 256);
        defaultNether = new WorldHeightConfig("nether", 32, 100);
        defaultEnd = new WorldHeightConfig("the_end", 40, 80);
    }

    private WorldHeightConfig loadWorldHeightConfig(ConfigurationSection section) {
        String type = section.getString("type", "normal");
        int minY = section.getInt("min", getDefaultMinForType(type));
        int maxY = section.getInt("max", getDefaultMaxForType(type));
        boolean useDefaultAlgorithm = section.getBoolean("use-default-algorithm", true);
        return new WorldHeightConfig(type, minY, maxY, useDefaultAlgorithm);
    }

    private int getDefaultMinForType(String type) {
        return switch (type.toLowerCase()) {
            case "nether" -> 32;
            case "the_end", "end" -> 40;
            default -> 60;
        };
    }

    private int getDefaultMaxForType(String type) {
        return switch (type.toLowerCase()) {
            case "nether" -> 100;
            case "the_end", "end" -> 80;
            default -> 256;
        };
    }

    public WorldHeightConfig getHeightConfigForWorld(World world) {
        String worldName = world.getName().toLowerCase();
        if (worldHeights.containsKey(worldName)) {
            return worldHeights.get(worldName);
        }
        return switch (world.getEnvironment()) {
            case NETHER -> defaultNether;
            case THE_END -> defaultEnd;
            default -> defaultNormal;
        };
    }

    public int getMinYForWorld(World world) {
        return getHeightConfigForWorld(world).getMinY();
    }

    public int getMaxYForWorld(World world) {
        return getHeightConfigForWorld(world).getMaxY();
    }

    public boolean shouldUseDefaultAlgorithm(World world) {
        return getHeightConfigForWorld(world).isUseDefaultAlgorithm();
    }

    @Getter
    public static class WorldHeightConfig {
        private final String worldType;
        private String worldName;
        private final int minY;
        private final int maxY;
        private final boolean useDefaultAlgorithm;

        public WorldHeightConfig(String worldType, int minY, int maxY) {
            this(worldType, minY, maxY, true);
        }

        public WorldHeightConfig(String worldType, int minY, int maxY, boolean useDefaultAlgorithm) {
            this.worldType = worldType;
            this.minY = minY;
            this.maxY = maxY;
            this.useDefaultAlgorithm = useDefaultAlgorithm;
        }

        public void setWorldName(String worldName) {
            this.worldName = worldName;
        }

        public String getDisplayName() {
            return worldName != null ? worldName : worldType;
        }
    }
}
