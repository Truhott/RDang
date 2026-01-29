package ru.truhot.rdang.сore.managers;

import lombok.Getter;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import ru.truhot.rdang.data.DangData;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class DangManager {

    private final List<DangData> dangs = new ArrayList<>();
    private final Map<String, DangData> dangMap = new HashMap<>();

    public void load(ConfigurationSection section) {
        dangs.clear();
        dangMap.clear();
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            ConfigurationSection dangSection = section.getConfigurationSection(key);
            if (dangSection == null) continue;
            String fileName = dangSection.getString("fileName");
            String world = dangSection.getString("world");
            String biomeString = dangSection.getString("biome", "DESERT");
            if (fileName == null || world == null) {
                continue;
            }
            List<Biome> biomes = parseBiomes(biomeString);
            DangData dangData = new DangData(fileName, world, biomes);
            dangs.add(dangData);
            dangMap.put(key, dangData);
        }
    }

    private List<Biome> parseBiomes(String biomeString) {
        List<Biome> biomes = new ArrayList<>();
        if (biomeString == null || biomeString.isEmpty()) {
            return List.of(Biome.DESERT);
        }
        String[] biomeNames = biomeString.split(";");
        for (String biomeName : biomeNames) {
            String trimmedName = biomeName.trim();
            if (trimmedName.isEmpty()) continue;

            Biome biome = getBiomeFromString(trimmedName);
            if (biome != null) {
                biomes.add(biome);
            }
        }

        return biomes.isEmpty() ? List.of(Biome.DESERT) : biomes;
    }

    private Biome getBiomeFromString(String biomeName) {
        if (biomeName == null || biomeName.isEmpty()) {
            return null;
        }
        try {
            return Biome.valueOf(biomeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            String formattedName = biomeName.toUpperCase().replace(' ', '_');
            try {
                return Biome.valueOf(formattedName);
            } catch (IllegalArgumentException e2) {
                return null;
            }
        }
    }

    public DangData getDangById(String id) {
        return dangMap.get(id);
    }

    public List<DangData> getDangsForWorld(String worldName) {
        List<DangData> result = new ArrayList<>();
        for (DangData dang : dangs) {
            if (dang.getWorld().equalsIgnoreCase(worldName)) {
                result.add(dang);
            }
        }
        return result;
    }
}