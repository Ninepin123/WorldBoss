package me.ninepin.worldboss.loot;

import me.ninepin.worldboss.WorldBoss;
import me.ninepin.worldboss.boss.BossData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RewardManager {

    private static final int TOP_N = 3;
    private final WorldBoss plugin;

    public RewardManager(WorldBoss plugin) {
        this.plugin = plugin;
    }

    public void distributeRewards(String bossId) {
        BossData bossData = plugin.getConfigManager().getBoss(bossId);
        String bossName = bossData != null
                ? ChatColor.translateAlternateColorCodes('&', bossData.getDisplayName())
                : bossId;

        List<Map.Entry<UUID, Double>> top3 = plugin.getDamageTracker().getTopDamage(bossId, TOP_N);

        if (top3.isEmpty()) {
            plugin.getLogger().info("Boss " + bossId + " 無玩家造成傷害，不發放獎勵");
            return;
        }

        String rewardMsgTemplate = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("reward.reward-message",
                        "&a你獲得了 %boss_name% 的第 %rank% 名獎勵！共 %count% 個物品"));
        String noDropMsgTemplate = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("reward.no-drop-message",
                        "&e你是 %boss_name% 的第 %rank% 名，但本次未擲中任何獎勵"));

        rewardMsgTemplate = insertBossName(rewardMsgTemplate, bossName);
        noDropMsgTemplate = insertBossName(noDropMsgTemplate, bossName);

        int rank = 1;
        for (Map.Entry<UUID, Double> entry : top3) {
            UUID uuid = entry.getKey();
            Player player = Bukkit.getPlayer(uuid);

            if (player == null || !player.isOnline()) {
                plugin.getLogger().info("第 " + rank + " 名玩家不在線上，跳過獎勵發放: " + uuid);
                rank++;
                continue;
            }

            List<ItemStack> rewards = plugin.getLootConfig().rollDrops(bossId);

            if (rewards.isEmpty()) {
                player.sendMessage(noDropMsgTemplate
                        .replace("%rank%", String.valueOf(rank)));
            } else {
                HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(
                        rewards.toArray(new ItemStack[0]));
                for (ItemStack leftover : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
                player.sendMessage(rewardMsgTemplate
                        .replace("%rank%", String.valueOf(rank))
                        .replace("%count%", String.valueOf(rewards.size())));
            }
            rank++;
        }
    }

    private String insertBossName(String template, String bossName) {
        int idx = template.indexOf("%boss_name%");
        if (idx < 0) return template;
        String restoreColor = "";
        for (int i = Math.min(idx - 2, template.length() - 2); i >= 0; i--) {
            if (template.charAt(i) == '§') {
                char code = Character.toLowerCase(template.charAt(i + 1));
                if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) {
                    restoreColor = "§" + code;
                    break;
                }
            }
        }
        return template.replace("%boss_name%", bossName + "§r" + restoreColor);
    }
}
