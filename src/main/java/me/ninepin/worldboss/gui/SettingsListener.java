package me.ninepin.worldboss.gui;

import io.lumine.mythic.bukkit.MythicBukkit;
import me.ninepin.worldboss.WorldBoss;
import me.ninepin.worldboss.loot.LootGUI;
import me.ninepin.worldboss.service.BossService;
import me.ninepin.worldboss.service.ConfigService;
import me.ninepin.worldboss.util.SoundHelper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;

public class SettingsListener implements Listener {

    private final WorldBoss plugin;
    private final SettingsGUI gui;
    private final ConfigService configService;
    private final BossService bossService;
    private final LootGUI lootGUI;

    public SettingsListener(WorldBoss plugin, SettingsGUI gui, ConfigService configService,
                            BossService bossService, LootGUI lootGUI) {
        this.plugin = plugin;
        this.gui = gui;
        this.configService = configService;
        this.bossService = bossService;
        this.lootGUI = lootGUI;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String menuKey = gui.openGUIs.get(player.getUniqueId());
        if (menuKey == null) return;

        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) return;

        Inventory topInventory = event.getView().getTopInventory();
        if (!clickedInventory.equals(topInventory)) return;

        event.setCancelled(true);

        int slot = event.getSlot();

        if ("main".equals(menuKey)) {
            handleMainMenuClick(player, slot, event.isLeftClick(), event.isRightClick());
        } else {
            handleBossMenuClick(player, menuKey, slot, event.isLeftClick(), event.isRightClick());
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        gui.openGUIs.remove(player.getUniqueId());
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (gui.openGUIs.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private void handleMainMenuClick(Player player, int slot, boolean leftClick, boolean rightClick) {
        if (slot == SettingsGUI.SLOT_NEW_BOSS) {
            player.closeInventory();
            gui.pendingInputs.put(player.getUniqueId(), new SettingsGUI.PendingInput(SettingsGUI.InputType.NEW_BOSS_ID, ""));
            player.sendMessage(Component.text("請在聊天室輸入新的 Boss ID（英文、數字、底線）:").color(NamedTextColor.YELLOW));
            player.sendMessage(Component.text("輸入 cancel 取消").color(NamedTextColor.GRAY));
            return;
        }

        if (slot >= 9 && slot <= 44) {
            List<String> bossIds = configService.getBossIds();
            int bossIndex = slot - 9;
            if (bossIndex < bossIds.size()) {
                if (leftClick) {
                    gui.openBossMenu(player, bossIds.get(bossIndex));
                } else if (rightClick) {
                    String bossId = bossIds.get(bossIndex);
                    configService.deleteBoss(bossId);
                    bossService.reloadConfig();
                    player.sendMessage(Component.text("已刪除 Boss: " + bossId).color(NamedTextColor.GREEN));
                    SoundHelper.playSuccess(player);
                    gui.openMainMenu(player);
                }
            }
        }
    }

    private void handleBossMenuClick(Player player, String bossId, int slot, boolean leftClick, boolean rightClick) {
        if (slot == SettingsGUI.SLOT_BACK) {
            gui.openMainMenu(player);
            return;
        }

        if (!SettingsGUI.BOSS_MENU_SLOTS.contains(slot)) return;

        if (slot == SettingsGUI.SLOT_MYTHICMOB) {
            handleMythicMobClick(player, bossId);
        } else if (slot == SettingsGUI.SLOT_DISPLAY_NAME) {
            promptChatInput(player, bossId, SettingsGUI.InputType.DISPLAY_NAME,
                    "請在聊天室輸入新的顯示名稱（可使用 & 顏色代碼）:");
        } else if (slot == SettingsGUI.SLOT_SPAWN_LOC) {
            if (leftClick) {
                setLocationToCurrent(player, bossId, SettingsGUI.InputType.SPAWN_LOCATION);
            } else if (rightClick) {
                promptChatInput(player, bossId, SettingsGUI.InputType.SPAWN_LOCATION,
                        "請在聊天室輸入座標，格式: x y z 或 world x y z:");
            }
        } else if (slot == SettingsGUI.SLOT_DESPAWN_TIMEOUT) {
            promptChatInput(player, bossId, SettingsGUI.InputType.DESPAWN_TIMEOUT,
                    "請在聊天室輸入消失逾時秒數:");
        } else if (slot == SettingsGUI.SLOT_SPAWN_INTERVAL) {
            promptChatInput(player, bossId, SettingsGUI.InputType.SPAWN_INTERVAL,
                    "請在聊天室輸入生成間隔秒數（0 = 停用）:");
        } else if (slot == SettingsGUI.SLOT_SCHEDULED_TIMES) {
            if (leftClick) {
                promptChatInput(player, bossId, SettingsGUI.InputType.SCHEDULED_TIME_ADD,
                        "請在聊天室輸入排程時間，格式: 星期 時:分",
                        "例如: MONDAY 20:00",
                        "星期: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY");
            } else if (rightClick) {
                configService.clearBossScheduledTimes(bossId);
                bossService.reloadConfig();
                player.sendMessage(Component.text("已清除所有排程時間").color(NamedTextColor.GREEN));
                SoundHelper.playSuccess(player);
                gui.openBossMenu(player, bossId);
            }
        } else if (slot == SettingsGUI.SLOT_DROP_ITEMS) {
            gui.openGUIs.remove(player.getUniqueId());
            lootGUI.openDropsGUI(player, bossId);
        } else if (slot == SettingsGUI.SLOT_REALTIME_LB) {
            if (leftClick) {
                setLocationToCurrent(player, bossId, SettingsGUI.InputType.REALTIME_LB_LOCATION);
            } else if (rightClick) {
                promptChatInput(player, bossId, SettingsGUI.InputType.REALTIME_LB_LOCATION,
                        "請在聊天室輸入即時排行榜座標，格式: x y z 或 world x y z:");
            }
        } else if (slot == SettingsGUI.SLOT_HISTORY_LB) {
            if (leftClick) {
                setLocationToCurrent(player, bossId, SettingsGUI.InputType.HISTORY_LB_LOCATION);
            } else if (rightClick) {
                promptChatInput(player, bossId, SettingsGUI.InputType.HISTORY_LB_LOCATION,
                        "請在聊天室輸入歷史排行榜座標，格式: x y z 或 world x y z:");
            }
        }
    }

    private void handleMythicMobClick(Player player, String bossId) {
        player.closeInventory();

        var boss = configService.getBoss(bossId);
        String currentId = boss != null ? boss.getMythicMobId() : "未設定";

        player.sendMessage(Component.text("===== 設定 MythicMob ID =====").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("目前: " + currentId).color(NamedTextColor.WHITE));
        player.sendMessage(Component.empty());

        List<String> mobIds = getMythicMobIds();
        if (mobIds.isEmpty()) {
            player.sendMessage(Component.text("無法取得 MythicMob ID 列表").color(NamedTextColor.RED));
        } else {
            player.sendMessage(Component.text("可用的 MythicMob ID:").color(NamedTextColor.YELLOW));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mobIds.size(); i++) {
                sb.append(mobIds.get(i));
                if (i < mobIds.size() - 1) sb.append(", ");
                if (sb.length() > 60 || i == mobIds.size() - 1) {
                    player.sendMessage(Component.text("  " + sb).color(NamedTextColor.WHITE));
                    sb = new StringBuilder();
                }
            }
        }

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("方法 1: 在聊天室輸入 ID（支援部分匹配自動補全）").color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("方法 2: 使用 /wbadmin setmob " + bossId + " <id>（支援 Tab 自動補全）").color(NamedTextColor.AQUA));
        player.sendMessage(Component.text("輸入 cancel 取消").color(NamedTextColor.GRAY));

        gui.pendingInputs.put(player.getUniqueId(), new SettingsGUI.PendingInput(SettingsGUI.InputType.MYTHICMOB_ID, bossId));
    }

    private void setLocationToCurrent(Player player, String bossId, SettingsGUI.InputType type) {
        Location loc = player.getLocation();
        switch (type) {
            case SPAWN_LOCATION -> configService.setBossSpawnLocation(bossId, loc);
            case REALTIME_LB_LOCATION -> configService.setRealtimeLBLocation(bossId, loc);
            case HISTORY_LB_LOCATION -> configService.setHistoryLBLocation(bossId, loc);
            default -> { return; }
        }
        bossService.reloadConfig();
        player.sendMessage(Component.text("已設定座標為您當前的位置").color(NamedTextColor.GREEN));
        SoundHelper.playSuccess(player);
        gui.openBossMenu(player, bossId);
    }

    private void promptChatInput(Player player, String bossId, SettingsGUI.InputType type, String... messages) {
        player.closeInventory();
        gui.pendingInputs.put(player.getUniqueId(), new SettingsGUI.PendingInput(type, bossId));
        for (String msg : messages) {
            player.sendMessage(Component.text(msg).color(NamedTextColor.YELLOW));
        }
        player.sendMessage(Component.text("輸入 cancel 取消").color(NamedTextColor.GRAY));
    }

    public boolean isAwaitingInput(UUID uuid) {
        return gui.pendingInputs.containsKey(uuid);
    }

    public void handleChatInput(UUID uuid, String message) {
        SettingsGUI.PendingInput pending = gui.pendingInputs.get(uuid);
        if (pending == null) return;

        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            gui.pendingInputs.remove(uuid);
            return;
        }

        String bossId = pending.bossId();

        if ("cancel".equalsIgnoreCase(message.trim())) {
            gui.pendingInputs.remove(uuid);
            player.sendMessage(Component.text("已取消輸入").color(NamedTextColor.GRAY));
            SoundHelper.playCancel(player);
            String lastBoss = gui.lastBossMenu.get(uuid);
            if (lastBoss != null && configService.bossExists(lastBoss)) {
                gui.openBossMenu(player, lastBoss);
            } else {
                gui.openMainMenu(player);
            }
            return;
        }

        switch (pending.type()) {
            case NEW_BOSS_ID -> handleNewBossId(player, message);
            case MYTHICMOB_ID -> handleMythicMobInput(player, bossId, message);
            case DISPLAY_NAME -> handleDisplayNameInput(player, bossId, message);
            case SPAWN_LOCATION -> handleLocationInput(player, bossId, SettingsGUI.InputType.SPAWN_LOCATION, message);
            case REALTIME_LB_LOCATION -> handleLocationInput(player, bossId, SettingsGUI.InputType.REALTIME_LB_LOCATION, message);
            case HISTORY_LB_LOCATION -> handleLocationInput(player, bossId, SettingsGUI.InputType.HISTORY_LB_LOCATION, message);
            case SPAWN_INTERVAL -> handleSpawnIntervalInput(player, bossId, message);
            case SCHEDULED_TIME_ADD -> handleScheduledTimeInput(player, bossId, message);
            case DESPAWN_TIMEOUT -> handleDespawnTimeoutInput(player, bossId, message);
        }
    }

    private void handleNewBossId(Player player, String message) {
        String newBossId = message.trim().replace(" ", "_").replaceAll("[^a-zA-Z0-9_-]", "");
        if (newBossId.isEmpty()) {
            player.sendMessage(Component.text("Boss ID 不能為空").color(NamedTextColor.RED));
            SoundHelper.playError(player);
            return;
        }
        if (configService.bossExists(newBossId)) {
            player.sendMessage(Component.text("Boss ID '" + newBossId + "' 已存在！").color(NamedTextColor.RED));
            SoundHelper.playError(player);
            return;
        }

        gui.pendingInputs.remove(player.getUniqueId());

        configService.createNewBoss(newBossId, "SkeletalKnight", "&c新 Boss", player.getLocation());
        bossService.reloadConfig();

        player.sendMessage(Component.text("已建立新 Boss: " + newBossId).color(NamedTextColor.GREEN));
        SoundHelper.playSuccess(player);
        player.sendMessage(Component.text("請繼續設定 MythicMob ID 和其他選項").color(NamedTextColor.YELLOW));
        gui.openBossMenu(player, newBossId);
    }

    private void handleMythicMobInput(Player player, String bossId, String message) {
        String input = message.trim();
        List<String> mobIds = getMythicMobIds();

        if (mobIds.contains(input)) {
            gui.pendingInputs.remove(player.getUniqueId());
            configService.setBossMythicMobId(bossId, input);
            bossService.reloadConfig();
            player.sendMessage(Component.text("已設定 MythicMob ID: " + input).color(NamedTextColor.GREEN));
            SoundHelper.playSuccess(player);
            gui.openBossMenu(player, bossId);
            return;
        }

        List<String> matches = mobIds.stream()
                .filter(id -> id.toLowerCase().contains(input.toLowerCase()))
                .collect(Collectors.toList());

        if (matches.isEmpty()) {
            player.sendMessage(Component.text("找不到匹配的 MythicMob ID: " + input).color(NamedTextColor.RED));
            SoundHelper.playError(player);
            player.sendMessage(Component.text("請重新輸入，或輸入 cancel 取消").color(NamedTextColor.YELLOW));
        } else if (matches.size() == 1) {
            String matched = matches.get(0);
            gui.pendingInputs.remove(player.getUniqueId());
            configService.setBossMythicMobId(bossId, matched);
            bossService.reloadConfig();
            player.sendMessage(Component.text("自動匹配到 MythicMob ID: " + matched).color(NamedTextColor.GREEN));
            SoundHelper.playSuccess(player);
            gui.openBossMenu(player, bossId);
        } else {
            player.sendMessage(Component.text("找到 " + matches.size() + " 個匹配的 ID，請輸入更精確的名稱:").color(NamedTextColor.YELLOW));
            int shown = 0;
            for (String match : matches) {
                if (shown >= 15) {
                    player.sendMessage(Component.text("  ... 還有 " + (matches.size() - 15) + " 個結果").color(NamedTextColor.GRAY));
                    break;
                }
                player.sendMessage(Component.text("  - " + match).color(NamedTextColor.WHITE));
                shown++;
            }
            player.sendMessage(Component.text("或使用 /wbadmin setmob " + bossId + " <id>（支援 Tab 補全）").color(NamedTextColor.AQUA));
        }
    }

    private void handleDisplayNameInput(Player player, String bossId, String message) {
        gui.pendingInputs.remove(player.getUniqueId());
        configService.setBossDisplayName(bossId, message.trim());
        bossService.reloadConfig();
        player.sendMessage(Component.text("已設定顯示名稱").color(NamedTextColor.GREEN));
        SoundHelper.playSuccess(player);
        gui.openBossMenu(player, bossId);
    }

    private void handleLocationInput(Player player, String bossId, SettingsGUI.InputType type, String message) {
        Location loc = parseLocationInput(message.trim(), player);
        if (loc == null) {
            player.sendMessage(Component.text("無效的座標格式！使用: x y z 或 world x y z").color(NamedTextColor.RED));
            SoundHelper.playError(player);
            player.sendMessage(Component.text("請重新輸入，或輸入 cancel 取消").color(NamedTextColor.YELLOW));
            return;
        }

        gui.pendingInputs.remove(player.getUniqueId());
        switch (type) {
            case SPAWN_LOCATION -> configService.setBossSpawnLocation(bossId, loc);
            case REALTIME_LB_LOCATION -> configService.setRealtimeLBLocation(bossId, loc);
            case HISTORY_LB_LOCATION -> configService.setHistoryLBLocation(bossId, loc);
            default -> { return; }
        }
        bossService.reloadConfig();
        player.sendMessage(Component.text("已設定座標").color(NamedTextColor.GREEN));
        SoundHelper.playSuccess(player);
        gui.openBossMenu(player, bossId);
    }

    private void handleSpawnIntervalInput(Player player, String bossId, String message) {
        try {
            int seconds = Integer.parseInt(message.trim());
            if (seconds < 0) throw new NumberFormatException();
            gui.pendingInputs.remove(player.getUniqueId());
            configService.setBossSpawnInterval(bossId, seconds);
            bossService.reloadConfig();
            player.sendMessage(Component.text("已設定生成間隔: " + (seconds > 0 ? seconds + " 秒" : "已停用")).color(NamedTextColor.GREEN));
            SoundHelper.playSuccess(player);
            gui.openBossMenu(player, bossId);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("請輸入有效的非負整數").color(NamedTextColor.RED));
            SoundHelper.playError(player);
            player.sendMessage(Component.text("請重新輸入，或輸入 cancel 取消").color(NamedTextColor.YELLOW));
        }
    }

    private void handleScheduledTimeInput(Player player, String bossId, String message) {
        String input = message.trim().toUpperCase();
        try {
            String[] parts = input.split("\\s+");
            if (parts.length != 2) throw new IllegalArgumentException("格式錯誤");

            DayOfWeek day = DayOfWeek.valueOf(parts[0]);
            String[] timeParts = parts[1].split(":");
            if (timeParts.length != 2) throw new IllegalArgumentException("時間格式錯誤");
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                throw new IllegalArgumentException("時間範圍無效");
            }

            String scheduleString = parts[0] + " " + String.format("%02d:%02d", hour, minute);
            gui.pendingInputs.remove(player.getUniqueId());
            configService.addBossScheduledTime(bossId, scheduleString);
            bossService.reloadConfig();
            player.sendMessage(Component.text("已新增排程: " + scheduleString).color(NamedTextColor.GREEN));
            SoundHelper.playSuccess(player);
            gui.openBossMenu(player, bossId);
        } catch (Exception e) {
            player.sendMessage(Component.text("格式無效！請使用: 星期 時:分（例如 MONDAY 20:00）").color(NamedTextColor.RED));
            SoundHelper.playError(player);
            player.sendMessage(Component.text("星期: MONDAY~SUNDAY").color(NamedTextColor.GRAY));
            player.sendMessage(Component.text("請重新輸入，或輸入 cancel 取消").color(NamedTextColor.YELLOW));
        }
    }

    private void handleDespawnTimeoutInput(Player player, String bossId, String message) {
        try {
            int seconds = Integer.parseInt(message.trim());
            if (seconds <= 0) throw new NumberFormatException();
            gui.pendingInputs.remove(player.getUniqueId());
            configService.setBossDespawnTimeout(bossId, seconds);
            bossService.reloadConfig();
            player.sendMessage(Component.text("已設定消失逾時: " + seconds + " 秒").color(NamedTextColor.GREEN));
            SoundHelper.playSuccess(player);
            gui.openBossMenu(player, bossId);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("請輸入有效的正整數").color(NamedTextColor.RED));
            SoundHelper.playError(player);
            player.sendMessage(Component.text("請重新輸入，或輸入 cancel 取消").color(NamedTextColor.YELLOW));
        }
    }

    public List<String> getMythicMobIds() {
        try {
            return MythicBukkit.inst().getMobManager().getMobNames().stream()
                    .sorted()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            plugin.getLogger().warning("無法取得 MythicMob ID 列表: " + e.getMessage());
            return List.of();
        }
    }

    private Location parseLocationInput(String input, Player player) {
        String[] parts = input.trim().split("\\s+");
        try {
            if (parts.length == 3) {
                double x = Double.parseDouble(parts[0]);
                double y = Double.parseDouble(parts[1]);
                double z = Double.parseDouble(parts[2]);
                return new Location(player.getWorld(), x, y, z);
            } else if (parts.length == 4) {
                String worldName = parts[0];
                double x = Double.parseDouble(parts[1]);
                double y = Double.parseDouble(parts[2]);
                double z = Double.parseDouble(parts[3]);
                World world = Bukkit.getWorld(worldName);
                if (world == null) return null;
                return new Location(world, x, y, z);
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return null;
    }

    public String getLastBossMenu(UUID uuid) {
        return gui.lastBossMenu.get(uuid);
    }

    public void removePendingInput(UUID uuid) {
        gui.pendingInputs.remove(uuid);
    }
}
