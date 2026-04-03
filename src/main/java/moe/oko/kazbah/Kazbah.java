package moe.oko.kazbah;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import moe.oko.kazbah.api.KazbahAPI;
import moe.oko.kazbah.model.InventorySet;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import revxrsal.commands.bukkit.BukkitCommandHandler;

import java.util.concurrent.CompletableFuture;

public final class Kazbah extends JavaPlugin implements Listener, KazbahAPI {

    private DAO dao;

    @Override
    public void onEnable() {
        this.dao = new DAO(this);
        this.saveDefaultConfig();

        // Register API service
        getServer().getServicesManager().register(KazbahAPI.class, this, this, ServicePriority.Normal);

        var handler = BukkitCommandHandler.create(this);
        handler.register(new Commands(this, this));
        handler.registerBrigadier();

        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPlayerSpawn(PlayerPostRespawnEvent e) {
        String defaultInv = getConfig().getString("defaultInv");
        if (defaultInv != null && !defaultInv.isEmpty()) {
            loadInventory(e.getPlayer(), defaultInv);
        }
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregisterAll(this);
    }

    @Override
    public CompletableFuture<Boolean> saveInventory(Player player, String name) {
        InventorySet invSet = new InventorySet(player.getInventory().getContents(),
                player.getInventory().getArmorContents());
        return dao.saveInventory(player.getUniqueId().toString(), name, invSet);
    }

    @Override
    public CompletableFuture<Boolean> loadInventory(Player player, String name) {
        return dao.getInventorySet(name).thenCompose(result -> {
            if (result == null)
                return CompletableFuture.completedFuture(false);

            CompletableFuture<Boolean> future = new CompletableFuture<>();
            getServer().getScheduler().runTask(this, () -> {
                if (player.isOnline()) {
                    player.getInventory().setContents(result.inventory());
                    player.getInventory().setArmorContents(result.armor());
                    future.complete(true);
                } else {
                    future.complete(false);
                }
            });
            return future;
        });
    }

    @Override
    public CompletableFuture<InventorySet> getInventory(String name) {
        return dao.getInventorySet(name);
    }

    @Override
    public CompletableFuture<Boolean> removeInventory(String name) {
        return dao.removeInventory(name);
    }

    @Override
    public java.util.List<String> getInventoryNames() {
        return dao.getInvCatalog();
    }

    public DAO getDao() {
        return dao;
    }
}
