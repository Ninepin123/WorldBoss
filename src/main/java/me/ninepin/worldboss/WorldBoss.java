package me.ninepin.worldboss;

import me.ninepin.worldboss.boss.BossManager;
import me.ninepin.worldboss.command.CommandHandler;
import me.ninepin.worldboss.config.ConfigManager;
import me.ninepin.worldboss.damage.DamageLeaderboard;
import me.ninepin.worldboss.damage.DamageTracker;
import me.ninepin.worldboss.discord.DiscordNotificationService;
import me.ninepin.worldboss.gui.SettingsGUI;
import me.ninepin.worldboss.gui.SettingsListener;
import me.ninepin.worldboss.hologram.HologramManager;
import me.ninepin.worldboss.listener.BossDeathListener;
import me.ninepin.worldboss.listener.DamageListener;
import me.ninepin.worldboss.loot.LootConfig;
import me.ninepin.worldboss.loot.LootGUI;
import me.ninepin.worldboss.loot.LootListener;
import me.ninepin.worldboss.loot.RewardManager;
import me.ninepin.worldboss.service.BossService;
import me.ninepin.worldboss.service.ConfigService;
import me.ninepin.worldboss.service.DamageService;
import me.ninepin.worldboss.service.DropService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public final class WorldBoss extends JavaPlugin {

    private ConfigService configService;
    private DamageService damageService;
    private DropService dropService;
    private BossService bossService;

    private SettingsGUI settingsGUI;
    private SettingsListener settingsListener;
    private LootGUI lootGUI;
    private LootListener lootListener;

    @Override
    public void onEnable() {
        ConfigManager configManager = new ConfigManager(this);
        configManager.load();

        DamageTracker damageTracker = new DamageTracker();
        DamageLeaderboard damageLeaderboard = new DamageLeaderboard(this);
        damageLeaderboard.loadAll();

        LootConfig lootConfig = new LootConfig(this);

        configService = new ConfigService(this, configManager);
        damageService = new DamageService(damageTracker, damageLeaderboard);
        dropService = new DropService(lootConfig);

        RewardManager rewardManager = new RewardManager(this, configService, damageService, dropService);
        DiscordNotificationService discordService = new DiscordNotificationService(this);
        BossManager bossManager = new BossManager(this);
        HologramManager hologramManager = new HologramManager(this, configService, damageService);

        bossService = new BossService(this, configService, damageService, bossManager,
                hologramManager, discordService, rewardManager);

        lootGUI = new LootGUI(dropService);
        settingsGUI = new SettingsGUI(this, configService, bossService);
        lootListener = new LootListener(this, lootGUI, dropService, configService, settingsGUI);
        settingsListener = new SettingsListener(this, settingsGUI, configService, bossService, lootGUI);

        getServer().getPluginManager().registerEvents(new DamageListener(bossService, damageService), this);
        getServer().getPluginManager().registerEvents(new BossDeathListener(bossService), this);
        getServer().getPluginManager().registerEvents(lootListener, this);
        getServer().getPluginManager().registerEvents(settingsListener, this);
        getServer().getPluginManager().registerEvents(new ChatListener(), this);

        CommandHandler commandHandler = new CommandHandler(configService, bossService, dropService,
                settingsGUI, settingsListener, lootGUI);
        getCommand("wb").setExecutor(commandHandler);
        getCommand("wb").setTabCompleter(commandHandler);
        getCommand("wbadmin").setExecutor(commandHandler);
        getCommand("wbadmin").setTabCompleter(commandHandler);

        bossService.init();
        hologramManager.init();
        bossService.startInitialCountdowns();
        settingsGUI.startRefreshTask();

        getLogger().info("WorldBoss 插件已啟用！");
    }

    @Override
    public void onDisable() {
        if (settingsGUI != null) {
            settingsGUI.stopRefreshTask();
        }
        if (damageService != null) {
            damageService.saveAllHistory();
        }
        if (bossService != null) {
            bossService.shutdown();
        }

        getLogger().info("WorldBoss 插件已停用！");
    }

    public ConfigService getConfigService() {
        return configService;
    }

    public BossService getBossService() {
        return bossService;
    }

    public DamageService getDamageService() {
        return damageService;
    }

    public DropService getDropService() {
        return dropService;
    }

    public SettingsGUI getSettingsGUI() {
        return settingsGUI;
    }

    public SettingsListener getSettingsListener() {
        return settingsListener;
    }

    public LootGUI getLootGUI() {
        return lootGUI;
    }

    public LootListener getLootListener() {
        return lootListener;
    }

    private class ChatListener implements Listener {
        @EventHandler
        public void onPlayerChat(AsyncPlayerChatEvent event) {
            Player player = event.getPlayer();
            UUID uuid = player.getUniqueId();

            if (settingsListener.isAwaitingInput(uuid)) {
                event.setCancelled(true);
                String message = event.getMessage();
                getServer().getScheduler().runTask(WorldBoss.this, () ->
                        settingsListener.handleChatInput(uuid, message));
                return;
            }

            if (lootListener.isAwaitingChanceInput(uuid)) {
                event.setCancelled(true);
                String message = event.getMessage();
                getServer().getScheduler().runTask(WorldBoss.this, () ->
                        lootListener.handleChanceInput(uuid, message));
            }
        }
    }
}
