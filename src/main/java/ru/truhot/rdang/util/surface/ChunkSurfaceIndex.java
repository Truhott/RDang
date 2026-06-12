package ru.truhot.rdang.util.surface;

import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.Material;

public final class ChunkSurfaceIndex {
    private final ChunkSnapshot snapshot;
    private final int chunkX;
    private final int chunkZ;

    private ChunkSurfaceIndex(ChunkSnapshot snapshot, int chunkX, int chunkZ) {
        this.snapshot = snapshot;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public static ChunkSurfaceIndex create(World world, int chunkX, int chunkZ) {
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            world.loadChunk(chunkX, chunkZ);
        }
        Chunk chunk = world.getChunkAt(chunkX, chunkZ);
        return new ChunkSurfaceIndex(chunk.getChunkSnapshot(false, false, false), chunkX, chunkZ);
    }

    public int highestY(int blockX, int blockZ) {
        return snapshot.getHighestBlockYAt(localX(blockX), localZ(blockZ));
    }

    public Material blockType(int blockX, int blockY, int blockZ) {
        return snapshot.getBlockType(localX(blockX), blockY, localZ(blockZ));
    }

    public ColumnSample sample(int blockX, int blockZ) {
        int lx = localX(blockX);
        int lz = localZ(blockZ);
        int y = snapshot.getHighestBlockYAt(lx, lz);
        Material top = snapshot.getBlockType(lx, y, lz);
        Biome biome = snapshot.getBiome(lx, lz);
        return new ColumnSample(y, top, biome);
    }

    private int localX(int blockX) {
        return blockX - (chunkX << 4);
    }

    private int localZ(int blockZ) {
        return blockZ - (chunkZ << 4);
    }

    public static final class ColumnSample {
        public final int highestY;
        public final Material top;
        public final Biome biome;

        private ColumnSample(int highestY, Material top, Biome biome) {
            this.highestY = highestY;
            this.top = top;
            this.biome = biome;
        }
    }
}

