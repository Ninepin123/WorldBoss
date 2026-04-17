package me.ninepin.worldboss.damage;

import java.util.*;

public class DamageTracker {

    private final Map<String, Map<UUID, Double>> bossDamageMap = new HashMap<>();
    private final Map<String, Set<UUID>> bossAttackers = new HashMap<>();

    public void recordDamage(String bossId, UUID playerUUID, double damage) {
        bossDamageMap.computeIfAbsent(bossId, k -> new HashMap<>())
                .merge(playerUUID, damage, Double::sum);
        bossAttackers.computeIfAbsent(bossId, k -> new HashSet<>()).add(playerUUID);
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
        return bossAttackers.getOrDefault(bossId, Collections.emptySet());
    }

    public boolean hasAttacked(String bossId, UUID playerUUID) {
        Set<UUID> attackers = bossAttackers.get(bossId);
        return attackers != null && attackers.contains(playerUUID);
    }

    public void resetBoss(String bossId) {
        bossDamageMap.remove(bossId);
        bossAttackers.remove(bossId);
    }

    public Map<UUID, Double> getAllDamage(String bossId) {
        Map<UUID, Double> damageMap = bossDamageMap.get(bossId);
        if (damageMap == null) return Collections.emptyMap();
        return new HashMap<>(damageMap);
    }
}
