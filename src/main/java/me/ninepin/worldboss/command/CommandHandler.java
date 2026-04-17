package me.ninepin.worldboss.command;

import me.ninepin.worldboss.WorldBoss;
import me.ninepin.worldboss.boss.BossData;
import me.ninepin.worldboss.boss.BossManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CommandHandler implements TabExecutor {

    private final WorldBoss plugin;

    public CommandHandler(WorldBoss plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        switch (command.getName().toLowerCase()) {
            case "wb" -> handlePlayerCommand(sender, args);
            case "wbadmin" -> handleAdminCommand(sender, args);
        }
        return true;
    }

    private void handlePlayerCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendPlayerUsage(sender);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "list" -> cmdList(sender);
            case "info" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("用法: /wb info <bossId>").color(NamedTextColor.RED));
                    return;
                }
                cmdInfo(sender, args[1]);
            }
            default -> sendPlayerUsage(sender);
        }
    }

    private void handleAdminCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendAdminUsage(sender);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "spawn" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("用法: /wbadmin spawn <bossId>").color(NamedTextColor.RED));
                    return;
                }
                cmdSpawn(sender, args[1]);
            }
            case "despawn" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("用法: /wbadmin despawn <bossId>").color(NamedTextColor.RED));
                    return;
                }
                cmdDespawn(sender, args[1]);
            }
            case "reload" -> cmdReload(sender);
            case "drops" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("此指令僅限玩家使用").color(NamedTextColor.RED));
                    return;
                }
                if (args.length < 2) {
                    player.sendMessage(Component.text("用法: /wbadmin drops <bossId>").color(NamedTextColor.RED));
                    return;
                }
                cmdDrops(player, args[1]);
            }
            case "setdrop" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("此指令僅限玩家使用").color(NamedTextColor.RED));
                    return;
                }
                if (args.length < 3) {
                    player.sendMessage(Component.text("用法: /wbadmin setdrop <bossId> <chance>").color(NamedTextColor.RED));
                    return;
                }
                cmdSetDrop(player, args[1], args[2]);
            }
            case "setmob" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("此指令僅限玩家使用").color(NamedTextColor.RED));
                    return;
                }
                if (args.length < 3) {
                    player.sendMessage(Component.text("用法: /wbadmin setmob <bossId> <mythicMobId>").color(NamedTextColor.RED));
                    return;
                }
                cmdSetMob(player, args[1], args[2]);
            }
            case "settings" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("此指令僅限玩家使用").color(NamedTextColor.RED));
                    return;
                }
                cmdSettings(player);
            }
            default -> sendAdminUsage(sender);
        }
    }

    private void cmdList(CommandSender sender) {
        Map<String, BossData> bosses = plugin.getConfigManager().getBosses();
        BossManager bossManager = plugin.getBossManager();

        sender.sendMessage(Component.text("===== 世界 Boss 列表 =====").color(NamedTextColor.GOLD));
        for (BossData boss : bosses.values()) {
            boolean active = bossManager.isBossActive(boss.getId());
            NamedTextColor statusColor = active ? NamedTextColor.GREEN : NamedTextColor.RED;
            String status = active ? "存活中" : "等待生成";

            sender.sendMessage(Component.text("  ")
                    .append(LegacyComponentSerializer.legacySection().deserialize(
                            ChatColor.translateAlternateColorCodes('&', boss.getDisplayName())))
                    .append(Component.text(" [" + status + "]").color(statusColor))
                    .append(Component.text(" (" + boss.getId() + ")").color(NamedTextColor.GRAY)));
        }
    }

    private void cmdInfo(CommandSender sender, String bossId) {
        BossData boss = plugin.getConfigManager().getBoss(bossId);
        if (boss == null) {
            sender.sendMessage(Component.text("找不到 Boss: " + bossId).color(NamedTextColor.RED));
            return;
        }

        BossManager bossManager = plugin.getBossManager();
        boolean active = bossManager.isBossActive(bossId);

        sender.sendMessage(Component.text("===== Boss 資訊 =====").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("名稱: ").color(NamedTextColor.WHITE)
                .append(LegacyComponentSerializer.legacySection().deserialize(
                        ChatColor.translateAlternateColorCodes('&', boss.getDisplayName()))));
        sender.sendMessage(Component.text("MythicMobs ID: ").color(NamedTextColor.WHITE)
                .append(Component.text(boss.getMythicMobId()).color(NamedTextColor.AQUA)));
        sender.sendMessage(Component.text("狀態: ").color(NamedTextColor.WHITE)
                .append(Component.text(active ? "存活中" : "等待生成")
                        .color(active ? NamedTextColor.GREEN : NamedTextColor.RED)));
        sender.sendMessage(Component.text("生成間隔: ").color(NamedTextColor.WHITE)
                .append(Component.text(boss.hasIntervalSpawn() ? boss.getSpawnInterval() + " 秒" : "未設定")
                        .color(NamedTextColor.AQUA)));
        sender.sendMessage(Component.text("消失逾時: ").color(NamedTextColor.WHITE)
                .append(Component.text(boss.getDespawnTimeout() + " 秒").color(NamedTextColor.AQUA)));
    }

    private void cmdSpawn(CommandSender sender, String bossId) {
        BossData boss = plugin.getConfigManager().getBoss(bossId);
        if (boss == null) {
            sender.sendMessage(Component.text("找不到 Boss: " + bossId).color(NamedTextColor.RED));
            return;
        }

        BossManager bossManager = plugin.getBossManager();
        if (bossManager.isBossActive(bossId)) {
            sender.sendMessage(Component.text("此 Boss 已經在場！").color(NamedTextColor.RED));
            return;
        }

        if (bossManager.spawnBoss(boss)) {
            bossManager.getSpawnScheduler().cancelAll(bossId);
            sender.sendMessage(Component.text("Boss ").color(NamedTextColor.GREEN)
                    .append(LegacyComponentSerializer.legacySection().deserialize(
                            ChatColor.translateAlternateColorCodes('&', boss.getDisplayName())))
                    .append(Component.text(" 已生成！").color(NamedTextColor.GREEN)));
            plugin.getHologramManager().onBossSpawn(bossId);
        } else {
            sender.sendMessage(Component.text("Boss 生成失敗！").color(NamedTextColor.RED));
        }
    }

    private void cmdDespawn(CommandSender sender, String bossId) {
        BossManager bossManager = plugin.getBossManager();
        if (!bossManager.isBossActive(bossId)) {
            sender.sendMessage(Component.text("此 Boss 不在場！").color(NamedTextColor.RED));
            return;
        }

        bossManager.despawnBoss(bossId, true);
        plugin.getDamageTracker().resetBoss(bossId);
        sender.sendMessage(Component.text("Boss 已強制消失！").color(NamedTextColor.GREEN));
    }

    private void cmdReload(CommandSender sender) {
        plugin.applyConfigChanges();
        sender.sendMessage(Component.text("配置已重新載入！").color(NamedTextColor.GREEN));
    }

    private void cmdDrops(Player player, String bossId) {
        BossData boss = plugin.getConfigManager().getBoss(bossId);
        if (boss == null) {
            player.sendMessage(Component.text("找不到 Boss: " + bossId).color(NamedTextColor.RED));
            return;
        }
        plugin.getLootGUI().openDropsGUI(player, bossId);
    }

    private void cmdSetDrop(Player player, String bossId, String chanceStr) {
        BossData boss = plugin.getConfigManager().getBoss(bossId);
        if (boss == null) {
            player.sendMessage(Component.text("找不到 Boss: " + bossId).color(NamedTextColor.RED));
            return;
        }

        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem.getType().isAir()) {
            player.sendMessage(Component.text("請手持一個物品！").color(NamedTextColor.RED));
            return;
        }

        double chance;
        try {
            chance = Double.parseDouble(chanceStr);
            if (chance < 0 || chance > 100) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("機率必須是 0-100 的數字").color(NamedTextColor.RED));
            return;
        }

        plugin.getLootConfig().saveDrop(bossId, handItem.clone(), chance);
        player.sendMessage(Component.text("已新增掉落物品（機率: " + chance + "%）").color(NamedTextColor.GREEN));
    }

    private void cmdSetMob(Player player, String bossId, String mythicMobId) {
        if (!plugin.getConfigManager().bossExists(bossId)) {
            player.sendMessage(Component.text("找不到 Boss: " + bossId).color(NamedTextColor.RED));
            return;
        }

        // Verify the MythicMob ID exists
        List<String> mobIds = plugin.getSettingsListener().getMythicMobIds();
        if (!mobIds.isEmpty() && !mobIds.contains(mythicMobId)) {
            player.sendMessage(Component.text("找不到 MythicMob ID: " + mythicMobId).color(NamedTextColor.RED));

            // Show similar IDs
            List<String> similar = mobIds.stream()
                    .filter(id -> id.toLowerCase().contains(mythicMobId.toLowerCase()))
                    .limit(10)
                    .toList();
            if (!similar.isEmpty()) {
                player.sendMessage(Component.text("你是否指的是:").color(NamedTextColor.YELLOW));
                for (String s : similar) {
                    player.sendMessage(Component.text("  - " + s).color(NamedTextColor.WHITE));
                }
            }
            return;
        }

        plugin.getConfigManager().setBossMythicMobId(bossId, mythicMobId);
        plugin.applyConfigChanges();
        player.sendMessage(Component.text("已設定 Boss " + bossId + " 的 MythicMob ID: " + mythicMobId)
                .color(NamedTextColor.GREEN));

        // Reopen settings GUI if the player was in it
        UUID uuid = player.getUniqueId();
        String lastBoss = plugin.getSettingsListener().getLastBossMenu(uuid);
        if (lastBoss != null && lastBoss.equals(bossId)) {
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                    plugin.getSettingsGUI().openBossMenu(player, bossId), 1L);
        }
    }

    private void cmdSettings(Player player) {
        plugin.getSettingsGUI().openMainMenu(player);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
    }

    private void sendPlayerUsage(CommandSender sender) {
        sender.sendMessage(Component.text("===== WorldBoss 指令 =====").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/wb list").color(NamedTextColor.WHITE)
                .append(Component.text(" - 顯示所有 Boss 狀態").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/wb info <bossId>").color(NamedTextColor.WHITE)
                .append(Component.text(" - 查看指定 Boss 資訊").color(NamedTextColor.GRAY)));
    }

    private void sendAdminUsage(CommandSender sender) {
        sender.sendMessage(Component.text("===== WorldBoss 管理指令 =====").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/wbadmin settings").color(NamedTextColor.WHITE)
                .append(Component.text(" - 開啟設定 GUI").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/wbadmin spawn <bossId>").color(NamedTextColor.WHITE)
                .append(Component.text(" - 生成 Boss").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/wbadmin despawn <bossId>").color(NamedTextColor.WHITE)
                .append(Component.text(" - 強制移除 Boss").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/wbadmin reload").color(NamedTextColor.WHITE)
                .append(Component.text(" - 重載配置").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/wbadmin drops <bossId>").color(NamedTextColor.WHITE)
                .append(Component.text(" - 開啟掉落物 GUI").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/wbadmin setdrop <bossId> <chance>").color(NamedTextColor.WHITE)
                .append(Component.text(" - 設定手持物品為掉落").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/wbadmin setmob <bossId> <mobId>").color(NamedTextColor.WHITE)
                .append(Component.text(" - 設定 MythicMob ID（Tab 補全）").color(NamedTextColor.GRAY)));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        List<String> bossIds = new ArrayList<>(plugin.getConfigManager().getBossIds());

        switch (command.getName().toLowerCase()) {
            case "wb" -> {
                if (args.length == 1) {
                    completions.addAll(List.of("list", "info"));
                } else if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
                    completions.addAll(bossIds);
                }
            }
            case "wbadmin" -> {
                if (args.length == 1) {
                    completions.addAll(List.of("spawn", "despawn", "reload", "drops", "setdrop", "setmob", "settings"));
                } else if (args.length == 2) {
                    String sub = args[0].toLowerCase();
                    if (!sub.equals("reload") && !sub.equals("settings")) {
                        completions.addAll(bossIds);
                    }
                } else if (args.length == 3 && args[0].equalsIgnoreCase("setmob")) {
                    // Tab complete MythicMob IDs
                    List<String> mobIds = plugin.getSettingsListener().getMythicMobIds();
                    completions.addAll(mobIds);
                }
            }
        }

        String input = args[args.length - 1].toLowerCase();
        return completions.stream().filter(s -> s.toLowerCase().startsWith(input)).toList();
    }
}
