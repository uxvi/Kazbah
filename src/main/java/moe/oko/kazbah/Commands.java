package moe.oko.kazbah;

import moe.oko.kazbah.model.InventorySet;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Single;

public class Commands {

    private final DAO dao;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public Commands(DAO dao) { this.dao = dao; }

    @Command("inv save")
    public void invSave(Player sender, @Single String inventory) {
        PlayerInventory playerInv = sender.getInventory();
        String message = dao.saveInventory(sender.getUniqueId().toString(), inventory, new InventorySet(
                playerInv.getContents(), playerInv.getArmorContents()))
                ? "Saved inventory <green>%s</green>".formatted(inventory)
                : "<red>Unable to save inventory</red>";
        sender.sendMessage(mm.deserialize(message));
    }

    @Command("inv load")
    public void invLoad(Player sender, @Single String request) {
        String message = dao.setInventory(request, sender.getInventory())
                ? "Loaded inventory %s".formatted(request)
                : "Unable to load inventory";
        sender.sendMessage(mm.deserialize(message));
    }

    @Command("inv add")
    public void invAdd(Player sender, @Single String request) {
        String message = dao.addToInventory(request, sender.getInventory())
                ? "Loaded inventory %s".formatted(request)
                : "Unable to load inventory";
        sender.sendMessage(mm.deserialize(message));
    }

    @Command("inv remove")
    public void invRemove(Player sender, @Single String request) {
        String message = dao.removeInventory(request)
                ? "Removed inventory <green>%s</green>".formatted(request)
                : "<red>Unable to remove inventory</red>";
        sender.sendMessage(mm.deserialize(message));
    }

    @Command("inv list")
    public void invList(Player sender) {
        var sb = new StringBuilder();
        for (String name : dao.getInvCatalog())
            sb.append(name).append(", ");
        sender.sendMessage(sb.toString());
    }
}
