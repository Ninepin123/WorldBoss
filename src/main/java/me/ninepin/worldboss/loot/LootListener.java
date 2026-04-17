package me.ninepin.worldboss.loot;

import me.ninepin.worldboss.WorldBoss;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Sound;

import java.util.UUID;

public class LootListener implements Listener {

    private final WorldBoss plugin;
    private final LootGUI gui;

    public LootListener(WorldBoss plugin, LootGUI gui) {
        this.plugin = plugin;
        this.gui = gui;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String bossId = gui.openGUIs.get(player.getUniqueId());
        if (bossId == null) return;

        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) return;

        Inventory topInventory = event.getView().getTopInventory();

        // Handle clicks in the top (GUI) inventory
        if (clickedInventory.equals(topInventory)) {
            int slot = event.getSlot();

            // Block control row completely
            if (slot >= 45) {
                event.setCancelled(true);

                // Back button
                if (slot == LootGUI.BACK_SLOT) {
                    gui.openGUIs.remove(player.getUniqueId());
                    plugin.getSettingsGUI().openBossMenu(player, bossId);
                }
                return;
            }

            ItemStack currentItem = clickedInventory.getItem(slot);
            boolean hasExistingItem = currentItem != null && !currentItem.getType().isAir();
            ItemStack cursorItem = event.getCursor();
            boolean hasCursorItem = cursorItem != null && !cursorItem.getType().isAir();

            if (hasExistingItem) {
                // Clicking an existing drop
                event.setCancelled(true);

                if (event.isLeftClick()) {
                    // Edit chance via chat input
                    gui.awaitingChanceInput.put(player.getUniqueId(), bossId + ":" + slot);
                    player.closeInventory();
                    player.sendMessage(Component.text("請在聊天欄輸入新的掉落機率 (0-100):")
                            .color(NamedTextColor.YELLOW));
                } else if (event.isRightClick()) {
                    // Remove drop from config
                    removeDropAtSlot(bossId, slot);
                    player.sendMessage(Component.text("已移除掉落物品")
                            .color(NamedTextColor.RED));
                    playSuccessSound(player);
                    gui.openDropsGUI(player, bossId);
                }
            } else if (hasCursorItem && slot < LootGUI.DROP_AREA_END) {
                // Place new item from cursor into empty slot
                event.setCancelled(true);
                ItemStack newItem = cursorItem.clone();
                Bukkit.getScheduler().runTask(plugin, () -> player.setItemOnCursor(null));

                plugin.getLootConfig().saveDrop(bossId, newItem, 50.0);
                player.sendMessage(Component.text("已新增掉落物品（預設機率 50%）")
                        .color(NamedTextColor.GREEN));
                playSuccessSound(player);
                gui.openDropsGUI(player, bossId);
            } else {
                event.setCancelled(true);
            }
            return;
        }

        // Handle shift-click from bottom (player) inventory to add new drops
        if (clickedInventory.equals(event.getView().getBottomInventory())) {
            if (event.isShiftClick()) {
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem != null && !clickedItem.getType().isAir()) {
                    event.setCancelled(true);
                    ItemStack newItem = clickedItem.clone();
                    int slot = event.getSlot();

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        ItemStack slotItem = player.getInventory().getItem(slot);
                        if (slotItem != null && slotItem.isSimilar(newItem)) {
                            if (slotItem.getAmount() > 1) {
                                slotItem.setAmount(slotItem.getAmount() - 1);
                            } else {
                                player.getInventory().setItem(slot, null);
                            }
                        }
                    });

                    plugin.getLootConfig().saveDrop(bossId, newItem, 50.0);
                    player.sendMessage(Component.text("已新增掉落物品（預設機率 50%）")
                            .color(NamedTextColor.GREEN));
                    playSuccessSound(player);
                    gui.openDropsGUI(player, bossId);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String bossId = gui.openGUIs.get(player.getUniqueId());
        if (bossId == null) return;

        // Block dragging in control row
        for (int slot : event.getRawSlots()) {
            if (slot >= 45 && slot < 54) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        gui.openGUIs.remove(player.getUniqueId());
    }

    // ==================== Chat Input ====================

    public boolean isAwaitingChanceInput(UUID playerUUID) {
        return gui.awaitingChanceInput.containsKey(playerUUID);
    }

    public void handleChanceInput(UUID playerUUID, String message) {
        String data = gui.awaitingChanceInput.remove(playerUUID);
        if (data == null) return;

        String[] parts = data.split(":");
        String bossId = parts[0];
        int slot = Integer.parseInt(parts[1]);

        try {
            double chance = Double.parseDouble(message);
            if (chance < 0 || chance > 100) {
                Player player = Bukkit.getPlayer(playerUUID);
                if (player != null) {
                    player.sendMessage(Component.text("機率必須在 0-100 之間").color(NamedTextColor.RED));
                    playErrorSound(player);
                }
                return;
            }

            int dropIndex = resolveDropIndex(bossId, slot);
            if (dropIndex >= 0) {
                plugin.getLootConfig().updateChance(bossId, dropIndex, chance);
                Player player = Bukkit.getPlayer(playerUUID);
                if (player != null) {
                    player.sendMessage(Component.text("已更新掉落機率為 " + chance + "%")
                            .color(NamedTextColor.GREEN));
                    playSuccessSound(player);
                    gui.openDropsGUI(player, bossId);
                }
            }
        } catch (NumberFormatException e) {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player != null) {
                player.sendMessage(Component.text("請輸入有效的數字").color(NamedTextColor.RED));
                playErrorSound(player);
            }
        }
    }

    public void removeAwaitingInput(UUID playerUUID) {
        gui.awaitingChanceInput.remove(playerUUID);
    }

    // ==================== Sound Feedback ====================

    private void playSuccessSound(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
    }

    private void playErrorSound(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.6f);
    }

    // ==================== Drop Management ====================

    private void removeDropAtSlot(String bossId, int slot) {
        int dropIndex = resolveDropIndex(bossId, slot);
        if (dropIndex >= 0) {
            plugin.getLootConfig().removeDrop(bossId, dropIndex);
        }
    }

    private int resolveDropIndex(String bossId, int slot) {
        java.io.File file = new java.io.File(plugin.getDataFolder(), "drops/" + bossId + ".yml");
        if (!file.exists()) return -1;

        org.bukkit.configuration.file.YamlConfiguration yaml =
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
        org.bukkit.configuration.ConfigurationSection items = yaml.getConfigurationSection("items");
        if (items == null) return -1;

        java.util.List<String> keys = new java.util.ArrayList<>(items.getKeys(false));
        keys.sort(java.util.Comparator.comparingInt(Integer::parseInt));
        if (slot < keys.size()) {
            return Integer.parseInt(keys.get(slot));
        }
        return -1;
    }
}
