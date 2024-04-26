package moe.oko.kazbah;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import revxrsal.commands.bukkit.BukkitCommandHandler;

public final class Kazbah extends JavaPlugin implements Listener {

    public static Kazbah INSTANCE;
    public static DAO dao;

    @Override
    public void onEnable() {
        INSTANCE = this;
        dao = new DAO();
        this.saveDefaultConfig();
        this.reloadConfig();

        var handler = BukkitCommandHandler.create(this);
        handler.register(new Commands(dao));
        handler.registerBrigadier();

        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPlayerSpawn(PlayerPostRespawnEvent e) {
        dao.setInventory(getConfig().getString("defaultInv"), e.getPlayer().getInventory());
    }

    @Override
    public void onDisable() {}
}
