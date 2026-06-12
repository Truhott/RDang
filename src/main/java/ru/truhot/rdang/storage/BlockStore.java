package ru.truhot.rdang.storage;

import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.truhot.rdang.util.logger.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class BlockStore {

    private static final String JDBC = "jdbc:sqlite:";
    private static final String TABLE = "dungeons";

    private final JavaPlugin plugin;
    private final File dbFile;
    private Connection connection;

    public BlockStore(JavaPlugin plugin) {
        this.plugin = plugin;
        File dir = new File(plugin.getDataFolder(), "data");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        this.dbFile = new File(dir, "block.db");
        open();
        createTable();
        importLegacyYaml();
    }

    public static final class Snapshot {
        public final String regionId;
        public final String world;
        public final int anchorX;
        public final int anchorY;
        public final int anchorZ;
        public final String schematic;
        private final byte[] terrainGzip;

        private Snapshot(String regionId, String world, int anchorX, int anchorY, int anchorZ, String schematic, byte[] terrainGzip) {
            this.regionId = regionId;
            this.world = world;
            this.anchorX = anchorX;
            this.anchorY = anchorY;
            this.anchorZ = anchorZ;
            this.schematic = schematic;
            this.terrainGzip = terrainGzip;
        }

        public BlockVector3 anchor() {
            return BlockVector3.at(anchorX, anchorY, anchorZ);
        }

        public byte[] terrainBytes() throws IOException {
            return gunzip(terrainGzip);
        }
    }

    public synchronized void put(String regionId, String world, BlockVector3 anchor, String schematic, byte[] terrainSchem) throws Exception {
        byte[] gzip = gzip(terrainSchem);
        String sql = "INSERT INTO " + TABLE + " (region_id, world, min_x, min_y, min_z, schematic, terrain) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT(region_id) DO UPDATE SET "
                + "world = excluded.world, min_x = excluded.min_x, min_y = excluded.min_y, min_z = excluded.min_z, "
                + "schematic = excluded.schematic, terrain = excluded.terrain";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, regionId);
            ps.setString(2, world);
            ps.setInt(3, anchor.getX());
            ps.setInt(4, anchor.getY());
            ps.setInt(5, anchor.getZ());
            ps.setString(6, schematic);
            ps.setBytes(7, gzip);
            ps.executeUpdate();
        }
    }

    public synchronized Optional<Snapshot> get(String regionId) {
        String sql = "SELECT world, min_x, min_y, min_z, schematic, terrain FROM " + TABLE + " WHERE region_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, regionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new Snapshot(
                        regionId,
                        rs.getString("world"),
                        rs.getInt("min_x"),
                        rs.getInt("min_y"),
                        rs.getInt("min_z"),
                        rs.getString("schematic"),
                        rs.getBytes("terrain")
                ));
            }
        } catch (SQLException e) {
            Logger.error("block.db: не удалось прочитать " + regionId);
            return Optional.empty();
        }
    }

    public synchronized void remove(String regionId) {
        String sql = "DELETE FROM " + TABLE + " WHERE region_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, regionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            Logger.error("block.db: не удалось удалить " + regionId);
        }
    }

    public synchronized void close() {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException ignored) {
        }
        connection = null;
    }

    private void open() {
        try {
            connection = DriverManager.getConnection(JDBC + dbFile.getAbsolutePath());
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException("block.db: " + e.getMessage(), e);
        }
    }

    private void createTable() {
        String ddl = "CREATE TABLE IF NOT EXISTS " + TABLE + " ("
                + "region_id TEXT PRIMARY KEY NOT NULL, "
                + "world TEXT NOT NULL, "
                + "min_x INTEGER NOT NULL, "
                + "min_y INTEGER NOT NULL, "
                + "min_z INTEGER NOT NULL, "
                + "schematic TEXT, "
                + "terrain BLOB NOT NULL)";
        synchronized (this) {
            try (Statement st = connection.createStatement()) {
                st.executeUpdate(ddl);
            } catch (SQLException e) {
                throw new RuntimeException("block.db: " + e.getMessage(), e);
            }
        }
    }

    private void importLegacyYaml() {
        File legacy = new File(plugin.getDataFolder(), "data/block.yml");
        if (!legacy.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(legacy);
        ConfigurationSection history = yaml.getConfigurationSection("history");
        if (history == null) {
            archiveLegacyYaml(legacy);
            return;
        }
        int count = 0;
        for (String regionId : history.getKeys(false)) {
            ConfigurationSection row = history.getConfigurationSection(regionId);
            if (row == null) {
                continue;
            }
            String encoded = row.getString("terrain");
            if (encoded == null || encoded.isEmpty()) {
                continue;
            }
            try {
                put(
                        regionId,
                        row.getString("world", "world"),
                        BlockVector3.at(row.getInt("x"), row.getInt("y"), row.getInt("z")),
                        row.getString("schem"),
                        Base64.getDecoder().decode(encoded)
                );
                count++;
            } catch (Exception e) {
                Logger.warn("block.db: пропуск миграции " + regionId);
            }
        }
        if (count > 0) {
            Logger.info("block.db: импорт из block.yml — " + count);
        }
        archiveLegacyYaml(legacy);
    }

    private void archiveLegacyYaml(File legacy) {
        File archived = new File(legacy.getParentFile(), "block.yml.bak");
        try {
            Files.move(legacy.toPath(), archived.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Logger.warn("block.db: не удалось архивировать block.yml");
        }
    }

    private static byte[] gzip(byte[] source) throws IOException {
        if (source == null || source.length == 0) {
            return new byte[0];
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(source.length / 4);
        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(source);
        }
        return out.toByteArray();
    }

    private static byte[] gunzip(byte[] source) throws IOException {
        if (source == null || source.length == 0) {
            return new byte[0];
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(source.length * 4);
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(source))) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = gzip.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
        }
        return out.toByteArray();
    }
}
