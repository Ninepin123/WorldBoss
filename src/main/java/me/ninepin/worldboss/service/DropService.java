package me.ninepin.worldboss.service;

import me.ninepin.worldboss.loot.LootConfig;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class DropService {

    private final LootConfig lootConfig;

    public DropService(LootConfig lootConfig) {
        this.lootConfig = lootConfig;
    }

    public List<LootConfig.DropEntry> getDrops(String bossId) {
        return lootConfig.getDrops(bossId);
    }

    public void saveDrop(String bossId, ItemStack item, double chance) {
        lootConfig.saveDrop(bossId, item, chance);
    }

    public void removeDrop(String bossId, int index) {
        lootConfig.removeDrop(bossId, index);
    }

    public void updateChance(String bossId, int index, double chance) {
        lootConfig.updateChance(bossId, index, chance);
    }

    public List<ItemStack> rollDrops(String bossId) {
        return lootConfig.rollDrops(bossId);
    }

    public int resolveDropIndex(String bossId, int slot) {
        return lootConfig.resolveDropIndex(bossId, slot);
    }
}
