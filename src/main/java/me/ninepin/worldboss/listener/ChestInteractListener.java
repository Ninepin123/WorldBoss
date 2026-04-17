package me.ninepin.worldboss.listener;

import me.ninepin.worldboss.WorldBoss;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class ChestInteractListener implements Listener {

    private final WorldBoss plugin;

    public ChestInteractListener(WorldBoss plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        Location loc = block.getLocation();
        if (!plugin.getLootChestManager().isLootChest(loc)) return;

        String bossId = plugin.getLootChestManager().getBossIdByChest(loc);
        if (bossId == null) return;

        Player player = event.getPlayer();
        if (!plugin.getLootChestManager().canOpen(bossId, player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(Component.text("你沒有對此 Boss 造成傷害，無法開啟戰利品箱！")
                    .color(NamedTextColor.RED));
        }
    }
}
