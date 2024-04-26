package moe.oko.kazbah;

import moe.oko.kazbah.model.InventorySet;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.slf4j.Logger;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DAO {

    private static final Logger log = Kazbah.INSTANCE.getSLF4JLogger();
    private List<String> invCatalog = new ArrayList<>();

    public DAO() { initTables(); }

    protected boolean saveInventory(String uuid, String name, InventorySet invSet) {
        try (Connection conn = connect()) {
            ByteArrayInputStream inventory = serializeItemArray(invSet.inventory());
            ByteArrayInputStream armor = serializeItemArray(invSet.armor());

            PreparedStatement ps = conn.prepareStatement("REPLACE INTO inventories(uuid, name, si, sa) VALUES (?, ?, ?, ?)");
            ps.setString(1, uuid);
            ps.setString(2, name);
            ps.setBytes(3, inventory.readAllBytes());
            ps.setBytes(4, armor.readAllBytes());
            ps.execute();
            ps.close();

            updateInvCache(conn);
            return true;
        } catch (Exception e) { log.error("Unable to save ItemStack[] to DB", e); }
        return false;
    }

    protected boolean setInventory(String request, PlayerInventory inv) {
        InventorySet result = getInventory(request);
        if (result == null) return false;
        inv.setContents(result.inventory());
        inv.setArmorContents(result.armor());
        return true;
    }

    protected boolean addToInventory(String request, PlayerInventory inv) {
        InventorySet result = getInventory(request);
        if (result == null) return false;
        for (ItemStack item : result.inventory()) inv.addItem();
        return true;
    }

    protected boolean removeInventory(String request) {
        try (Connection conn = connect()) {
            PreparedStatement ps = conn.prepareStatement("DELETE FROM inventories WHERE name=?");
            ps.setString(1, request);
            ps.execute();
            ps.close();

            updateInvCache(conn);
            return true;
        } catch (SQLException e) { log.error("Unable to remove inventory from DB", e); }
        return false;
    }

    protected List<String> getInvCatalog() { return invCatalog; }

    private InventorySet getInventory(String request) {
        try (Connection conn = connect()) {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM inventories WHERE name=?");
            ps.setString(1, request);
            ResultSet rs = ps.executeQuery();

            return new InventorySet(
                    deserializeItemArray(new ByteArrayInputStream(rs.getBytes(3))),
                    deserializeItemArray(new ByteArrayInputStream(rs.getBytes(4))));
        } catch (Exception e) { return null; }
    }

    private void updateInvCache(Connection conn) {
        try {
            PreparedStatement ps = conn.prepareStatement("SELECT name FROM inventories");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                List<String> names = new ArrayList<>();
                do { names.add(rs.getString("name")); }
                while (rs.next());
                ps.close();
                rs.close();
                invCatalog = names;
            }
        } catch (SQLException e) { log.error("Unable to get the inventory list.", e); }
    }

    private void initTables() {
        var dataFolder = new File(Kazbah.INSTANCE.getDataFolder(),"data.db");
        if (!dataFolder.exists()){
            try { dataFolder.createNewFile(); }
            catch (IOException e) { log.error("Unable to create DB"); }
        }
        String sql =
                "CREATE TABLE IF NOT EXISTS inventories("
                + "uuid TEXT, name TEXT UNIQUE, si BLOB, sa BLOB)";
        try (Connection conn = connect()) { conn.prepareStatement(sql).execute(); updateInvCache(conn); }
        catch (SQLException e) { throw new RuntimeException(e); }
    }

    private Connection connect() {
        final String url = "jdbc:sqlite:plugins/kazbah/data.db";
        Connection conn = null;
        try { conn = DriverManager.getConnection(url); }
        catch (SQLException e) { log.error("Unable to connect to DB!"); }
        return conn;
    }

    private ByteArrayInputStream serializeItemArray(ItemStack[] items) throws IOException {
        var outputStream = new ByteArrayOutputStream();
        var dataOutput = new BukkitObjectOutputStream(outputStream);

        dataOutput.writeInt(items.length);

        for (ItemStack item : items)
            dataOutput.writeObject(item);

        dataOutput.close();
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    private ItemStack[] deserializeItemArray(InputStream inputStream) throws IOException, ClassNotFoundException {
        if (inputStream == null || inputStream.available() == 0)
            return new ItemStack[0];
        var dataInput = new BukkitObjectInputStream(inputStream);
        ItemStack[] items = new ItemStack[dataInput.readInt()];

        for (int i = 0; i < items.length; i++)
            items[i] = (ItemStack) dataInput.readObject();

        dataInput.close();
        return items;
    }
}
