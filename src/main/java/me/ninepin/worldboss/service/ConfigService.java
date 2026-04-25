package me.ninepin.worldboss.service;

import me.ninepin.worldboss.WorldBoss;
import me.ninepin.worldboss.boss.BossData;
import me.ninepin.worldboss.config.ConfigManager;
import org.bukkit.Location;

import java.util.List;
import java.util.Map;

public class ConfigService {

    private final WorldBoss plugin;
    private final ConfigManager configManager;

    public ConfigService(WorldBoss plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void load() {
        configManager.load();
    }

    public void reloadConfigData() {
        configManager.reload();
    }

    public Map<String, BossData> getBosses() {
        return configManager.getBosses();
    }

    public BossData getBoss(String bossId) {
        return configManager.getBoss(bossId);
    }

    public List<String> getBossIds() {
        return configManager.getBossIds();
    }

    public boolean bossExists(String bossId) {
        return configManager.bossExists(bossId);
    }

    public ConfigManager.LeaderboardConfig getLeaderboardConfig(String bossId) {
        return configManager.getLeaderboardConfig(bossId);
    }

    public List<Integer> getCountdownTimes() {
        return configManager.getCountdownTimes();
    }

    public String getCountdownMessage() {
        return configManager.getCountdownMessage();
    }

    public String getSpawnMessage() {
        return configManager.getSpawnMessage();
    }

    public String getDespawnMessage() {
        return configManager.getDespawnMessage();
    }

    public String getDeathMessage() {
        return configManager.getDeathMessage();
    }

    public String getRewardMessage() {
        return configManager.getRewardMessage();
    }

    public String getNoDropMessage() {
        return configManager.getNoDropMessage();
    }

    public void setBossMythicMobId(String bossId, String mythicMobId) {
        configManager.setBossMythicMobId(bossId, mythicMobId);
    }

    public void setBossDisplayName(String bossId, String displayName) {
        configManager.setBossDisplayName(bossId, displayName);
    }

    public void setBossSpawnLocation(String bossId, Location loc) {
        configManager.setBossSpawnLocation(bossId, loc);
    }

    public void setBossSpawnInterval(String bossId, int seconds) {
        configManager.setBossSpawnInterval(bossId, seconds);
    }

    public void addBossScheduledTime(String bossId, String scheduleString) {
        configManager.addBossScheduledTime(bossId, scheduleString);
    }

    public void clearBossScheduledTimes(String bossId) {
        configManager.clearBossScheduledTimes(bossId);
    }

    public void setBossDespawnTimeout(String bossId, int seconds) {
        configManager.setBossDespawnTimeout(bossId, seconds);
    }

    public void setRealtimeLBLocation(String bossId, Location loc) {
        configManager.setRealtimeLBLocation(bossId, loc);
    }

    public void setHistoryLBLocation(String bossId, Location loc) {
        configManager.setHistoryLBLocation(bossId, loc);
    }

    public void createNewBoss(String bossId, String mythicMobId, String displayName, Location loc) {
        configManager.createNewBoss(bossId, mythicMobId, displayName, loc);
    }

    public void deleteBoss(String bossId) {
        configManager.deleteBoss(bossId);
    }
}
