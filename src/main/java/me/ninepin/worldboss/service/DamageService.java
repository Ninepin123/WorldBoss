package me.ninepin.worldboss.service;

import me.ninepin.worldboss.damage.DamageLeaderboard;
import me.ninepin.worldboss.damage.DamageTracker;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class DamageService {

    private final DamageTracker tracker;
    private final DamageLeaderboard leaderboard;

    public DamageService(DamageTracker tracker, DamageLeaderboard leaderboard) {
        this.tracker = tracker;
        this.leaderboard = leaderboard;
    }

    public void recordDamage(String bossId, UUID playerUUID, double damage) {
        tracker.recordDamage(bossId, playerUUID, damage);
    }

    public List<Map.Entry<UUID, Double>> getTopDamage(String bossId, int n) {
        return tracker.getTopDamage(bossId, n);
    }

    public double getTotalDamage(String bossId, UUID playerUUID) {
        return tracker.getTotalDamage(bossId, playerUUID);
    }

    public Set<UUID> getAttackers(String bossId) {
        return tracker.getAttackers(bossId);
    }

    public boolean hasAttacked(String bossId, UUID playerUUID) {
        return tracker.hasAttacked(bossId, playerUUID);
    }

    public void resetBoss(String bossId) {
        tracker.resetBoss(bossId);
    }

    public void clearRealtimeLeaderboard(String bossId) {
        tracker.resetBoss(bossId);
    }

    public void clearHistoryLeaderboard(String bossId) {
        leaderboard.clearHistory(bossId);
    }

    public void clearAllLeaderboardData(String bossId) {
        clearRealtimeLeaderboard(bossId);
        clearHistoryLeaderboard(bossId);
    }

    public Map<UUID, Double> getAllDamage(String bossId) {
        return tracker.getAllDamage(bossId);
    }

    public void updateHistory(String bossId, Map<UUID, Double> currentDamage) {
        leaderboard.updateHistory(bossId, currentDamage);
    }

    public List<Map.Entry<UUID, DamageLeaderboard.HistoryRecord>> getTopHistory(String bossId, int n) {
        return leaderboard.getTopHistory(bossId, n);
    }

    public void saveAllHistory() {
        leaderboard.saveAll();
    }

    public void loadAllHistory() {
        leaderboard.loadAll();
    }
}
