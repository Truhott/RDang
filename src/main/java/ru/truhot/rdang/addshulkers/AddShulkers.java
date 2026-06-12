package ru.truhot.rdang.addshulkers;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import ru.truhot.rdang.shulker.ShulkerActions;

import java.util.ArrayList;
import java.util.List;

public class AddShulkers {
    private final ShulkerActions actions;

    public AddShulkers(ShulkerActions actions) {
        this.actions = actions;
    }

    public int addShulkers(Location center, int radiusX, int radiusZ, int minY, int maxY) {
        World world = center.getWorld();
        if (world == null) {
            return 0;
        }

        int centerX = center.getBlockX();
        int centerZ = center.getBlockZ();
        int startX = centerX - radiusX;
        int endX = centerX + radiusX;
        int startZ = centerZ - radiusZ;
        int endZ = centerZ + radiusZ;

        int yMin = Math.max(minY, world.getMinHeight());
        int yMax = Math.min(maxY, world.getMaxHeight() - 1);
        if (yMin > yMax) {
            return 0;
        }

        int chunkMinX = startX >> 4;
        int chunkMaxX = endX >> 4;
        int chunkMinZ = startZ >> 4;
        int chunkMaxZ = endZ >> 4;

        List<Location> found = new ArrayList<>(4);

        for (int chunkX = chunkMinX; chunkX <= chunkMaxX; chunkX++) {
            for (int chunkZ = chunkMinZ; chunkZ <= chunkMaxZ; chunkZ++) {
                collectShulkersInChunk(world, chunkX, chunkZ, startX, endX, startZ, endZ, yMin, yMax, found);
            }
        }

        if (found.isEmpty()) {
            return 0;
        }
        return actions.addShulkers(found);
    }

    private static void collectShulkersInChunk(
            World world,
            int chunkX,
            int chunkZ,
            int startX,
            int endX,
            int startZ,
            int endZ,
            int yMin,
            int yMax,
            List<Location> found
    ) {
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            world.loadChunk(chunkX, chunkZ);
        }
        Chunk chunk = world.getChunkAt(chunkX, chunkZ);
        for (BlockState state : chunk.getTileEntities()) {
            if (!(state instanceof ShulkerBox)) {
                continue;
            }
            Location loc = state.getLocation();
            int x = loc.getBlockX();
            int y = loc.getBlockY();
            int z = loc.getBlockZ();
            if (x < startX || x > endX || z < startZ || z > endZ || y < yMin || y > yMax) {
                continue;
            }
            found.add(loc);
        }
    }
}
