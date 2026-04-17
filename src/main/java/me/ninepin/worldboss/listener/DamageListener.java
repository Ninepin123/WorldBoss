package me.ninepin.worldboss.listener;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.ninepin.worldboss.WorldBoss;
import me.ninepin.worldboss.boss.BossManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Optional;

public class DamageListener implements Listener {

    private final WorldBoss plugin;

    public DamageListener(WorldBoss plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        BossManager bossManager = plugin.getBossManager();
        String bossId = bossManager.getBossIdByEntity(target);
        if (bossId == null) return;

        // Verify it's actually a MythicMob
        Optional<ActiveMob> mobOpt = MythicBukkit.inst().getMobManager().getActiveMob(target.getUniqueId());
        if (mobOpt.isEmpty()) return;

        Player attacker = resolveAttacker(event);
        if (attacker == null) return;

        double damage = event.getFinalDamage();
        plugin.getDamageTracker().recordDamage(bossId, attacker.getUniqueId(), damage);
        bossManager.markAttacked(bossId);
    }

    private Player resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            return player;
        }
        if (event.getDamager() instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player player) {
                return player;
            }
        }
        return null;
    }
}
