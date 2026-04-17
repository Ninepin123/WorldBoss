package me.ninepin.worldboss.loot;

import me.ninepin.worldboss.WorldBoss;
import me.ninepin.worldboss.util.SerializationUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class LootConfig {

    private final WorldBoss plugin;
    private final File dropsFolder;

    public LootConfig(WorldBoss plugin) {
        this.plugin = plugin;
        this.dropsFolder = new File(plugin.getDataFolder(), "drops");
        if (!dropsFolder.exists()) {
            dropsFolder.mkdirs();
        }
    }

    public List<DropEntry> getDrops(String bossId) {
        File file = new File(dropsFolder, bossId + ".yml");
        if (!file.exists()) {
            plugin.getLogger().warning("找不到掉落設定檔: " + file.getAbsolutePath());
            return Collections.emptyList();
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection items = yaml.getConfigurationSection("items");
        if (items == null) {
            plugin.getLogger().warning("掉落設定檔中沒有 items 區段: " + bossId);
            return Collections.emptyList();
        }

        List<DropEntry> drops = new ArrayList<>();
        for (String key : items.getKeys(false)) {
            String base64 = items.getString(key + ".item-bytes");
            double chance = items.getDouble(key + ".chance", 0);
            String displayName = items.getString(key + ".display-name", "");

            ItemStack item = SerializationUtil.deserializeItemStack(base64);
            if (item != null) {
                drops.add(new DropEntry(item, chance, displayName));
            } else {
                plugin.getLogger().warning("無法反序列化掉落物品: bossId=" + bossId + " key=" + key + " displayName=" + displayName);
            }
        }
        plugin.getLogger().info("載入 " + drops.size() + " 個掉落物品: bossId=" + bossId);
        return drops;
    }

    public void saveDrop(String bossId, ItemStack item, double chance) {
        File file = new File(dropsFolder, bossId + ".yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection items = yaml.getConfigurationSection("items");
        if (items == null) {
            items = yaml.createSection("items");
        }

        int nextIndex = getNextIndex(items);
        String path = "items." + nextIndex;
        yaml.set(path + ".item-bytes", SerializationUtil.serializeItemStack(item));
        yaml.set(path + ".display-name", item.getType().name());
        yaml.set(path + ".chance", chance);

        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("無法儲存掉落設定: " + bossId);
        }
    }

    public void removeDrop(String bossId, int index) {
        File file = new File(dropsFolder, bossId + ".yml");
        if (!file.exists()) return;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        yaml.set("items." + index, null);

        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("無法儲存掉落設定: " + bossId);
        }
    }

    public void updateChance(String bossId, int index, double chance) {
        File file = new File(dropsFolder, bossId + ".yml");
        if (!file.exists()) return;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        yaml.set("items." + index + ".chance", chance);

        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("無法儲存掉落設定: " + bossId);
        }
    }

    public List<ItemStack> rollDrops(String bossId) {
        List<DropEntry> drops = getDrops(bossId);
        List<ItemStack> result = new ArrayList<>();
        Random random = new Random();

        for (DropEntry drop : drops) {
            double roll = random.nextDouble() * 100;
            if (roll < drop.chance()) {
                result.add(drop.item().clone());
            }
        }
        return result;
    }

    private int getNextIndex(ConfigurationSection items) {
        return items.getKeys(false).stream()
                .mapToInt(Integer::parseInt)
                .max().orElse(-1) + 1;
    }

    public record DropEntry(ItemStack item, double chance, String displayName) {}
}
