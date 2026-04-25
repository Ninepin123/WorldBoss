package me.ninepin.worldboss.listener;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import me.ninepin.worldboss.service.BossService;
import me.ninepin.worldboss.service.DamageService;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Optional;

public class DamageListener implements Listener {

    private final BossService bossService;
    private final DamageService damageService;

    public DamageListener(BossService bossService, DamageService damageService) {
        this.bossService = bossService;
        this.damageService = damageService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        String bossId = bossService.getBossIdByEntity(target);
        if (bossId == null) return;

        Optional<ActiveMob> mobOpt = MythicBukkit.inst().getMobManager().getActiveMob(target.getUniqueId());
        if (mobOpt.isEmpty()) return;

        Player attacker = resolveAttacker(event);
        if (attacker == null) return;

        double damage = event.getFinalDamage();
        damageService.recordDamage(bossId, attacker.getUniqueId(), damage);
        bossService.markAttacked(bossId);
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
