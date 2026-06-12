package ru.truhot.rdang.util.surface;

import org.bukkit.World;

public final class ChunkAnchorScanner {
    private ChunkAnchorScanner() {}

    public static int[] findNearest(
            ChunkSurfaceCache cache,
            World world,
            int centerX,
            int centerZ,
            int step,
            int maxBlockRadius,
            ColumnPredicate predicate
    ) {
        if (predicate.test(cache.index(world, centerX, centerZ), centerX, centerZ)) {
            return new int[]{centerX, centerZ};
        }

        int centerChunkX = centerX >> 4;
        int centerChunkZ = centerZ >> 4;
        int maxChunkRadius = (maxBlockRadius >> 4) + 1;

        for (int chunkRadius = 0; chunkRadius <= maxChunkRadius; chunkRadius++) {
            for (int dChunkX = -chunkRadius; dChunkX <= chunkRadius; dChunkX++) {
                for (int dChunkZ = -chunkRadius; dChunkZ <= chunkRadius; dChunkZ++) {
                    if (chunkRadius <= 0 || Math.abs(dChunkX) == chunkRadius || Math.abs(dChunkZ) == chunkRadius) {
                        int chunkX = centerChunkX + dChunkX;
                        int chunkZ = centerChunkZ + dChunkZ;
                        int[] hit = scanChunk(cache, world, chunkX, chunkZ, step, centerX, centerZ, maxBlockRadius, predicate);
                        if (hit != null) return hit;
                    }
                }
            }
        }
        return null;
    }

    private static int[] scanChunk(
            ChunkSurfaceCache cache,
            World world,
            int chunkX,
            int chunkZ,
            int step,
            int centerX,
            int centerZ,
            int maxBlockRadius,
            ColumnPredicate predicate
    ) {
        ChunkSurfaceIndex index = cache.indexChunk(world, chunkX, chunkZ);
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        int gridStep = Math.max(1, step);

        for (int wx = baseX; wx < baseX + 16; wx += gridStep) {
            for (int wz = baseZ; wz < baseZ + 16; wz += gridStep) {
                if (Math.abs(wx - centerX) <= maxBlockRadius
                        && Math.abs(wz - centerZ) <= maxBlockRadius
                        && predicate.test(index, wx, wz)) {
                    return new int[]{wx, wz};
                }
            }
        }
        return null;
    }

    @FunctionalInterface
    public interface ColumnPredicate {
        boolean test(ChunkSurfaceIndex index, int blockX, int blockZ);
    }
}

