package me.ninepin.worldboss.loot;

import me.ninepin.worldboss.WorldBoss;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class LootGUI {

    private final WorldBoss plugin;
    final Map<UUID, String> openGUIs = new HashMap<>();
    final Map<UUID, String> awaitingChanceInput = new HashMap<>();

    // Fixed 6x9 layout: rows 0-4 (slots 0-44) for drops, row 5 (slots 45-53) for controls
    static final int GUI_SIZE = 54;
    static final int DROP_AREA_END = 45; // exclusive, slots 0-44
    static final int BACK_SLOT = 45; // Back button in bottom row
    static final int INFO_SLOT = 49; // Center of bottom row

    public LootGUI(WorldBoss plugin) {
        this.plugin = plugin;
    }

    public void openDropsGUI(Player player, String bossId) {
        List<LootConfig.DropEntry> drops = plugin.getLootConfig().getDrops(bossId);

        Inventory gui = Bukkit.createInventory(null, GUI_SIZE,
                Component.text(ChatColor.GOLD + "Boss 掉落設定: " + bossId));

        // Fill bottom row with glass
        for (int i = 45; i < 54; i++) {
            ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta fillerMeta = filler.getItemMeta();
            fillerMeta.displayName(Component.empty());
            filler.setItemMeta(fillerMeta);
            gui.setItem(i, filler);
        }

        // Display existing drops with chance info (slots 0-44)
        for (int i = 0; i < drops.size() && i < DROP_AREA_END; i++) {
            LootConfig.DropEntry drop = drops.get(i);
            ItemStack display = drop.item().clone();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
                lore.add(Component.empty());
                lore.add(Component.text("掉落機率: " + drop.chance() + "%")
                        .color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("左鍵 → 修改機率 | 右鍵 → 移除")
                        .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                meta.lore(lore);
                display.setItemMeta(meta);
            }
            gui.setItem(i, display);
        }

        // Back button
        ItemStack backItem = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.displayName(Component.text("返回 Boss 設定")
                .color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        backItem.setItemMeta(backMeta);
        gui.setItem(BACK_SLOT, backItem);

        // Instruction item in bottom row center
        ItemStack instructionItem = new ItemStack(Material.PAPER);
        ItemMeta instructionMeta = instructionItem.getItemMeta();
        instructionMeta.displayName(Component.text("掉落物設定說明")
                .color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        List<Component> instructionLore = new ArrayList<>();
        instructionLore.add(Component.text("直接放入物品 → 新增掉落（預設50%）")
                .color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        instructionLore.add(Component.text("Shift+左鍵物品 → 新增掉落")
                .color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        instructionLore.add(Component.text("左鍵掉落物 → 修改機率")
                .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        instructionLore.add(Component.text("右鍵掉落物 → 移除")
                .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        instructionMeta.lore(instructionLore);
        instructionItem.setItemMeta(instructionMeta);
        gui.setItem(INFO_SLOT, instructionItem);

        player.openInventory(gui);
        openGUIs.put(player.getUniqueId(), bossId);
    }
}
