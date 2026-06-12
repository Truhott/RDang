package ru.truhot.rdang.util.surface;

import org.bukkit.World;

import java.util.concurrent.ConcurrentHashMap;

public final class ChunkSurfaceCache {
    private static final int MAX_CHUNKS_PER_WORLD = 256;
    private final ConcurrentHashMap<String, ConcurrentHashMap<Long, ChunkSurfaceIndex>> byWorld = new ConcurrentHashMap<>();

    public ChunkSurfaceIndex index(World world, int blockX, int blockZ) {
        return indexChunk(world, blockX >> 4, blockZ >> 4);
    }

    public ChunkSurfaceIndex indexChunk(World world, int chunkX, int chunkZ) {
        String worldName = world.getName();
        ConcurrentHashMap<Long, ChunkSurfaceIndex> chunks = byWorld.computeIfAbsent(worldName, ignored -> new ConcurrentHashMap<>());
        long key = chunkKey(chunkX, chunkZ);

        ChunkSurfaceIndex cached = chunks.get(key);
        if (cached != null) return cached;

        if (chunks.size() >= MAX_CHUNKS_PER_WORLD) {
            chunks.clear();
        }

        ChunkSurfaceIndex fresh = ChunkSurfaceIndex.create(world, chunkX, chunkZ);
        chunks.put(key, fresh);
        return fresh;
    }

    public void clear() {
        byWorld.clear();
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | ((long) chunkZ & 0xFFFFFFFFL);
    }
}

