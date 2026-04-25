package me.ninepin.worldboss.loot;

import me.ninepin.worldboss.gui.SettingsGUI;
import me.ninepin.worldboss.service.ConfigService;
import me.ninepin.worldboss.service.DropService;
import me.ninepin.worldboss.util.SoundHelper;
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

import java.util.UUID;

public class LootListener implements Listener {

    private final WorldBoss plugin;
    private final LootGUI gui;
    private final DropService dropService;
    private final ConfigService configService;
    private final SettingsGUI settingsGUI;

    public LootListener(WorldBoss plugin, LootGUI gui, DropService dropService,
                        ConfigService configService, SettingsGUI settingsGUI) {
        this.plugin = plugin;
        this.gui = gui;
        this.dropService = dropService;
        this.configService = configService;
        this.settingsGUI = settingsGUI;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String bossId = gui.openGUIs.get(player.getUniqueId());
        if (bossId == null) return;

        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) return;

        Inventory topInventory = event.getView().getTopInventory();

        if (clickedInventory.equals(topInventory)) {
            int slot = event.getSlot();

            if (slot >= 45) {
                event.setCancelled(true);

                if (slot == LootGUI.BACK_SLOT) {
                    gui.openGUIs.remove(player.getUniqueId());
                    settingsGUI.openBossMenu(player, bossId);
                }
                return;
            }

            ItemStack currentItem = clickedInventory.getItem(slot);
            boolean hasExistingItem = currentItem != null && !currentItem.getType().isAir();
            ItemStack cursorItem = event.getCursor();
            boolean hasCursorItem = cursorItem != null && !cursorItem.getType().isAir();

            if (hasExistingItem) {
                event.setCancelled(true);

                if (event.isLeftClick()) {
                    gui.awaitingChanceInput.put(player.getUniqueId(), bossId + ":" + slot);
                    player.closeInventory();
                    player.sendMessage(Component.text("請在聊天欄輸入新的掉落機率 (0-100):")
                            .color(NamedTextColor.YELLOW));
                } else if (event.isRightClick()) {
                    removeDropAtSlot(bossId, slot);
                    player.sendMessage(Component.text("已移除掉落物品")
                            .color(NamedTextColor.RED));
                    SoundHelper.playSuccess(player);
                    gui.openDropsGUI(player, bossId);
                }
            } else if (hasCursorItem && slot < LootGUI.DROP_AREA_END) {
                event.setCancelled(true);
                ItemStack newItem = cursorItem.clone();
                Bukkit.getScheduler().runTask(plugin, () -> player.setItemOnCursor(null));

                dropService.saveDrop(bossId, newItem, 50.0);
                player.sendMessage(Component.text("已新增掉落物品（預設機率 50%）")
                        .color(NamedTextColor.GREEN));
                SoundHelper.playSuccess(player);
                gui.openDropsGUI(player, bossId);
            } else {
                event.setCancelled(true);
            }
            return;
        }

        if (clickedInventory.equals(event.getView().getBottomInventory())) {
            if (event.isShiftClick()) {
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem != null && !clickedItem.getType().isAir()) {
                    event.setCancelled(true);
                    ItemStack newItem = clickedItem.clone();
                    newItem.setAmount(1);
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

                    dropService.saveDrop(bossId, newItem, 50.0);
                    player.sendMessage(Component.text("已新增掉落物品（預設機率 50%）")
                            .color(NamedTextColor.GREEN));
                    SoundHelper.playSuccess(player);
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
                    SoundHelper.playError(player);
                }
                return;
            }

            int dropIndex = dropService.resolveDropIndex(bossId, slot);
            if (dropIndex >= 0) {
                dropService.updateChance(bossId, dropIndex, chance);
                Player player = Bukkit.getPlayer(playerUUID);
                if (player != null) {
                    player.sendMessage(Component.text("已更新掉落機率為 " + chance + "%")
                            .color(NamedTextColor.GREEN));
                    SoundHelper.playSuccess(player);
                    gui.openDropsGUI(player, bossId);
                }
            }
        } catch (NumberFormatException e) {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player != null) {
                player.sendMessage(Component.text("請輸入有效的數字").color(NamedTextColor.RED));
                SoundHelper.playError(player);
            }
        }
    }

    public void removeAwaitingInput(UUID playerUUID) {
        gui.awaitingChanceInput.remove(playerUUID);
    }

    private void removeDropAtSlot(String bossId, int slot) {
        int dropIndex = dropService.resolveDropIndex(bossId, slot);
        if (dropIndex >= 0) {
            dropService.removeDrop(bossId, dropIndex);
        }
    }
}
