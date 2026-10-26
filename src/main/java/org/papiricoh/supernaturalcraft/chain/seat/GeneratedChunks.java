package org.papiricoh.supernaturalcraft.chain.seat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Which chunks of a level have ever been generated: loaded now, or saved in its region files. Only the 4 KiB header of each
 * region file is read (its table of chunk offsets), so no chunk is loaded. One instance answers one search (headers are cached).
 */
final class GeneratedChunks {
    static final int HEADER = 4096;
    /** A header that could not be read: every chunk counts as generated. */
    private static final byte[] UNREADABLE = filled();

    private final ServerLevel level;
    private final Path regions;
    private final Map<Long, byte[]> headers = new HashMap<>();

    GeneratedChunks(ServerLevel level) {
        this.level = level;
        this.regions = DimensionType.getStorageFolder(level.dimension(), level.getServer().getWorldPath(LevelResource.ROOT)).resolve("region");
    }

    /** Whether any chunk touching the block square [minX, maxX] × [minZ, maxZ] was ever generated. */
    boolean any(int minX, int minZ, int maxX, int maxZ) {
        for (int cx = minX >> 4; cx <= maxX >> 4; cx++)
            for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) if (generated(cx, cz)) return true;
        return false;
    }

    boolean generated(int cx, int cz) {
        if (level.getChunkSource().hasChunk(cx, cz)) return true;
        byte[] header = headers.computeIfAbsent(ChunkPos.asLong(cx >> 5, cz >> 5), k -> read(cx >> 5, cz >> 5));
        return saved(header, cx, cz);
    }

    /** Whether a region header (null: no region file) lists the chunk: its 4-byte offset entry is not zero. */
    static boolean saved(byte[] header, int cx, int cz) {
        if (header == null) return false;
        int i = 4 * ((cx & 31) + (cz & 31) * 32);
        return i + 4 <= header.length && (header[i] | header[i + 1] | header[i + 2] | header[i + 3]) != 0;
    }

    private byte[] read(int rx, int rz) {
        try (InputStream in = Files.newInputStream(regions.resolve("r." + rx + "." + rz + ".mca"))) {
            return in.readNBytes(HEADER);
        } catch (NoSuchFileException e) {
            return null;
        } catch (IOException e) {
            return UNREADABLE;
        }
    }

    private static byte[] filled() {
        byte[] b = new byte[HEADER];
        Arrays.fill(b, (byte) 1);
        return b;
    }
}
