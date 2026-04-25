package me.ninepin.worldboss.loot;

import me.ninepin.worldboss.service.ConfigService;
import me.ninepin.worldboss.service.DamageService;
import me.ninepin.worldboss.service.DropService;
import me.ninepin.worldboss.boss.BossData;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RewardManager {

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacyAmpersand();
    private static final int TOP_N = 3;
    private final JavaPlugin plugin;
    private final ConfigService configService;
    private final DamageService damageService;
    private final DropService dropService;

    public RewardManager(JavaPlugin plugin, ConfigService configService,
                         DamageService damageService, DropService dropService) {
        this.plugin = plugin;
        this.configService = configService;
        this.damageService = damageService;
        this.dropService = dropService;
    }

    public void distributeRewards(String bossId) {
        BossData bossData = configService.getBoss(bossId);
        String bossName = bossData != null ? bossData.getDisplayName() : bossId;

        List<Map.Entry<UUID, Double>> top3 = damageService.getTopDamage(bossId, TOP_N);

        if (top3.isEmpty()) {
            plugin.getLogger().info("Boss " + bossId + " 無玩家造成傷害，不發放獎勵");
            return;
        }

        String rewardMsgRaw = configService.getRewardMessage();
        String noDropMsgRaw = configService.getNoDropMessage();

        int rank = 1;
        for (Map.Entry<UUID, Double> entry : top3) {
            UUID uuid = entry.getKey();
            Player player = Bukkit.getPlayer(uuid);

            if (player == null || !player.isOnline()) {
                plugin.getLogger().info("第 " + rank + " 名玩家不在線上，跳過獎勵發放: " + uuid);
                rank++;
                continue;
            }

            List<ItemStack> rewards = dropService.rollDrops(bossId);

            if (rewards.isEmpty()) {
                String raw = noDropMsgRaw
                        .replace("%boss_name%", bossName)
                        .replace("%rank%", String.valueOf(rank));
                player.sendMessage(SERIALIZER.deserialize(raw));
            } else {
                HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(
                        rewards.toArray(new ItemStack[0]));
                for (ItemStack leftover : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
                String raw = rewardMsgRaw
                        .replace("%boss_name%", bossName)
                        .replace("%rank%", String.valueOf(rank))
                        .replace("%count%", String.valueOf(rewards.size()));
                player.sendMessage(SERIALIZER.deserialize(raw));
            }
            rank++;
        }
    }
}
