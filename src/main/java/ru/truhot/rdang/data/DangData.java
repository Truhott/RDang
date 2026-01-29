package ru.truhot.rdang.data;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.bukkit.block.Biome;

import java.util.List;

@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class DangData {

    private final String fileName;
    private final String world;
    private final List<Biome> biome;

}
