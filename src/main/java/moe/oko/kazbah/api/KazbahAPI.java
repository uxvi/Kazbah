package moe.oko.kazbah.api;

import moe.oko.kazbah.model.InventorySet;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public interface KazbahAPI {

    /**
     * Saves a player's current inventory under the specified name.
     *
     * @param player The player whose inventory to save.
     * @param name   The name to save the inventory as.
     * @return A CompletableFuture that completes with true if saved successfully.
     */
    CompletableFuture<Boolean> saveInventory(Player player, String name);

    /**
     * Loads a saved inventory to a player.
     * Automatically handles thread synchronization (loads from DB async, sets
     * inventory sync).
     *
     * @param player The player to load the inventory to.
     * @param name   The name of the inventory to load.
     * @return A CompletableFuture that completes with true if loaded successfully.
     */
    CompletableFuture<Boolean> loadInventory(Player player, String name);

    /**
     * Retrieves the raw InventorySet for a given name.
     *
     * @param name The name of the inventory.
     * @return A CompletableFuture that completes with the InventorySet, or null if
     *         not found.
     */
    CompletableFuture<InventorySet> getInventory(String name);

    /**
     * Removes a saved inventory.
     *
     * @param name The name of the inventory to remove.
     * @return A CompletableFuture that completes with true if removed successfully.
     */
    CompletableFuture<Boolean> removeInventory(String name);

    /**
     * Gets a list of all saved inventory names.
     *
     * @return A list of all saved inventory names.
     */
    java.util.List<String> getInventoryNames();
}
