package me.ninepin.worldboss.gui;

import me.ninepin.worldboss.WorldBoss;
import me.ninepin.worldboss.boss.BossData;
import me.ninepin.worldboss.config.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class SettingsGUI {

    private final WorldBoss plugin;

    // Track which menu is open: uuid -> "main" or bossId
    final Map<UUID, String> openGUIs = new HashMap<>();
    // Remember last boss menu for reopening after chat input
    final Map<UUID, String> lastBossMenu = new HashMap<>();

    // Chat input state
    public enum InputType {
        MYTHICMOB_ID, DISPLAY_NAME, SPAWN_LOCATION,
        REALTIME_LB_LOCATION, HISTORY_LB_LOCATION, SPAWN_INTERVAL,
        SCHEDULED_TIME_ADD, DESPAWN_TIMEOUT, NEW_BOSS_ID
    }

    public record PendingInput(InputType type, String bossId) {}

    final Map<UUID, PendingInput> pendingInputs = new HashMap<>();

    // Boss menu slot constants — headers at column 0, settings follow on the same row
    public static final int HEADER_BASIC = 0;
    public static final int SLOT_MYTHICMOB = 1;
    public static final int SLOT_DISPLAY_NAME = 2;
    public static final int SLOT_SPAWN_LOC = 3;
    public static final int SLOT_DESPAWN_TIMEOUT = 4;

    public static final int HEADER_SPAWN = 9;
    public static final int SLOT_SPAWN_INTERVAL = 10;
    public static final int SLOT_SCHEDULED_TIMES = 11;

    public static final int HEADER_LOOT = 18;
    public static final int SLOT_DROP_ITEMS = 19;

    public static final int HEADER_LB = 27;
    public static final int SLOT_REALTIME_LB = 28;
    public static final int SLOT_HISTORY_LB = 29;

    public static final int SLOT_BACK = 49;
    public static final int SLOT_NEW_BOSS = 49;

    // All interactive slots in boss menu
    public static final Set<Integer> BOSS_MENU_SLOTS = Set.of(
            SLOT_MYTHICMOB, SLOT_DISPLAY_NAME, SLOT_SPAWN_LOC, SLOT_DESPAWN_TIMEOUT,
            SLOT_SPAWN_INTERVAL, SLOT_SCHEDULED_TIMES, SLOT_DROP_ITEMS,
            SLOT_REALTIME_LB, SLOT_HISTORY_LB, SLOT_BACK
    );

    private BukkitTask refreshTask;

    public SettingsGUI(WorldBoss plugin) {
        this.plugin = plugin;
    }

    public void startRefreshTask() {
        if (refreshTask != null) return;
        refreshTask = new BukkitRunnable() {
            @Override
            public void run() {
                refreshOpenGUIs();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void stopRefreshTask() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
    }

    private void refreshOpenGUIs() {
        if (openGUIs.isEmpty()) return;
        for (Map.Entry<UUID, String> entry : new HashMap<>(openGUIs).entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) continue;
            Inventory inv = player.getOpenInventory().getTopInventory();
            if (inv == null || inv.getSize() != 54) continue;
            String menuKey = entry.getValue();
            if ("main".equals(menuKey)) {
                refreshMainMenu(inv);
            } else {
                refreshBossMenu(inv, menuKey);
            }
        }
    }

    private void refreshMainMenu(Inventory inv) {
        List<String> bossIds = plugin.getConfigManager().getBossIds();
        int slot = 9;
        for (String bossId : bossIds) {
            if (slot > 44) break;
            inv.setItem(slot, createBossListItem(bossId));
            slot++;
        }
    }

    private void refreshBossMenu(Inventory inv, String bossId) {
        if (!plugin.getConfigManager().bossExists(bossId)) return;
        inv.setItem(SLOT_SPAWN_INTERVAL, createSpawnIntervalItem(bossId));
        inv.setItem(SLOT_SCHEDULED_TIMES, createScheduledTimesItem(bossId));
    }

    // ==================== Main Menu ====================

    public void openMainMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54,
                Component.text(ChatColor.GOLD + "WorldBoss 設定"));

        List<String> bossIds = plugin.getConfigManager().getBossIds();

        // Fill with glass
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, createFiller());
        }

        // Boss items in rows 1-4 (slots 9-44)
        int slot = 9;
        for (String bossId : bossIds) {
            if (slot > 44) break;
            gui.setItem(slot, createBossListItem(bossId));
            slot++;
        }

        // New Boss button
        ItemStack newBossItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta newBossMeta = newBossItem.getItemMeta();
        newBossMeta.displayName(Component.text("新增 Boss").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        newBossMeta.lore(List.of(
                Component.text("點擊建立新的 Boss 設定").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        ));
        newBossItem.setItemMeta(newBossMeta);
        gui.setItem(SLOT_NEW_BOSS, newBossItem);

        // Title
        ItemStack titleItem = new ItemStack(Material.BOOK);
        ItemMeta titleMeta = titleItem.getItemMeta();
        titleMeta.displayName(Component.text("WorldBoss 管理面板").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        titleItem.setItemMeta(titleMeta);
        gui.setItem(4, titleItem);

        player.openInventory(gui);
        openGUIs.put(player.getUniqueId(), "main");
    }

    // ==================== Boss Edit Menu ====================

    public void openBossMenu(Player player, String bossId) {
        ConfigManager cm = plugin.getConfigManager();
        BossData boss = cm.getBoss(bossId);
        ConfigManager.LeaderboardConfig lbConfig = cm.getLeaderboardConfig(bossId);

        String displayName = boss != null ? boss.getDisplayName() : bossId;
        Inventory gui = Bukkit.createInventory(null, 54,
                Component.text(ChatColor.GOLD + "Boss 設定: " + ChatColor.WHITE +
                        ChatColor.translateAlternateColorCodes('&', displayName)));

        // Fill with glass
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, createFiller());
        }

        // Section headers
        setHeader(gui, HEADER_BASIC, "&e&l基本設定");
        setHeader(gui, HEADER_SPAWN, "&e&l生成設定");
        setHeader(gui, HEADER_LOOT, "&e&l戰利品設定");
        setHeader(gui, HEADER_LB, "&e&l排行榜設定");

        // MythicMob ID
        String mythicMobId = boss != null ? boss.getMythicMobId() : "未設定";
        gui.setItem(SLOT_MYTHICMOB, createMenuItem(Material.ZOMBIE_SPAWN_EGG, "&aMythicMob ID",
                "&7目前: &f" + mythicMobId,
                "",
                "&e左鍵 → 聊天室輸入（支援自動補全）",
                "&b或使用 /wbadmin setmob " + bossId + " <id>",
                "&b  （支援 Tab 自動補全）"));

        // Display Name
        gui.setItem(SLOT_DISPLAY_NAME, createMenuItem(Material.NAME_TAG, "&a顯示名稱",
                "&7目前: &f" + ChatColor.translateAlternateColorCodes('&', displayName),
                "",
                "&e左鍵 → 在聊天室輸入名稱"));

        // Spawn Location
        String spawnLocStr = boss != null ? formatLocation(boss.getSpawnLocation()) : "未設定";
        gui.setItem(SLOT_SPAWN_LOC, createMenuItem(Material.COMPASS, "&a生成座標",
                "&7目前: &f" + spawnLocStr,
                "",
                "&e左鍵 → 使用當前位置",
                "&c右鍵 → 手動輸入座標"));

        // Despawn Timeout
        int despawnTimeout = boss != null ? boss.getDespawnTimeout() : 180;
        gui.setItem(SLOT_DESPAWN_TIMEOUT, createMenuItem(Material.CLOCK, "&a消失逾時",
                "&7目前: &f" + despawnTimeout + " 秒",
                "",
                "&e左鍵 → 在聊天室輸入秒數"));

        // Spawn Interval (dynamic countdown)
        gui.setItem(SLOT_SPAWN_INTERVAL, createSpawnIntervalItem(bossId));

        // Scheduled Times (dynamic countdown)
        gui.setItem(SLOT_SCHEDULED_TIMES, createScheduledTimesItem(bossId));

        // Drop Items
        gui.setItem(SLOT_DROP_ITEMS, createMenuItem(Material.DIAMOND, "&a掉落物設定",
                "&e點擊開啟掉落物設定介面",
                "&7可在 6x9 的 UI 中放置物品"));

        // Realtime LB Location
        String rtLbLocStr = (lbConfig != null && lbConfig.realtimeLocation != null)
                ? formatLocation(lbConfig.realtimeLocation) : "未設定";
        gui.setItem(SLOT_REALTIME_LB, createMenuItem(Material.GOLD_BLOCK, "&a即時排行榜座標",
                "&7目前: &f" + rtLbLocStr,
                "",
                "&e左鍵 → 使用當前位置",
                "&c右鍵 → 手動輸入座標"));

        // History LB Location
        String histLbLocStr = (lbConfig != null && lbConfig.historyLocation != null)
                ? formatLocation(lbConfig.historyLocation) : "未設定";
        gui.setItem(SLOT_HISTORY_LB, createMenuItem(Material.DIAMOND_BLOCK, "&a歷史排行榜座標",
                "&7目前: &f" + histLbLocStr,
                "",
                "&e左鍵 → 使用當前位置",
                "&c右鍵 → 手動輸入座標"));

        // Back button
        gui.setItem(SLOT_BACK, createMenuItem(Material.BARRIER, "&c返回主選單"));

        player.openInventory(gui);
        openGUIs.put(player.getUniqueId(), bossId);
        lastBossMenu.put(player.getUniqueId(), bossId);
    }

    // ==================== Utility Methods ====================

    private ItemStack createFiller() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.empty());
        item.setItemMeta(meta);
        return item;
    }

    private void setHeader(Inventory gui, int slot, String title) {
        ItemStack item = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(ChatColor.translateAlternateColorCodes('&', title))
                .decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        gui.setItem(slot, item);
    }

    private ItemStack createMenuItem(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(ChatColor.translateAlternateColorCodes('&', name))
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        for (String line : loreLines) {
            if (line.isEmpty()) {
                lore.add(Component.empty());
            } else {
                lore.add(Component.text(ChatColor.translateAlternateColorCodes('&', line))
                        .decoration(TextDecoration.ITALIC, false));
            }
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String formatLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) return "未設定";
        return String.format("%s %.1f, %.1f, %.1f",
                loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
    }

    // ==================== Dynamic Item Builders ====================

    private ItemStack createBossListItem(String bossId) {
        BossData boss = plugin.getConfigManager().getBoss(bossId);
        String displayName = boss != null ? boss.getDisplayName() : bossId;
        String mythicMobId = boss != null ? boss.getMythicMobId() : "未設定";
        boolean active = plugin.getBossManager().isBossActive(bossId);

        ItemStack item = new ItemStack(active ? Material.DIAMOND_SWORD : Material.IRON_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(ChatColor.translateAlternateColorCodes('&', displayName))
                .color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(loreLine("&7ID: &f" + bossId));
        lore.add(loreLine("&bMythicMob: &f" + mythicMobId));
        lore.add(Component.text("狀態: " + (active ? "存活中" : "等待生成"))
                .color(active ? NamedTextColor.GREEN : NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());

        if (boss != null && boss.hasIntervalSpawn()) {
            lore.add(loreLine("&e間隔生成: &f" + boss.getSpawnInterval() + "秒"));
        }
        if (boss != null && boss.hasScheduledSpawn()) {
            lore.add(loreLine("&e固定時間:"));
            for (BossData.ScheduledTime st : boss.getScheduledTimes()) {
                lore.add(loreLine("&f  " + st.getDayOfWeek() + " " +
                        String.format("%02d:%02d", st.getHour(), st.getMinute())));
            }
        }
        if (boss != null && (boss.hasIntervalSpawn() || boss.hasScheduledSpawn())) {
            if (active) {
                lore.add(loreLine("&6下次生成: &c存活中"));
            } else {
                Long next = plugin.getBossManager().getSpawnScheduler().getNextSpawnMillis(bossId);
                if (next != null) {
                    lore.add(loreLine("&6下次生成: &f" + formatCountdown(secondsUntil(next))));
                } else {
                    lore.add(loreLine("&6下次生成: &7尚未排程"));
                }
            }
        }

        lore.add(Component.empty());
        lore.add(loreLine("&f左鍵點擊開啟設定"));
        lore.add(loreLine("&c右鍵點擊刪除 Boss"));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSpawnIntervalItem(String bossId) {
        BossData boss = plugin.getConfigManager().getBoss(bossId);
        int interval = boss != null ? boss.getSpawnInterval() : 0;
        boolean active = plugin.getBossManager().isBossActive(bossId);

        List<String> lines = new ArrayList<>();
        lines.add("&7目前: &f" + (interval > 0 ? interval + " 秒" : "未設定"));
        if (interval > 0) {
            if (active) {
                lines.add("&6下次生成倒數: &c存活中");
            } else {
                Long next = plugin.getBossManager().getSpawnScheduler().getNextIntervalSpawnMillis(bossId);
                if (next != null) {
                    lines.add("&6下次生成倒數: &f" + formatCountdown(secondsUntil(next)));
                } else {
                    lines.add("&6下次生成倒數: &7尚未排程");
                }
            }
        }
        lines.add("");
        lines.add("&e左鍵 → 在聊天室輸入秒數");
        lines.add("&7設為 0 可停用間隔生成");
        return createMenuItem(Material.REPEATER, "&a生成間隔", lines.toArray(new String[0]));
    }

    private ItemStack createScheduledTimesItem(String bossId) {
        BossData boss = plugin.getConfigManager().getBoss(bossId);
        boolean active = plugin.getBossManager().isBossActive(bossId);

        List<String> lines = new ArrayList<>();
        lines.add("&7目前排程:");
        if (boss != null && boss.hasScheduledSpawn()) {
            for (BossData.ScheduledTime st : boss.getScheduledTimes()) {
                lines.add("&f  " + st.getDayOfWeek() + " " +
                        String.format("%02d:%02d", st.getHour(), st.getMinute()));
            }
            if (active) {
                lines.add("&6下次生成倒數: &c存活中");
            } else {
                Long next = plugin.getBossManager().getSpawnScheduler().getNextScheduledSpawnMillis(bossId);
                if (next != null) {
                    lines.add("&6下次生成倒數: &f" + formatCountdown(secondsUntil(next)));
                } else {
                    lines.add("&6下次生成倒數: &7尚未排程");
                }
            }
        } else {
            lines.add("&f  無");
        }
        lines.add("");
        lines.add("&e左鍵 → 新增排程時間");
        lines.add("&c右鍵 → 清除所有排程");
        return createMenuItem(Material.WRITABLE_BOOK, "&a固定生成時間", lines.toArray(new String[0]));
    }

    private Component loreLine(String text) {
        return Component.text(ChatColor.translateAlternateColorCodes('&', text))
                .decoration(TextDecoration.ITALIC, false);
    }

    private long secondsUntil(long nextMillis) {
        long remaining = nextMillis - System.currentTimeMillis();
        if (remaining <= 0) return 0;
        return (remaining + 999) / 1000;
    }

    private String formatCountdown(long seconds) {
        if (seconds <= 0) return "即將生成";
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (h > 0) return String.format("%d小時%02d分%02d秒", h, m, s);
        if (m > 0) return String.format("%d分%02d秒", m, s);
        return s + "秒";
    }
}
