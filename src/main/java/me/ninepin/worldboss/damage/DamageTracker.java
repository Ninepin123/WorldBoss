package me.ninepin.worldboss.damage;

import java.util.*;

public class DamageTracker {

    private final Map<String, Map<UUID, Double>> bossDamageMap = new HashMap<>();

    public void recordDamage(String bossId, UUID playerUUID, double damage) {
        bossDamageMap.computeIfAbsent(bossId, k -> new HashMap<>())
                .merge(playerUUID, damage, Double::sum);
    }

    public List<Map.Entry<UUID, Double>> getTopDamage(String bossId, int n) {
        Map<UUID, Double> damageMap = bossDamageMap.get(bossId);
        if (damageMap == null || damageMap.isEmpty()) return Collections.emptyList();

        return damageMap.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .limit(n)
                .toList();
    }

    public double getTotalDamage(String bossId, UUID playerUUID) {
        Map<UUID, Double> damageMap = bossDamageMap.get(bossId);
        if (damageMap == null) return 0;
        return damageMap.getOrDefault(playerUUID, 0.0);
    }

    public Set<UUID> getAttackers(String bossId) {
        Map<UUID, Double> damageMap = bossDamageMap.get(bossId);
        if (damageMap == null) return Collections.emptySet();
        return Collections.unmodifiableSet(damageMap.keySet());
    }

    public boolean hasAttacked(String bossId, UUID playerUUID) {
        Map<UUID, Double> damageMap = bossDamageMap.get(bossId);
        return damageMap != null && damageMap.containsKey(playerUUID);
    }

    public void resetBoss(String bossId) {
        bossDamageMap.remove(bossId);
    }

    public Map<UUID, Double> getAllDamage(String bossId) {
        Map<UUID, Double> damageMap = bossDamageMap.get(bossId);
        if (damageMap == null) return Collections.emptyMap();
        return new HashMap<>(damageMap);
    }
}
