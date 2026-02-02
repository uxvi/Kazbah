# Kazbah
Dead simple inventory management plugin.

## Functionality
+ `/inv <save/load/add/remove/list>`
+ Default inventory on spawn
---

### Why

+ [Alcazar](https://github.com/uxvi/alcazar) can be difficult and/or time-consuming to set up if you are not familiar with SQL.
+ I needed a performant, self-contained inventory serialization library.
+ Alcazar is poorly written.

### How
Download the latest [release](https://github.com/uxvi/kazbah/releases) & restart Paper.

**DO NOT CONTACT FOR SUPPORT. PULL REQUESTS WILL BE REVIEWED.**

### API

Plugins can interact with Kazbah using the KazbahAPI interface.

Access the API:

```java
KazbahAPI api = Bukkit.getServicesManager().load(KazbahAPI.class);
if (api != null) {
    // Logic
}
```

Example of loading an inventory to a player

```java
api.loadInventory(player, "starter").thenAccept(success -> {
    if (success) {
        player.sendMessage("Your starter kit has been loaded!");
    }
});
```

Example of grabbing the raw InventorySet

```java
api.getInventory("starter").thenAccept(invSet -> {
    if (invSet != null) {
        ItemStack[] contents = invSet.inventory();
        // Do something with the items
    }
});
```

### Who
This software is licensed under the terms of the MIT license.

You can find a copy of the license in the [LICENSE file](LICENSE).