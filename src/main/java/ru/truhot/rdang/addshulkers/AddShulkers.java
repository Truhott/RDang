package ru.truhot.rdang.addshulkers;

import lombok.AllArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import ru.truhot.rdang.shulker.ShulkerActions;

@AllArgsConstructor
public class AddShulkers {

    private final ShulkerActions actions;

    public void addShulkersInRegion(Location center, int radiusX, int radiusZ, int minY, int maxY) {
        int startX = center.getBlockX() - radiusX;
        int endX = center.getBlockX() + radiusX;
        int startZ = center.getBlockZ() - radiusZ;
        int endZ = center.getBlockZ() + radiusZ;
        for (int x = startX; x <= endX; x++) {
            for (int z = startZ; z <= endZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    Block block = center.getWorld().getBlockAt(x, y, z);
                    if (isShulker(block.getType())) {
                        actions.addShulker(block.getLocation());
                    }
                }
            }
        }
    }

    private boolean isShulker(Material placedBlock) {
        return placedBlock.toString().endsWith("SHULKER_BOX");
    }
}