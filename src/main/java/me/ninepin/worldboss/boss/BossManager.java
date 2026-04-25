package me.ninepin.worldboss.boss;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.ninepin.worldboss.WorldBoss;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.*;

public class BossManager {

    private final WorldBoss plugin;
    private final Map<String, UUID> activeBosses = new HashMap<>();
    private final Map<UUID, String> entityToBossId = new HashMap<>();
    private final Map<String, Long> lastAttackTime = new HashMap<>();
    private final Map<String, Boolean> hasBeenAttacked = new HashMap<>();

    public BossManager(WorldBoss plugin) {
        this.plugin = plugin;
    }

    public boolean spawnEntity(BossData bossData) {
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
        entityToBossId.put(entityUUID, bossData.getId());
        lastAttackTime.put(bossData.getId(), System.currentTimeMillis());
        hasBeenAttacked.put(bossData.getId(), false);
        return true;
    }

    public void removeEntity(String bossId) {
        UUID entityUUID = activeBosses.remove(bossId);
        if (entityUUID != null) {
            entityToBossId.remove(entityUUID);
            Entity entity = Bukkit.getEntity(entityUUID);
            if (entity != null) {
                entity.remove();
            }
        }
        lastAttackTime.remove(bossId);
        hasBeenAttacked.remove(bossId);
    }

    public void clearTracking(String bossId) {
        UUID entityUUID = activeBosses.remove(bossId);
        if (entityUUID != null) {
            entityToBossId.remove(entityUUID);
        }
        lastAttackTime.remove(bossId);
        hasBeenAttacked.remove(bossId);
    }

    public boolean isBossActive(String bossId) {
        return activeBosses.containsKey(bossId);
    }

    public String getBossIdByEntity(UUID entityUUID) {
        return entityToBossId.get(entityUUID);
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

    public Long getLastAttackTime(String bossId) {
        return lastAttackTime.get(bossId);
    }

    public Map<String, UUID> getActiveBosses() {
        return Collections.unmodifiableMap(activeBosses);
    }
}
