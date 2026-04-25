package me.ninepin.worldboss.service;

import me.ninepin.worldboss.WorldBoss;
import me.ninepin.worldboss.boss.BossData;
import me.ninepin.worldboss.boss.BossManager;
import me.ninepin.worldboss.boss.SpawnScheduler;
import me.ninepin.worldboss.discord.DiscordNotificationService;
import me.ninepin.worldboss.hologram.HologramManager;
import me.ninepin.worldboss.loot.RewardManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class BossService {

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    private final WorldBoss plugin;
    private final ConfigService configService;
    private final DamageService damageService;
    private final BossManager bossManager;
    private final HologramManager hologramManager;
    private final DiscordNotificationService discordService;
    private final RewardManager rewardManager;
    private final SpawnScheduler spawnScheduler;
    private BukkitTask despawnCheckTask;

    public BossService(WorldBoss plugin, ConfigService configService, DamageService damageService,
                       BossManager bossManager, HologramManager hologramManager,
                       DiscordNotificationService discordService, RewardManager rewardManager) {
        this.plugin = plugin;
        this.configService = configService;
        this.damageService = damageService;
        this.bossManager = bossManager;
        this.hologramManager = hologramManager;
        this.discordService = discordService;
        this.rewardManager = rewardManager;
        this.spawnScheduler = new SpawnScheduler(plugin, this, configService, discordService);
    }

    public void init() {
        startDespawnChecker();
        spawnScheduler.startTimer();
    }

    public void shutdown() {
        if (despawnCheckTask != null) despawnCheckTask.cancel();
        spawnScheduler.stopTimer();
        for (String bossId : new HashSet<>(bossManager.getActiveBosses().keySet())) {
            bossManager.removeEntity(bossId);
        }
    }

    public boolean spawnBoss(BossData bossData) {
        if (!bossManager.spawnEntity(bossData)) return false;

        plugin.getLogger().info("Boss " + bossData.getDisplayName() + " 已生成於 " +
                bossData.getSpawnLocation().getWorld().getName() + " " +
                bossData.getSpawnLocation().getBlockX() + "," +
                bossData.getSpawnLocation().getBlockY() + "," +
                bossData.getSpawnLocation().getBlockZ());

        String raw = configService.getSpawnMessage()
                .replace("%boss_name%", bossData.getDisplayName());
        Bukkit.broadcast(SERIALIZER.deserialize(raw));

        try {
            discordService.notifyBossSpawn(bossData.getId(), bossData.getDisplayName());
        } catch (Exception e) {
            plugin.getLogger().warning("發送 Discord 生成通知時出錯: " + e.getMessage());
        }

        hologramManager.onBossSpawn(bossData.getId());
        return true;
    }

    public boolean spawnBoss(String bossId) {
        BossData bossData = configService.getBoss(bossId);
        if (bossData == null) return false;
        return spawnBoss(bossData);
    }

    public void despawnBoss(String bossId, boolean notify) {
        BossData bossData = configService.getBoss(bossId);

        if (notify) {
            if (bossData != null) {
                String raw = configService.getDespawnMessage()
                        .replace("%boss_name%", bossData.getDisplayName());
                Bukkit.broadcast(SERIALIZER.deserialize(raw));

                try {
                    discordService.notifyBossDespawn(bossId, bossData.getDisplayName());
                } catch (Exception e) {
                    plugin.getLogger().warning("發送 Discord 消失通知時出錯: " + e.getMessage());
                }
            } else {
                plugin.getLogger().warning("Boss " + bossId + " 的設定不存在，無法發送消失通知");
            }
        }

        bossManager.removeEntity(bossId);
        damageService.resetBoss(bossId);

        if (bossData != null) {
            spawnScheduler.startCountdown(bossData);
        }
    }

    public void onBossDeath(String bossId) {
        damageService.updateHistory(bossId, damageService.getAllDamage(bossId));

        BossData bossData = configService.getBoss(bossId);
        String bossDisplayName = bossData != null ? bossData.getDisplayName() : bossId;
        String topPlayers = buildTopPlayersString(bossId);

        String raw = configService.getDeathMessage()
                .replace("%boss_name%", bossDisplayName)
                .replace("%top_players%", topPlayers);
        Bukkit.broadcast(SERIALIZER.deserialize(raw));

        try {
            discordService.notifyBossDeath(bossId, bossDisplayName, topPlayers);
        } catch (Exception e) {
            plugin.getLogger().warning("發送 Discord 死亡通知時出錯: " + e.getMessage());
        }

        rewardManager.distributeRewards(bossId);
        hologramManager.onBossDeath(bossId);
        bossManager.clearTracking(bossId);
        damageService.resetBoss(bossId);

        if (bossData != null) {
            spawnScheduler.startCountdown(bossData);
        }
    }

    private String buildTopPlayersString(String bossId) {
        List<Map.Entry<UUID, Double>> top3 = damageService.getTopDamage(bossId, 3);
        if (top3.isEmpty()) return "無";

        List<String> parts = new ArrayList<>();
        for (Map.Entry<UUID, Double> entry : top3) {
            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            if (name == null) name = entry.getKey().toString().substring(0, 8);
            parts.add(name + " (" + entry.getValue().intValue() + ")");
        }
        return String.join(", ", parts);
    }

    public void startInitialCountdowns() {
        for (BossData bossData : configService.getBosses().values()) {
            if (!bossManager.isBossActive(bossData.getId())) {
                spawnScheduler.startCountdown(bossData);
            }
        }
    }

    public void cancelCountdowns(String bossId) {
        spawnScheduler.cancelBoss(bossId);
    }

    public void cancelAllCountdowns() {
        spawnScheduler.cancelAll();
    }

    public void clearRealtimeLeaderboard(String bossId) {
        damageService.clearRealtimeLeaderboard(bossId);
        hologramManager.refreshBossHolograms(bossId);
    }

    public void clearHistoryLeaderboard(String bossId) {
        damageService.clearHistoryLeaderboard(bossId);
        hologramManager.refreshBossHolograms(bossId);
    }

    public void deleteBoss(String bossId) {
        spawnScheduler.cancelBoss(bossId);
        bossManager.removeEntity(bossId);
        damageService.clearAllLeaderboardData(bossId);
        hologramManager.removeBossHolograms(bossId);

        configService.deleteBoss(bossId);
        reloadConfig();
    }

    public void reloadConfig() {
        configService.reloadConfigData();
        spawnScheduler.cancelAll();
        startInitialCountdowns();
        hologramManager.shutdown();
        hologramManager.init();
        discordService.reload();
    }

    public boolean isBossActive(String bossId) {
        return bossManager.isBossActive(bossId);
    }

    public String getBossIdByEntity(Entity entity) {
        return bossManager.getBossIdByEntity(entity);
    }

    public Map<String, UUID> getActiveBosses() {
        return bossManager.getActiveBosses();
    }

    public void markAttacked(String bossId) {
        bossManager.markAttacked(bossId);
    }

    public Long getNextSpawnMillis(String bossId) {
        return spawnScheduler.getNextSpawnMillis(bossId);
    }

    public Long getNextIntervalSpawnMillis(String bossId) {
        return spawnScheduler.getNextIntervalSpawnMillis(bossId);
    }

    public Long getNextScheduledSpawnMillis(String bossId) {
        return spawnScheduler.getNextScheduledSpawnMillis(bossId);
    }

    private void startDespawnChecker() {
        despawnCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                for (Map.Entry<String, UUID> entry : new HashMap<>(bossManager.getActiveBosses()).entrySet()) {
                    String bossId = entry.getKey();
                    BossData bossData = configService.getBoss(bossId);
                    if (bossData == null) continue;

                    Long lastAttack = bossManager.getLastAttackTime(bossId);
                    if (lastAttack == null) continue;

                    long elapsed = (now - lastAttack) / 1000;
                    if (elapsed >= bossData.getDespawnTimeout()) {
                        plugin.getLogger().info("Boss " + bossId + " 已超時無攻擊，自動消失");
                        despawnBoss(bossId, true);
                    }
                }
            }
        }.runTaskTimer(plugin, 100L, 100L);
    }
}
