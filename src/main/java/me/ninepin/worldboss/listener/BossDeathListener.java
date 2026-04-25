package me.ninepin.worldboss.listener;

import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import me.ninepin.worldboss.service.BossService;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class BossDeathListener implements Listener {

    private final BossService bossService;

    public BossDeathListener(BossService bossService) {
        this.bossService = bossService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMythicMobDeath(MythicMobDeathEvent event) {
        Entity entity = event.getEntity();
        if (entity == null) return;

        String bossId = bossService.getBossIdByEntity(entity);
        if (bossId == null) return;

        event.setDrops(null);
        bossService.onBossDeath(bossId);
    }
}
