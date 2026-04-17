package me.ninepin.worldboss.loot;

import me.ninepin.worldboss.WorldBoss;
import me.ninepin.worldboss.damage.DamageTracker;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.Arrays;

public class LootChestManager {

    private final WorldBoss plugin;
    private final Map<String, Location> activeChests = new HashMap<>();
    private final Map<String, Set<UUID>> chestAttackers = new HashMap<>();

    public LootChestManager(WorldBoss plugin) {
        this.plugin = plugin;
    }

    public void spawnChest(String bossId) {
        Location chestLoc = plugin.getConfigManager().getChestLocation(bossId);
        if (chestLoc == null || chestLoc.getWorld() == null) {
            plugin.getLogger().warning("Boss " + bossId + " 的戰利品箱座標無效");
            return;
        }

        // Remove existing chest if any
        removeChest(bossId);

        // Place chest
        Block block = chestLoc.getBlock();
        block.setType(Material.CHEST);

        activeChests.put(bossId, chestLoc);

        // Snapshot attackers from DamageTracker before it gets cleared
        DamageTracker tracker = plugin.getDamageTracker();
        chestAttackers.put(bossId, new HashSet<>(tracker.getAttackers(bossId)));

        // Populate chest on next tick to ensure tile entity is fully initialized
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Block freshBlock = chestLoc.getBlock();
            if (freshBlock.getState() instanceof Chest chest) {
                List<ItemStack> drops = plugin.getLootConfig().rollDrops(bossId);
                plugin.getLogger().info("Boss " + bossId + " 掉落擲骰: " + drops.size() + " 個物品");
                org.bukkit.inventory.Inventory inv = chest.getInventory();
                for (ItemStack drop : drops) {
                    inv.addItem(drop);
                }
                // Do NOT call chest.update() - in Paper 1.21, BlockState.update() overwrites
                // the inventory with the captured (empty) snapshot, erasing our items.
                // The inventory modifications through getInventory() already apply directly
                // to the world's tile entity in Paper.
                plugin.getLogger().info("戰利品箱已生成於 Boss: " + bossId + " (" + drops.size() + " 個物品)");
            } else {
                plugin.getLogger().warning("無法取得 Chest 方塊狀態: " + bossId + " (type=" + freshBlock.getType() + ")");
            }
        }, 1L);
    }

    public void removeChest(String bossId) {
        chestAttackers.remove(bossId);
        Location loc = activeChests.remove(bossId);
        if (loc != null && loc.getWorld() != null) {
            Block block = loc.getBlock();
            if (block.getState() instanceof Chest chest) {
                chest.getInventory().clear();
            }
            block.setType(Material.AIR);
        }
    }

    public boolean isLootChest(Location location) {
        for (Map.Entry<String, Location> entry : activeChests.entrySet()) {
            Location chestLoc = entry.getValue();
            if (chestLoc.getWorld().equals(location.getWorld()) &&
                    chestLoc.getBlockX() == location.getBlockX() &&
                    chestLoc.getBlockY() == location.getBlockY() &&
                    chestLoc.getBlockZ() == location.getBlockZ()) {
                return true;
            }
        }
        return false;
    }

    public String getBossIdByChest(Location location) {
        for (Map.Entry<String, Location> entry : activeChests.entrySet()) {
            Location chestLoc = entry.getValue();
            if (chestLoc.getWorld().equals(location.getWorld()) &&
                    chestLoc.getBlockX() == location.getBlockX() &&
                    chestLoc.getBlockY() == location.getBlockY() &&
                    chestLoc.getBlockZ() == location.getBlockZ()) {
                return entry.getKey();
            }
        }
        return null;
    }

    public boolean canOpen(String bossId, UUID playerUUID) {
        Set<UUID> attackers = chestAttackers.get(bossId);
        return attackers != null && attackers.contains(playerUUID);
    }

    public void clearAll() {
        for (String bossId : new HashSet<>(activeChests.keySet())) {
            removeChest(bossId);
        }
    }
}
