package me.ninepin.worldboss.config;

import me.ninepin.worldboss.WorldBoss;
import me.ninepin.worldboss.boss.BossData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private final WorldBoss plugin;
    private FileConfiguration config;
    private final Map<String, BossData> bosses = new HashMap<>();
    private final Map<String, LeaderboardConfig> leaderboardConfigs = new HashMap<>();

    public ConfigManager(WorldBoss plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();

        loadBosses();
        loadLeaderboard();
    }

    public void reload() {
        bosses.clear();
        leaderboardConfigs.clear();
        load();
    }

    private void loadBosses() {
        ConfigurationSection bossSection = config.getConfigurationSection("boss-settings.bosses");
        if (bossSection == null) return;

        for (String bossId : bossSection.getKeys(false)) {
            ConfigurationSection section = bossSection.getConfigurationSection(bossId);
            if (section == null) continue;

            String mythicMobId = section.getString("mythicmob-id");
            String displayName = section.getString("display-name", "&cBoss");

            ConfigurationSection locSection = section.getConfigurationSection("spawn-location");
            Location spawnLoc = parseLocation(locSection);
            if (spawnLoc == null || spawnLoc.getWorld() == null) {
                plugin.getLogger().warning("Boss " + bossId + " 的生成座標無效，已跳過");
                continue;
            }

            ConfigurationSection spawnSection = section.getConfigurationSection("spawn");
            int interval = 0;
            List<BossData.ScheduledTime> scheduledTimes = new ArrayList<>();

            if (spawnSection != null) {
                interval = spawnSection.getInt("interval", 0);
                List<String> scheduleStrings = spawnSection.getStringList("schedule");
                for (String scheduleStr : scheduleStrings) {
                    BossData.ScheduledTime st = parseScheduledTime(scheduleStr);
                    if (st != null) {
                        scheduledTimes.add(st);
                    }
                }
            }

            int despawnTimeout = section.getInt("despawn-timeout", 180);

            BossData bossData = new BossData(bossId, mythicMobId, displayName, spawnLoc,
                    interval, scheduledTimes, despawnTimeout);
            bosses.put(bossId, bossData);
        }
    }

    private void loadLeaderboard() {
        ConfigurationSection lbSection = config.getConfigurationSection("leaderboard");
        if (lbSection == null) return;

        for (String bossId : lbSection.getKeys(false)) {
            ConfigurationSection section = lbSection.getConfigurationSection(bossId);
            if (section == null) continue;

            LeaderboardConfig lbConfig = new LeaderboardConfig();

            ConfigurationSection realtime = section.getConfigurationSection("realtime");
            if (realtime != null) {
                lbConfig.realtimeEnabled = realtime.getBoolean("enabled", true);
                lbConfig.realtimeLocation = parseLocation(realtime.getConfigurationSection("location"));
                lbConfig.realtimeDisplayCount = realtime.getInt("display-count", 10);
                lbConfig.realtimeTitle = realtime.getString("title", "&6&l即時傷害排行榜");
                lbConfig.realtimeFormat = realtime.getString("format", "&e#%rank% &f%player% &7- &c%damage%");
            }

            ConfigurationSection history = section.getConfigurationSection("history");
            if (history != null) {
                lbConfig.historyEnabled = history.getBoolean("enabled", true);
                lbConfig.historyLocation = parseLocation(history.getConfigurationSection("location"));
                lbConfig.historyDisplayCount = history.getInt("display-count", 10);
                lbConfig.historyTitle = history.getString("title", "&b&l歷史最高傷害");
                lbConfig.historyFormat = history.getString("format", "&e#%rank% &f%player% &7- &c%damage%");
            }

            lbConfig.hideRealtimeOnDeath = section.getBoolean("hide-realtime-on-death", false);
            leaderboardConfigs.put(bossId, lbConfig);
        }
    }

    private Location parseLocation(ConfigurationSection section) {
        if (section == null) return null;
        String worldName = section.getString("world");
        if (worldName == null) return null;
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        return new Location(Bukkit.getWorld(worldName), x, y, z);
    }

    private BossData.ScheduledTime parseScheduledTime(String str) {
        try {
            String[] parts = str.trim().split("\\s+");
            DayOfWeek day = DayOfWeek.valueOf(parts[0].toUpperCase());
            String[] timeParts = parts[1].split(":");
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);
            return new BossData.ScheduledTime(day, hour, minute);
        } catch (Exception e) {
            plugin.getLogger().warning("無法解析排程時間: " + str);
            return null;
        }
    }

    // === Getters ===

    public Map<String, BossData> getBosses() {
        return Collections.unmodifiableMap(bosses);
    }

    public BossData getBoss(String bossId) {
        return bosses.get(bossId);
    }

    public LeaderboardConfig getLeaderboardConfig(String bossId) {
        return leaderboardConfigs.get(bossId);
    }

    public List<String> getBossIds() {
        return new ArrayList<>(bosses.keySet());
    }

    public boolean bossExists(String bossId) {
        return config.contains("boss-settings.bosses." + bossId);
    }

    public List<Integer> getCountdownTimes() {
        List<Integer> times = config.getIntegerList("broadcast.countdown-times");
        return times.isEmpty() ? List.of(30, 20, 10) : times;
    }

    public String getCountdownMessage() {
        return config.getString("broadcast.countdown-message",
                "&6&l【世界Boss】&e%boss_name% &f將在 &c%time% &f後出現！");
    }

    public String getSpawnMessage() {
        return config.getString("broadcast.spawn-message",
                "&6&l【世界Boss】&e%boss_name% &f已降臨！快去挑戰吧！");
    }

    public String getDespawnMessage() {
        return config.getString("broadcast.despawn-message",
                "&6&l【世界Boss】&e%boss_name% &f因無人攻擊已消失！");
    }

    public String getDeathMessage() {
        return config.getString("broadcast.death-message",
                "&6&l【世界Boss】&e%boss_name% &f已被討伐！前三名： &a%top_players%");
    }

    public String getRewardMessage() {
        return config.getString("reward.reward-message",
                "&a你獲得了 %boss_name% 的第 %rank% 名獎勵！共 %count% 個物品");
    }

    public String getNoDropMessage() {
        return config.getString("reward.no-drop-message",
                "&e你是 %boss_name% 的第 %rank% 名，但本次未擲中任何獎勵");
    }

    // === Save Methods ===

    public void setBossMythicMobId(String bossId, String mythicMobId) {
        config.set("boss-settings.bosses." + bossId + ".mythicmob-id", mythicMobId);
        plugin.saveConfig();
    }

    public void setBossDisplayName(String bossId, String displayName) {
        config.set("boss-settings.bosses." + bossId + ".display-name", displayName);
        plugin.saveConfig();
    }

    public void setBossSpawnLocation(String bossId, Location loc) {
        String base = "boss-settings.bosses." + bossId + ".spawn-location";
        config.set(base + ".world", loc.getWorld().getName());
        config.set(base + ".x", loc.getX());
        config.set(base + ".y", loc.getY());
        config.set(base + ".z", loc.getZ());
        plugin.saveConfig();
    }

    public void setBossSpawnInterval(String bossId, int seconds) {
        config.set("boss-settings.bosses." + bossId + ".spawn.interval", seconds);
        plugin.saveConfig();
    }

    public void setBossScheduledTimes(String bossId, List<String> scheduleStrings) {
        config.set("boss-settings.bosses." + bossId + ".spawn.schedule", scheduleStrings);
        plugin.saveConfig();
    }

    public void addBossScheduledTime(String bossId, String scheduleString) {
        String path = "boss-settings.bosses." + bossId + ".spawn.schedule";
        List<String> current = config.getStringList(path);
        current = new ArrayList<>(current);
        current.add(scheduleString);
        config.set(path, current);
        plugin.saveConfig();
    }

    public void clearBossScheduledTimes(String bossId) {
        config.set("boss-settings.bosses." + bossId + ".spawn.schedule", new ArrayList<String>());
        plugin.saveConfig();
    }

    public void setBossDespawnTimeout(String bossId, int seconds) {
        config.set("boss-settings.bosses." + bossId + ".despawn-timeout", seconds);
        plugin.saveConfig();
    }

    public void setRealtimeLBLocation(String bossId, Location loc) {
        String base = "leaderboard." + bossId + ".realtime.location";
        config.set(base + ".world", loc.getWorld().getName());
        config.set(base + ".x", loc.getX());
        config.set(base + ".y", loc.getY());
        config.set(base + ".z", loc.getZ());
        plugin.saveConfig();
    }

    public void setHistoryLBLocation(String bossId, Location loc) {
        String base = "leaderboard." + bossId + ".history.location";
        config.set(base + ".world", loc.getWorld().getName());
        config.set(base + ".x", loc.getX());
        config.set(base + ".y", loc.getY());
        config.set(base + ".z", loc.getZ());
        plugin.saveConfig();
    }

    public void deleteBoss(String bossId) {
        config.set("boss-settings.bosses." + bossId, null);
        config.set("leaderboard." + bossId, null);
        plugin.saveConfig();

        File dropsFile = new File(plugin.getDataFolder(), "drops/" + bossId + ".yml");
        if (dropsFile.exists()) {
            dropsFile.delete();
        }

        File historyFile = new File(plugin.getDataFolder(), "data/" + bossId + "_history.yml");
        if (historyFile.exists()) {
            historyFile.delete();
        }
    }

    public void createNewBoss(String bossId, String mythicMobId, String displayName, Location spawnLoc) {
        String base = "boss-settings.bosses." + bossId;
        config.set(base + ".mythicmob-id", mythicMobId);
        config.set(base + ".display-name", displayName);
        config.set(base + ".spawn-location.world", spawnLoc.getWorld().getName());
        config.set(base + ".spawn-location.x", spawnLoc.getX());
        config.set(base + ".spawn-location.y", spawnLoc.getY());
        config.set(base + ".spawn-location.z", spawnLoc.getZ());
        config.set(base + ".spawn.interval", 3600);
        config.set(base + ".spawn.schedule", new ArrayList<String>());
        config.set(base + ".despawn-timeout", 180);

        // Default leaderboard config
        String lbBase = "leaderboard." + bossId;
        config.set(lbBase + ".realtime.enabled", true);
        config.set(lbBase + ".realtime.location.world", spawnLoc.getWorld().getName());
        config.set(lbBase + ".realtime.location.x", spawnLoc.getX() + 5);
        config.set(lbBase + ".realtime.location.y", spawnLoc.getY() + 6);
        config.set(lbBase + ".realtime.location.z", spawnLoc.getZ());
        config.set(lbBase + ".realtime.display-count", 10);
        config.set(lbBase + ".realtime.title", "&6&l即時傷害排行榜");
        config.set(lbBase + ".realtime.format", "&e#%rank% &f%player% &7- &c%damage%");

        config.set(lbBase + ".history.enabled", true);
        config.set(lbBase + ".history.location.world", spawnLoc.getWorld().getName());
        config.set(lbBase + ".history.location.x", spawnLoc.getX() + 10);
        config.set(lbBase + ".history.location.y", spawnLoc.getY() + 6);
        config.set(lbBase + ".history.location.z", spawnLoc.getZ());
        config.set(lbBase + ".history.display-count", 10);
        config.set(lbBase + ".history.title", "&b&l歷史最高傷害");
        config.set(lbBase + ".history.format", "&e#%rank% &f%player% &7- &c%damage%");

        config.set(lbBase + ".hide-realtime-on-death", false);

        plugin.saveConfig();
    }

    public static class LeaderboardConfig {
        public boolean realtimeEnabled = true;
        public Location realtimeLocation;
        public int realtimeDisplayCount = 10;
        public String realtimeTitle = "&6&l即時傷害排行榜";
        public String realtimeFormat = "&e#%rank% &f%player% &7- &c%damage%";

        public boolean historyEnabled = true;
        public Location historyLocation;
        public int historyDisplayCount = 10;
        public String historyTitle = "&b&l歷史最高傷害";
        public String historyFormat = "&e#%rank% &f%player% &7- &c%damage%";

        public boolean hideRealtimeOnDeath = false;
    }
}
