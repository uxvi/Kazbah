package moe.oko.kazbah;

import moe.oko.kazbah.model.InventorySet;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.slf4j.Logger;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DAO {

    private final Kazbah plugin;
    private final Logger log;
    private volatile List<String> invCatalog = new ArrayList<>();

    public DAO(Kazbah plugin) {
        this.plugin = plugin;
        this.log = plugin.getSLF4JLogger();
        initTables();
    }

    protected CompletableFuture<Boolean> saveInventory(String uuid, String name, InventorySet invSet) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = connect();
                    ByteArrayInputStream inventory = serializeItemArray(invSet.inventory());
                    ByteArrayInputStream armor = serializeItemArray(invSet.armor())) {

                PreparedStatement ps = conn
                        .prepareStatement("REPLACE INTO inventories(uuid, name, si, sa) VALUES (?, ?, ?, ?)");
                ps.setString(1, uuid);
                ps.setString(2, name);
                ps.setBytes(3, inventory.readAllBytes());
                ps.setBytes(4, armor.readAllBytes());
                ps.execute();
                ps.close();

                updateInvCache(conn);
                return true;
            } catch (Exception e) {
                log.error("Unable to save ItemStack[] to DB", e);
            }
            return false;
        });
    }

    protected CompletableFuture<InventorySet> getInventorySet(String request) {
        return CompletableFuture.supplyAsync(() -> getInventory(request));
    }

    protected CompletableFuture<Boolean> removeInventory(String request) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = connect();
                    PreparedStatement ps = conn.prepareStatement("DELETE FROM inventories WHERE name=?")) {
                ps.setString(1, request);
                ps.execute();

                updateInvCache(conn);
                return true;
            } catch (SQLException e) {
                log.error("Unable to remove inventory from DB", e);
            }
            return false;
        });
    }

    protected List<String> getInvCatalog() {
        return invCatalog;
    }

    private InventorySet getInventory(String request) {
        try (Connection conn = connect();
                PreparedStatement ps = conn.prepareStatement("SELECT si, sa FROM inventories WHERE name=?")) {
            ps.setString(1, request);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    byte[] siBytes = rs.getBytes("si");
                    byte[] saBytes = rs.getBytes("sa");
                    return new InventorySet(
                            deserializeItemArray(new ByteArrayInputStream(siBytes)),
                            deserializeItemArray(new ByteArrayInputStream(saBytes)));
                }
            }
        } catch (Exception e) {
            log.error("Error retrieving inventory: " + request, e);
        }
        return null;
    }

    private void updateInvCache(Connection conn) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT name FROM inventories");
                ResultSet rs = ps.executeQuery()) {
            List<String> names = new ArrayList<>();
            while (rs.next()) {
                names.add(rs.getString("name"));
            }
            invCatalog = names;
        } catch (SQLException e) {
            log.error("Unable to get the inventory list.", e);
        }
    }

    private void initTables() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists())
            dataFolder.mkdirs();

        File dbFile = new File(dataFolder, "data.db");
        if (!dbFile.exists()) {
            try {
                dbFile.createNewFile();
            } catch (IOException e) {
                log.error("Unable to create DB file", e);
            }
        }
        String sql = "CREATE TABLE IF NOT EXISTS inventories(uuid TEXT, name TEXT UNIQUE, si BLOB, sa BLOB)";
        try (Connection conn = connect();
                Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            updateInvCache(conn);
        } catch (SQLException e) {
            log.error("Failed to initialize database tables", e);
        }
    }

    private Connection connect() {
        String url = "jdbc:sqlite:" + new File(plugin.getDataFolder(), "data.db").getAbsolutePath();
        try {
            return DriverManager.getConnection(url);
        } catch (SQLException e) {
            log.error("Unable to connect to DB!", e);
            return null;
        }
    }

    private ByteArrayInputStream serializeItemArray(ItemStack[] items) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
            dataOutput.writeInt(items.length);
            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }
            dataOutput.flush();
            return new ByteArrayInputStream(outputStream.toByteArray());
        }
    }

    private ItemStack[] deserializeItemArray(InputStream inputStream) throws IOException, ClassNotFoundException {
        if (inputStream == null || inputStream.available() == 0)
            return new ItemStack[0];
        try (BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
            ItemStack[] items = new ItemStack[dataInput.readInt()];
            for (int i = 0; i < items.length; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }
            return items;
        }
    }
}
