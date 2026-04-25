package me.ninepin.worldboss.discord;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.JDA;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import me.ninepin.worldboss.WorldBoss;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.awt.Color;
import java.time.Instant;

public class DiscordNotificationService {

    private static final String DEFAULT_CHANNEL_ID = "000000000000000000";

    private final WorldBoss plugin;
    private boolean enabled;
    private String channelId;
    private boolean countdownEnabled;
    private String countdownChannelId;
    private String spawnChannelId;
    private String deathChannelId;
    private String despawnChannelId;
    private EmbedConfig countdownEmbed;
    private EmbedConfig spawnEmbed;
    private EmbedConfig deathEmbed;
    private EmbedConfig despawnEmbed;
    private String footer;

    public DiscordNotificationService(WorldBoss plugin) {
        this.plugin = plugin;
        loadConfig();
        logStatus();
    }

    private void loadConfig() {
        enabled = plugin.getConfig().getBoolean("discord.enabled", false);
        countdownEnabled = plugin.getConfig().getBoolean("discord.countdown.enabled", false);
        channelId = getConfiguredChannelId("discord.channel-id");
        countdownChannelId = getEventChannelId("countdown", "respawn", "spawn");
        spawnChannelId = getEventChannelId("respawn", "spawn");
        deathChannelId = getEventChannelId("death");
        despawnChannelId = getEventChannelId("despawn");
        footer = plugin.getConfig().getString("discord.embed.footer", "WorldBoss 通知系統");

        countdownEmbed = new EmbedConfig("discord.embed.countdown.", "#00AAFF", "⏳ Boss 重生倒數", "**%boss_name%** 將在 **%time%** 後重生！");
        spawnEmbed = new EmbedConfig("discord.embed.spawn.", "#FF4444", "⚔️ Boss 出現通知", "**%boss_name%** 已出現！快來挑戰吧！");
        deathEmbed = new EmbedConfig("discord.embed.death.", "#FFD700", "💀 Boss 擊敗通知", "**%boss_name%** 已被擊敗！");
        despawnEmbed = new EmbedConfig("discord.embed.despawn.", "#808080", "⏰ Boss 消失通知", "**%boss_name%** 因無人攻擊已消失");
    }

    private void logStatus() {
        if (!enabled) return;
        if (Bukkit.getPluginManager().getPlugin("DiscordSRV") == null) {
            plugin.getLogger().warning("DiscordSRV 未安裝，Discord 通知無法運作");
            return;
        }
        plugin.getLogger().info("Discord 通知已啟用 (Embed 模式)，倒數頻道 ID: " + countdownChannelId
                + "，重生頻道 ID: " + spawnChannelId
                + "，死亡頻道 ID: " + deathChannelId
                + "，消失頻道 ID: " + despawnChannelId);
    }

    public void reload() {
        loadConfig();
        logStatus();
    }

    public void notifyBossSpawn(String bossId, String bossName) {
        String cleanName = stripColor(bossName);
        sendEmbed(spawnEmbed, spawnChannelId, bossId, cleanName, null, null);
    }

    public void notifyBossDeath(String bossId, String bossName, String topPlayers) {
        String cleanName = stripColor(bossName);
        sendEmbed(deathEmbed, deathChannelId, bossId, cleanName, topPlayers, null);
    }

    public void notifyBossDespawn(String bossId, String bossName) {
        String cleanName = stripColor(bossName);
        sendEmbed(despawnEmbed, despawnChannelId, bossId, cleanName, null, null);
    }

    public void notifyBossCountdown(String bossId, String bossName, String time) {
        if (!countdownEnabled) return;
        String cleanName = stripColor(bossName);
        sendEmbed(countdownEmbed, countdownChannelId, bossId, cleanName, null, time);
    }

    private void sendEmbed(EmbedConfig config, String targetChannelId, String bossId, String bossName, String topPlayers, String time) {
        try {
            if (!enabled) return;

            TextChannel channel = resolveChannel(targetChannelId);
            if (channel == null) return;

            EmbedBuilder builder = new EmbedBuilder();
            builder.setTitle(config.title);
            builder.setDescription(config.description
                    .replace("%boss_name%", bossName)
                    .replace("%boss_id%", bossId)
                    .replace("%time%", time == null ? "" : time));
            builder.setColor(config.color);
            builder.setTimestamp(Instant.now());

            if (footer != null && !footer.isEmpty()) {
                builder.setFooter(footer);
            }

            if (topPlayers != null) {
                String topPlayersTitle = plugin.getConfig().getString("discord.embed.death.top-players-title", "🏆 前三名傷害");
                builder.addField(topPlayersTitle, topPlayers, false);
            }

            channel.sendMessageEmbeds(builder.build()).queue(
                    null,
                    error -> plugin.getLogger().warning("Discord 訊息發送失敗: " + error.getMessage())
            );
        } catch (Exception e) {
            plugin.getLogger().warning("Discord 通知發送失敗: " + e.getMessage());
        }
    }

    private String getEventChannelId(String... events) {
        for (String event : events) {
            String eventChannelId = getConfiguredChannelId("discord.channels." + event);
            if (!eventChannelId.isBlank()) {
                return eventChannelId;
            }
        }
        return channelId;
    }

    private String getConfiguredChannelId(String path) {
        String configuredChannelId = plugin.getConfig().getString(path, "");
        if (configuredChannelId == null || configuredChannelId.isBlank() || DEFAULT_CHANNEL_ID.equals(configuredChannelId)) {
            return "";
        }
        return configuredChannelId;
    }

    private TextChannel resolveChannel(String targetChannelId) {
        if (Bukkit.getPluginManager().getPlugin("DiscordSRV") == null) return null;
        JDA jda = DiscordSRV.getPlugin().getJda();
        if (jda == null) return null;
        if (targetChannelId == null || targetChannelId.isBlank()) {
            plugin.getLogger().warning("Discord 頻道 ID 未設定");
            return null;
        }
        TextChannel ch = jda.getTextChannelById(targetChannelId);
        if (ch == null) {
            plugin.getLogger().warning("找不到 Discord 頻道 ID: " + targetChannelId);
        }
        return ch;
    }

    private String stripColor(String text) {
        return ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', text));
    }

    private class EmbedConfig {
        final Color color;
        final String title;
        final String description;

        EmbedConfig(String configPrefix, String defaultColor, String defaultTitle, String defaultDescription) {
            String hex = plugin.getConfig().getString(configPrefix + "color", defaultColor);
            this.color = parseColor(hex);
            this.title = plugin.getConfig().getString(configPrefix + "title", defaultTitle);
            this.description = plugin.getConfig().getString(configPrefix + "description", defaultDescription);
        }
    }

    private static Color parseColor(String hex) {
        try {
            return Color.decode(hex);
        } catch (NumberFormatException e) {
            return Color.GRAY;
        }
    }
}
