package me.ninepin.worldboss.hologram;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import me.ninepin.worldboss.boss.BossData;
import me.ninepin.worldboss.config.ConfigManager;
import me.ninepin.worldboss.damage.DamageLeaderboard;
import me.ninepin.worldboss.service.ConfigService;
import me.ninepin.worldboss.service.DamageService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.logging.Level;

public class HologramManager {

    private final JavaPlugin plugin;
    private final ConfigService configService;
    private final DamageService damageService;
    private BukkitTask updateTask;
    private final Set<String> activeRealtimeHolograms = new HashSet<>();

    public HologramManager(JavaPlugin plugin, ConfigService configService, DamageService damageService) {
        this.plugin = plugin;
        this.configService = configService;
        this.damageService = damageService;
    }

    public void init() {
        createAllHolograms();
        startRealtimeUpdater();
    }

    public void shutdown() {
        if (updateTask != null) updateTask.cancel();
        removeAllHolograms();
    }

    private void createAllHolograms() {
        for (String bossId : configService.getBosses().keySet()) {
            ConfigManager.LeaderboardConfig lbConfig = configService.getLeaderboardConfig(bossId);
            if (lbConfig == null) continue;

            if (lbConfig.historyEnabled && lbConfig.historyLocation != null && lbConfig.historyLocation.getWorld() != null) {
                try {
                    removeHologramIfExists(getHistoryName(bossId));
                    List<String> lines = buildHistoryLines(bossId, lbConfig);
                    DHAPI.createHologram(getHistoryName(bossId), lbConfig.historyLocation, lines);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "建立歷史排行榜 Hologram 失敗: " + bossId, e);
                }
            }

            if (lbConfig.realtimeEnabled && lbConfig.realtimeLocation != null && lbConfig.realtimeLocation.getWorld() != null) {
                try {
                    removeHologramIfExists(getRealtimeName(bossId));
                    List<String> lines = buildRealtimeLines(bossId, lbConfig);
                    DHAPI.createHologram(getRealtimeName(bossId), lbConfig.realtimeLocation, lines);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "建立即時排行榜 Hologram 失敗: " + bossId, e);
                }
            }
        }
    }

    public void onBossSpawn(String bossId) {
        ConfigManager.LeaderboardConfig lbConfig = configService.getLeaderboardConfig(bossId);
        if (lbConfig == null || !lbConfig.realtimeEnabled) return;

        activeRealtimeHolograms.add(bossId);
        refreshRealtimeHologram(bossId);
    }

    public void onBossDeath(String bossId) {
        ConfigManager.LeaderboardConfig lbConfig = configService.getLeaderboardConfig(bossId);

        if (lbConfig != null && lbConfig.historyEnabled) {
            refreshHistoryHologram(bossId);
        }

        activeRealtimeHolograms.remove(bossId);

        if (lbConfig != null && lbConfig.hideRealtimeOnDeath) {
            removeHologramIfExists(getRealtimeName(bossId));
        } else {
            refreshRealtimeHologram(bossId);
        }
    }

    private void startRealtimeUpdater() {
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (String bossId : new HashSet<>(activeRealtimeHolograms)) {
                    refreshRealtimeHologram(bossId);
                }
            }
        }.runTaskTimer(plugin, 40L, 40L);
    }

    private void refreshRealtimeHologram(String bossId) {
        ConfigManager.LeaderboardConfig lbConfig = configService.getLeaderboardConfig(bossId);
        if (lbConfig == null) return;

        String holoName = getRealtimeName(bossId);
        if (!hologramExists(holoName)) return;

        try {
            List<String> lines = buildRealtimeLines(bossId, lbConfig);
            Hologram holo = DHAPI.getHologram(holoName);
            if (holo != null) {
                DHAPI.setHologramLines(holo, lines);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.FINE, "更新即時排行榜失敗: " + bossId, e);
        }
    }

    private void refreshHistoryHologram(String bossId) {
        ConfigManager.LeaderboardConfig lbConfig = configService.getLeaderboardConfig(bossId);
        if (lbConfig == null) return;

        String holoName = getHistoryName(bossId);
        try {
            removeHologramIfExists(holoName);
            List<String> lines = buildHistoryLines(bossId, lbConfig);
            DHAPI.createHologram(holoName, lbConfig.historyLocation, lines);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "更新歷史排行榜失敗: " + bossId, e);
        }
    }

    private List<String> buildRealtimeLines(String bossId, ConfigManager.LeaderboardConfig lbConfig) {
        List<String> lines = new ArrayList<>();
        lines.add(getBossDisplayName(bossId) + " " + lbConfig.realtimeTitle);

        List<Map.Entry<UUID, Double>> top = damageService.getTopDamage(bossId, lbConfig.realtimeDisplayCount);

        if (top.isEmpty()) {
            lines.add("&7尚無傷害記錄");
        } else {
            int rank = 1;
            for (Map.Entry<UUID, Double> entry : top) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
                String name = player.getName() != null ? player.getName() : entry.getKey().toString().substring(0, 8);
                String line = lbConfig.realtimeFormat
                        .replace("%rank%", String.valueOf(rank))
                        .replace("%player%", name)
                        .replace("%damage%", formatDamage(entry.getValue()));
                lines.add(line);
                rank++;
            }
        }
        return lines;
    }

    private List<String> buildHistoryLines(String bossId, ConfigManager.LeaderboardConfig lbConfig) {
        List<String> lines = new ArrayList<>();
        lines.add(getBossDisplayName(bossId) + " " + lbConfig.historyTitle);

        List<Map.Entry<UUID, DamageLeaderboard.HistoryRecord>> top =
                damageService.getTopHistory(bossId, lbConfig.historyDisplayCount);

        if (top.isEmpty()) {
            lines.add("&7尚無歷史記錄");
        } else {
            int rank = 1;
            for (Map.Entry<UUID, DamageLeaderboard.HistoryRecord> entry : top) {
                String name = entry.getValue().name();
                String line = lbConfig.historyFormat
                        .replace("%rank%", String.valueOf(rank))
                        .replace("%player%", name)
                        .replace("%damage%", formatDamage(entry.getValue().damage()));
                lines.add(line);
                rank++;
            }
        }
        return lines;
    }

    private String formatDamage(double damage) {
        if (damage >= 1_000_000) {
            return String.format("%.1fM", damage / 1_000_000);
        } else if (damage >= 1_000) {
            return String.format("%.1fK", damage / 1_000);
        }
        return String.format("%.0f", damage);
    }

    private void removeHologramIfExists(String name) {
        try {
            if (hologramExists(name)) {
                DHAPI.removeHologram(name);
            }
        } catch (Exception ignored) {}
    }

    private boolean hologramExists(String name) {
        try {
            return DHAPI.getHologram(name) != null;
        } catch (Exception e) {
            return false;
        }
    }

    private void removeAllHolograms() {
        for (String bossId : configService.getBosses().keySet()) {
            removeHologramIfExists(getRealtimeName(bossId));
            removeHologramIfExists(getHistoryName(bossId));
        }
        activeRealtimeHolograms.clear();
    }

    private String getRealtimeName(String bossId) {
        return "wb_" + bossId + "_realtime";
    }

    private String getHistoryName(String bossId) {
        return "wb_" + bossId + "_history";
    }

    private String getBossDisplayName(String bossId) {
        BossData boss = configService.getBoss(bossId);
        return boss != null ? boss.getDisplayName() : bossId;
    }
}
