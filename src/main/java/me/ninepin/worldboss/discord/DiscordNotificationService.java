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

    private final WorldBoss plugin;
    private boolean enabled;
    private String channelId;
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
        channelId = plugin.getConfig().getString("discord.channel-id", "");
        footer = plugin.getConfig().getString("discord.embed.footer", "WorldBoss 通知系統");

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
        plugin.getLogger().info("Discord 通知已啟用 (Embed 模式)，頻道 ID: " + channelId);
    }

    public void reload() {
        loadConfig();
        logStatus();
    }

    public void notifyBossSpawn(String bossId, String bossName) {
        String cleanName = stripColor(bossName);
        sendEmbed(spawnEmbed, bossId, cleanName, null);
    }

    public void notifyBossDeath(String bossId, String bossName, String topPlayers) {
        String cleanName = stripColor(bossName);
        sendEmbed(deathEmbed, bossId, cleanName, topPlayers);
    }

    public void notifyBossDespawn(String bossId, String bossName) {
        String cleanName = stripColor(bossName);
        sendEmbed(despawnEmbed, bossId, cleanName, null);
    }

    private void sendEmbed(EmbedConfig config, String bossId, String bossName, String topPlayers) {
        try {
            if (!enabled) return;

            TextChannel channel = resolveChannel();
            if (channel == null) return;

            EmbedBuilder builder = new EmbedBuilder();
            builder.setTitle(config.title);
            builder.setDescription(config.description
                    .replace("%boss_name%", bossName)
                    .replace("%boss_id%", bossId));
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

    private TextChannel resolveChannel() {
        if (Bukkit.getPluginManager().getPlugin("DiscordSRV") == null) return null;
        JDA jda = DiscordSRV.getPlugin().getJda();
        if (jda == null) return null;
        TextChannel ch = jda.getTextChannelById(channelId);
        if (ch == null) {
            plugin.getLogger().warning("找不到 Discord 頻道 ID: " + channelId);
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
