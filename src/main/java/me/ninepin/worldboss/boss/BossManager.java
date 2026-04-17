package me.ninepin.worldboss.boss;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.ninepin.worldboss.WorldBoss;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class BossManager {

    private final WorldBoss plugin;
    private final SpawnScheduler spawnScheduler;
    private final Map<String, UUID> activeBosses = new HashMap<>();
    private final Map<String, Long> lastAttackTime = new HashMap<>();
    private final Map<String, Boolean> hasBeenAttacked = new HashMap<>();
    private BukkitTask despawnCheckTask;

    public BossManager(WorldBoss plugin) {
        this.plugin = plugin;
        this.spawnScheduler = new SpawnScheduler(plugin, this);
    }

    public void init() {
        startDespawnChecker();
    }

    public void shutdown() {
        if (despawnCheckTask != null) despawnCheckTask.cancel();
        spawnScheduler.cancelAll();
        for (String bossId : new HashSet<>(activeBosses.keySet())) {
            despawnBoss(bossId, false);
        }
    }

    public boolean spawnBoss(BossData bossData) {
        if (activeBosses.containsKey(bossData.getId())) return false;

        Location loc = bossData.getSpawnLocation();
        if (loc.getWorld() == null) return false;

        ActiveMob mob = MythicBukkit.inst().getMobManager().spawnMob(bossData.getMythicMobId(), loc);
        if (mob == null) {
            plugin.getLogger().warning("無法生成 MythicMob: " + bossData.getMythicMobId());
            return false;
        }

        if (mob.getEntity() == null) return false;

        UUID entityUUID = mob.getEntity().getUniqueId();
        activeBosses.put(bossData.getId(), entityUUID);
        lastAttackTime.put(bossData.getId(), System.currentTimeMillis());
        hasBeenAttacked.put(bossData.getId(), false);

        plugin.getLogger().info("Boss " + bossData.getDisplayName() + " 已生成於 " +
                loc.getWorld().getName() + " " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());

        String spawnBroadcast = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfigManager().getSpawnMessage()
                        .replace("%boss_name%", ChatColor.translateAlternateColorCodes('&', bossData.getDisplayName())));
        Bukkit.broadcastMessage(spawnBroadcast);

        try {
            plugin.getDiscordNotificationService().notifyBossSpawn(bossData.getId(), bossData.getDisplayName());
        } catch (Exception e) {
            plugin.getLogger().warning("發送 Discord 生成通知時出錯: " + e.getMessage());
        }

        return true;
    }

    public void despawnBoss(String bossId, boolean startNextCycle) {
        UUID entityUUID = activeBosses.remove(bossId);
        if (entityUUID != null) {
            Entity entity = Bukkit.getEntity(entityUUID);
            if (entity != null) {
                entity.remove();
            }
        }
        lastAttackTime.remove(bossId);
        hasBeenAttacked.remove(bossId);

        if (startNextCycle) {
            BossData bossData = plugin.getConfigManager().getBoss(bossId);
            if (bossData != null) {
                spawnScheduler.startCountdown(bossData);
            }
        }
    }

    public boolean isBossActive(String bossId) {
        return activeBosses.containsKey(bossId);
    }

    public String getBossIdByEntity(UUID entityUUID) {
        for (Map.Entry<String, UUID> entry : activeBosses.entrySet()) {
            if (entry.getValue().equals(entityUUID)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public String getBossIdByEntity(Entity entity) {
        return getBossIdByEntity(entity.getUniqueId());
    }

    public void markAttacked(String bossId) {
        lastAttackTime.put(bossId, System.currentTimeMillis());
        hasBeenAttacked.put(bossId, true);
    }

    public boolean hasBeenAttacked(String bossId) {
        return hasBeenAttacked.getOrDefault(bossId, false);
    }

    public void onBossDeath(String bossId) {
        activeBosses.remove(bossId);
        lastAttackTime.remove(bossId);
        hasBeenAttacked.remove(bossId);

        BossData bossData = plugin.getConfigManager().getBoss(bossId);
        if (bossData != null) {
            spawnScheduler.startCountdown(bossData);
        }
    }

    public SpawnScheduler getSpawnScheduler() {
        return spawnScheduler;
    }

    public Map<String, UUID> getActiveBosses() {
        return Collections.unmodifiableMap(activeBosses);
    }

    public void startInitialCountdowns() {
        for (BossData bossData : plugin.getConfigManager().getBosses().values()) {
            if (!activeBosses.containsKey(bossData.getId())) {
                spawnScheduler.startCountdown(bossData);
            }
        }
    }

    private void startDespawnChecker() {
        despawnCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                for (Map.Entry<String, UUID> entry : new HashMap<>(activeBosses).entrySet()) {
                    String bossId = entry.getKey();
                    BossData bossData = plugin.getConfigManager().getBoss(bossId);
                    if (bossData == null) continue;

                    Long lastAttack = lastAttackTime.get(bossId);
                    if (lastAttack == null) continue;

                    long elapsed = (now - lastAttack) / 1000;
                    if (elapsed >= bossData.getDespawnTimeout()) {
                        plugin.getLogger().info("Boss " + bossId + " 已超時無攻擊，自動消失");
                        String despawnBroadcast = ChatColor.translateAlternateColorCodes('&',
                                plugin.getConfigManager().getDespawnMessage()
                                        .replace("%boss_name%", ChatColor.translateAlternateColorCodes('&', bossData.getDisplayName())));
                        Bukkit.broadcastMessage(despawnBroadcast);
                        try {
                            plugin.getDiscordNotificationService().notifyBossDespawn(bossId, bossData.getDisplayName());
                        } catch (Exception e) {
                            plugin.getLogger().warning("發送 Discord 消失通知時出錯: " + e.getMessage());
                        }
                        despawnBoss(bossId, true);
                        plugin.getDamageTracker().resetBoss(bossId);
                    }
                }
            }
        }.runTaskTimer(plugin, 100L, 100L); // 5 seconds
    }
}
