package moe.oko.kazbah;

import moe.oko.kazbah.api.KazbahAPI;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Description;
import revxrsal.commands.annotation.Single;
import revxrsal.commands.bukkit.annotation.CommandPermission;

public class Commands {

    private final Kazbah kazbah;
    private final KazbahAPI api;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public Commands(Kazbah kazbah, KazbahAPI api) {
        this.kazbah = kazbah;
        this.api = api;
    }

    @Command("inv save")
    @CommandPermission("kazbah.admin")
    @Description("Save your current inventory")
    public void invSave(Player sender, @Single String inventory) {
        api.saveInventory(sender, inventory).thenAccept(success -> {
            String message = success
                    ? "<green>Saved inventory</green> <white>%s</white>".formatted(inventory)
                    : "<red>Unable to save inventory</red>";
            sender.sendMessage(mm.deserialize(message));
        });
    }

    @Command("inv load")
    @CommandPermission("kazbah.admin")
    @Description("Load a saved inventory")
    public void invLoad(Player sender, @Single String request) {
        api.loadInventory(sender, request).thenAccept(success -> {
            String message = success
                    ? "<green>Loaded inventory</green> <white>%s</white>".formatted(request)
                    : "<red>Unable to load inventory</red>";
            sender.sendMessage(mm.deserialize(message));
        });
    }

    @Command("inv add")
    @CommandPermission("kazbah.admin")
    @Description("Add a saved inventory to your current items")
    public void invAdd(Player sender, @Single String request) {
        api.getInventory(request).thenAccept(result -> {
            kazbah.getServer().getScheduler().runTask(kazbah, () -> {
                if (result == null) {
                    sender.sendMessage(mm.deserialize("<red>Unable to find inventory</red>"));
                    return;
                }
                boolean full = false;
                for (ItemStack item : result.inventory()) {
                    if (item != null && !full) {
                        if (!sender.getInventory().addItem(item).isEmpty()) {
                            full = true;
                        }
                    }
                }
                sender.sendMessage(mm
                        .deserialize("<green>Added items from inventory</green> <white>%s</white>".formatted(request)));
                if (full) {
                    sender.sendMessage(mm.deserialize("<yellow>Inventory full; some items were omitted.</yellow>"));
                }
            });
        });
    }

    @Command("inv remove")
    @CommandPermission("kazbah.admin")
    @Description("Remove a saved inventory")
    public void invRemove(Player sender, @Single String request) {
        api.removeInventory(request).thenAccept(success -> {
            String message = success
                    ? "<green>Removed inventory</green> <white>%s</white>".formatted(request)
                    : "<red>Unable to remove inventory</red>";
            sender.sendMessage(mm.deserialize(message));
        });
    }

    @Command("inv list")
    @CommandPermission("kazbah.admin")
    @Description("List all saved inventories")
    public void invList(Player sender) {
        var catalog = api.getInventoryNames();
        if (catalog.isEmpty()) {
            sender.sendMessage(mm.deserialize("<yellow>No inventories saved.</yellow>"));
            return;
        }
        String list = String.join(", ", catalog);
        sender.sendMessage(mm.deserialize("<gold>Saved inventories:</gold> <white>" + mm.escapeTags(list) + "</white>"));
    }
}
