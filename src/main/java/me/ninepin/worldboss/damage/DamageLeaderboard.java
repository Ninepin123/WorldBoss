package me.ninepin.worldboss.damage;

import me.ninepin.worldboss.WorldBoss;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class DamageLeaderboard {

    private final WorldBoss plugin;
    private final Map<String, Map<UUID, HistoryRecord>> historyMap = new HashMap<>();
    private final File dataFolder;

    public DamageLeaderboard(WorldBoss plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    public void loadAll() {
        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith("_history.yml"));
        if (files == null) return;

        for (File file : files) {
            String bossId = file.getName().replace("_history.yml", "");
            loadHistory(bossId);
        }
    }

    private void loadHistory(String bossId) {
        File file = new File(dataFolder, bossId + "_history.yml");
        if (!file.exists()) return;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection records = yaml.getConfigurationSection("records");
        if (records == null) return;

        Map<UUID, HistoryRecord> bossHistory = new HashMap<>();
        for (String uuidStr : records.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                String name = records.getString(uuidStr + ".name", "Unknown");
                double damage = records.getDouble(uuidStr + ".damage", 0);
                bossHistory.put(uuid, new HistoryRecord(name, damage));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("無法解析 UUID: " + uuidStr);
            }
        }
        historyMap.put(bossId, bossHistory);
    }

    public void saveHistory(String bossId) {
        Map<UUID, HistoryRecord> bossHistory = historyMap.get(bossId);
        if (bossHistory == null || bossHistory.isEmpty()) return;

        File file = new File(dataFolder, bossId + "_history.yml");
        saveHistoryToFile(file, bossHistory);
    }

    public void saveHistoryAsync(String bossId) {
        Map<UUID, HistoryRecord> bossHistory = historyMap.get(bossId);
        if (bossHistory == null || bossHistory.isEmpty()) return;

        Map<UUID, HistoryRecord> snapshot = new HashMap<>(bossHistory);
        File file = new File(dataFolder, bossId + "_history.yml");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> saveHistoryToFile(file, snapshot));
    }

    private void saveHistoryToFile(File file, Map<UUID, HistoryRecord> data) {
        YamlConfiguration yaml = new YamlConfiguration();

        for (Map.Entry<UUID, HistoryRecord> entry : data.entrySet()) {
            String path = "records." + entry.getKey().toString();
            yaml.set(path + ".name", entry.getValue().name());
            yaml.set(path + ".damage", entry.getValue().damage());
        }

        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "無法儲存歷史排行榜: " + file.getName(), e);
        }
    }

    public void saveAll() {
        for (String bossId : historyMap.keySet()) {
            saveHistory(bossId);
        }
    }

    public void updateHistory(String bossId, Map<UUID, Double> currentDamage) {
        Map<UUID, HistoryRecord> bossHistory = historyMap.computeIfAbsent(bossId, k -> new HashMap<>());

        for (Map.Entry<UUID, Double> entry : currentDamage.entrySet()) {
            UUID uuid = entry.getKey();
            double damage = entry.getValue();
            HistoryRecord existing = bossHistory.get(uuid);
            if (existing == null || damage > existing.damage()) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                String name = player.getName() != null ? player.getName() : uuid.toString();
                bossHistory.put(uuid, new HistoryRecord(name, damage));
            }
        }
        saveHistoryAsync(bossId);
    }

    public List<Map.Entry<UUID, HistoryRecord>> getTopHistory(String bossId, int n) {
        Map<UUID, HistoryRecord> bossHistory = historyMap.get(bossId);
        if (bossHistory == null || bossHistory.isEmpty()) return Collections.emptyList();

        return bossHistory.entrySet().stream()
                .sorted(Map.Entry.<UUID, HistoryRecord>comparingByValue(
                        Comparator.comparingDouble(HistoryRecord::damage).reversed()
                ))
                .limit(n)
                .toList();
    }

    public record HistoryRecord(String name, double damage) {}
}
