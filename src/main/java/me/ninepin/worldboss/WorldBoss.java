package me.ninepin.worldboss;

import me.ninepin.worldboss.boss.BossManager;
import me.ninepin.worldboss.command.CommandHandler;
import me.ninepin.worldboss.config.ConfigManager;
import me.ninepin.worldboss.damage.DamageLeaderboard;
import me.ninepin.worldboss.damage.DamageTracker;
import me.ninepin.worldboss.gui.SettingsGUI;
import me.ninepin.worldboss.gui.SettingsListener;
import me.ninepin.worldboss.hologram.HologramManager;
import me.ninepin.worldboss.listener.BossDeathListener;
import me.ninepin.worldboss.listener.ChestInteractListener;
import me.ninepin.worldboss.listener.DamageListener;
import me.ninepin.worldboss.loot.LootConfig;
import me.ninepin.worldboss.loot.LootChestManager;
import me.ninepin.worldboss.loot.LootGUI;
import me.ninepin.worldboss.loot.LootListener;
import me.ninepin.worldboss.loot.RewardManager;
import me.ninepin.worldboss.discord.DiscordNotificationService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public final class WorldBoss extends JavaPlugin {

    private ConfigManager configManager;
    private BossManager bossManager;
    private DamageTracker damageTracker;
    private DamageLeaderboard damageLeaderboard;
    private HologramManager hologramManager;
    private LootConfig lootConfig;
    private LootChestManager lootChestManager;
    private RewardManager rewardManager;
    private DiscordNotificationService discordNotificationService;
    private LootGUI lootGUI;
    private LootListener lootListener;
    private SettingsGUI settingsGUI;
    private SettingsListener settingsListener;

    @Override
    public void onEnable() {
        // Initialize config
        configManager = new ConfigManager(this);
        configManager.load();

        // Initialize core systems
        damageTracker = new DamageTracker();
        damageLeaderboard = new DamageLeaderboard(this);
        damageLeaderboard.loadAll();

        lootConfig = new LootConfig(this);
        lootChestManager = new LootChestManager(this);
        rewardManager = new RewardManager(this);
        discordNotificationService = new DiscordNotificationService(this);

        bossManager = new BossManager(this);
        hologramManager = new HologramManager(this);

        lootGUI = new LootGUI(this);
        settingsGUI = new SettingsGUI(this);
        lootListener = new LootListener(this, lootGUI);
        settingsListener = new SettingsListener(this, settingsGUI);

        // Register listeners
        getServer().getPluginManager().registerEvents(new DamageListener(this), this);
        getServer().getPluginManager().registerEvents(new BossDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new ChestInteractListener(this), this);
        getServer().getPluginManager().registerEvents(lootListener, this);
        getServer().getPluginManager().registerEvents(settingsListener, this);
        getServer().getPluginManager().registerEvents(new ChatListener(), this);

        // Register commands
        CommandHandler commandHandler = new CommandHandler(this);
        getCommand("wb").setExecutor(commandHandler);
        getCommand("wb").setTabCompleter(commandHandler);
        getCommand("wbadmin").setExecutor(commandHandler);
        getCommand("wbadmin").setTabCompleter(commandHandler);

        // Start systems
        bossManager.init();
        hologramManager.init();
        bossManager.startInitialCountdowns();
        settingsGUI.startRefreshTask();

        getLogger().info("WorldBoss 插件已啟用！");
    }

    @Override
    public void onDisable() {
        if (settingsGUI != null) {
            settingsGUI.stopRefreshTask();
        }
        if (damageLeaderboard != null) {
            damageLeaderboard.saveAll();
        }
        if (hologramManager != null) {
            hologramManager.shutdown();
        }
        if (bossManager != null) {
            bossManager.shutdown();
        }
        if (lootChestManager != null) {
            lootChestManager.clearAll();
        }

        getLogger().info("WorldBoss 插件已停用！");
    }

    /**
     * Apply config changes by reloading config and restarting systems.
     */
    public void applyConfigChanges() {
        configManager.reload();
        bossManager.getSpawnScheduler().cancelAll();
        bossManager.startInitialCountdowns();
        hologramManager.shutdown();
        hologramManager.init();
        discordNotificationService.reload();
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public BossManager getBossManager() {
        return bossManager;
    }

    public DamageTracker getDamageTracker() {
        return damageTracker;
    }

    public DamageLeaderboard getDamageLeaderboard() {
        return damageLeaderboard;
    }

    public HologramManager getHologramManager() {
        return hologramManager;
    }

    public LootConfig getLootConfig() {
        return lootConfig;
    }

    public LootChestManager getLootChestManager() {
        return lootChestManager;
    }

    public RewardManager getRewardManager() {
        return rewardManager;
    }

    public DiscordNotificationService getDiscordNotificationService() {
        return discordNotificationService;
    }

    public LootGUI getLootGUI() {
        return lootGUI;
    }

    public LootListener getLootListener() {
        return lootListener;
    }

    public SettingsGUI getSettingsGUI() {
        return settingsGUI;
    }

    public SettingsListener getSettingsListener() {
        return settingsListener;
    }

    private class ChatListener implements Listener {
        @EventHandler
        public void onPlayerChat(AsyncPlayerChatEvent event) {
            Player player = event.getPlayer();
            UUID uuid = player.getUniqueId();

            // Check SettingsGUI chat input first
            if (settingsListener.isAwaitingInput(uuid)) {
                event.setCancelled(true);
                String message = event.getMessage();
                getServer().getScheduler().runTask(WorldBoss.this, () ->
                        settingsListener.handleChatInput(uuid, message));
                return;
            }

            // Check LootGUI chance input
            if (lootListener.isAwaitingChanceInput(uuid)) {
                event.setCancelled(true);
                String message = event.getMessage();
                getServer().getScheduler().runTask(WorldBoss.this, () ->
                        lootListener.handleChanceInput(uuid, message));
            }
        }
    }
}
