package me.ninepin.worldboss.listener;

import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import me.ninepin.worldboss.WorldBoss;
import me.ninepin.worldboss.boss.BossData;
import me.ninepin.worldboss.boss.BossManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BossDeathListener implements Listener {

    private final WorldBoss plugin;

    public BossDeathListener(WorldBoss plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMythicMobDeath(MythicMobDeathEvent event) {
        Entity entity = event.getEntity();
        if (entity == null) return;

        BossManager bossManager = plugin.getBossManager();
        String bossId = bossManager.getBossIdByEntity(entity);
        if (bossId == null) return;

        // Cancel MythicMobs default drops, we handle loot ourselves
        event.setDrops(null);

        // Update history leaderboard with current damage data
        plugin.getDamageLeaderboard().updateHistory(bossId, plugin.getDamageTracker().getAllDamage(bossId));

        // Send Discord death notification
        try {
            BossData bossData = plugin.getConfigManager().getBoss(bossId);
            String bossDisplayName = bossData != null ? bossData.getDisplayName() : bossId;
            String topPlayers = buildTopPlayersString(bossId);
            plugin.getDiscordNotificationService().notifyBossDeath(bossId, bossDisplayName, topPlayers);
        } catch (Exception e) {
            plugin.getLogger().warning("發送 Discord 死亡通知時出錯: " + e.getMessage());
        }

        // Distribute rewards to top 3 damage dealers (reads tracker before reset)
        plugin.getRewardManager().distributeRewards(bossId);

        // Update hologram displays
        plugin.getHologramManager().onBossDeath(bossId);

        // Remove boss from active tracking (triggers next cycle countdown)
        bossManager.onBossDeath(bossId);

        // Reset real-time damage tracker
        plugin.getDamageTracker().resetBoss(bossId);
    }

    private String buildTopPlayersString(String bossId) {
        List<Map.Entry<UUID, Double>> top3 = plugin.getDamageTracker().getTopDamage(bossId, 3);
        if (top3.isEmpty()) return "無";

        List<String> parts = new ArrayList<>();
        for (Map.Entry<UUID, Double> entry : top3) {
            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            if (name == null) name = entry.getKey().toString().substring(0, 8);
            parts.add(name + " (" + entry.getValue().intValue() + ")");
        }
        return String.join(", ", parts);
    }
}
