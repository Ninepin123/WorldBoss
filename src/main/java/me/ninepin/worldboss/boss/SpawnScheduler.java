package me.ninepin.worldboss.boss;

import me.ninepin.worldboss.WorldBoss;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class SpawnScheduler {

    private final WorldBoss plugin;
    private final BossManager bossManager;
    private final Map<String, BukkitTask> intervalTasks = new HashMap<>();
    private final Map<String, BukkitTask> scheduleTasks = new HashMap<>();
    private final Map<String, List<BukkitTask>> broadcastTasks = new HashMap<>();
    private final Map<String, Long> nextIntervalSpawnMillis = new HashMap<>();
    private final Map<String, Long> nextScheduledSpawnMillis = new HashMap<>();

    public SpawnScheduler(WorldBoss plugin, BossManager bossManager) {
        this.plugin = plugin;
        this.bossManager = bossManager;
    }

    public void startCountdown(BossData bossData) {
        cancelAll(bossData.getId());

        if (bossData.hasIntervalSpawn()) {
            startIntervalCountdown(bossData);
        }

        if (bossData.hasScheduledSpawn()) {
            startScheduledCountdown(bossData);
        }
    }

    private void startIntervalCountdown(BossData bossData) {
        long delayTicks = bossData.getSpawnInterval() * 20L;
        scheduleCountdownBroadcasts(bossData, delayTicks);
        nextIntervalSpawnMillis.put(bossData.getId(),
                System.currentTimeMillis() + bossData.getSpawnInterval() * 1000L);
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                nextIntervalSpawnMillis.remove(bossData.getId());
                if (!bossManager.isBossActive(bossData.getId())) {
                    bossManager.spawnBoss(bossData);
                }
            }
        }.runTaskLater(plugin, delayTicks);
        intervalTasks.put(bossData.getId(), task);
    }

    private void startScheduledCountdown(BossData bossData) {
        LocalDateTime now = LocalDateTime.now();
        long minSeconds = Long.MAX_VALUE;

        for (BossData.ScheduledTime st : bossData.getScheduledTimes()) {
            LocalDateTime next = getNextOccurrence(now, st);
            long seconds = ChronoUnit.SECONDS.between(now, next);
            if (seconds < minSeconds) {
                minSeconds = seconds;
            }
        }

        if (minSeconds <= 0) return;

        long delayTicks = minSeconds * 20L;
        scheduleCountdownBroadcasts(bossData, delayTicks);
        nextScheduledSpawnMillis.put(bossData.getId(),
                System.currentTimeMillis() + minSeconds * 1000L);
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                nextScheduledSpawnMillis.remove(bossData.getId());
                if (bossManager.isBossActive(bossData.getId())) return;
                bossManager.spawnBoss(bossData);
            }
        }.runTaskLater(plugin, delayTicks);
        scheduleTasks.put(bossData.getId(), task);
    }

    private LocalDateTime getNextOccurrence(LocalDateTime now, BossData.ScheduledTime st) {
        LocalDateTime target = now.with(DayOfWeek.from(st.getDayOfWeek()))
                .withHour(st.getHour()).withMinute(st.getMinute()).withSecond(0).withNano(0);
        if (!target.isAfter(now)) {
            target = target.plusWeeks(1);
        }
        return target;
    }

    private void scheduleCountdownBroadcasts(BossData bossData, long totalDelayTicks) {
        List<BukkitTask> tasks = new ArrayList<>();
        String bossId = bossData.getId();
        String bossName = ChatColor.translateAlternateColorCodes('&', bossData.getDisplayName());

        List<Integer> countdownTimes = plugin.getConfigManager().getCountdownTimes();
        String messageTemplate = plugin.getConfigManager().getCountdownMessage();

        for (int secondsLeft : countdownTimes) {
            long broadcastAt = totalDelayTicks - ((long) secondsLeft * 20L);
            if (broadcastAt > 0) {
                final String timeStr = secondsLeft + "秒";
                BukkitTask task = new BukkitRunnable() {
                    @Override
                    public void run() {
                        String message = ChatColor.translateAlternateColorCodes('&',
                                messageTemplate
                                        .replace("%boss_name%", bossName)
                                        .replace("%time%", timeStr));
                        Bukkit.broadcastMessage(message);
                    }
                }.runTaskLater(plugin, broadcastAt);
                tasks.add(task);
            }
        }

        broadcastTasks.computeIfAbsent(bossId, k -> new ArrayList<>()).addAll(tasks);
    }

    public void cancelAll(String bossId) {
        BukkitTask intervalTask = intervalTasks.remove(bossId);
        if (intervalTask != null) intervalTask.cancel();

        BukkitTask scheduleTask = scheduleTasks.remove(bossId);
        if (scheduleTask != null) scheduleTask.cancel();

        List<BukkitTask> bTasks = broadcastTasks.remove(bossId);
        if (bTasks != null) {
            for (BukkitTask t : bTasks) t.cancel();
        }

        nextIntervalSpawnMillis.remove(bossId);
        nextScheduledSpawnMillis.remove(bossId);
    }

    public void cancelAll() {
        for (BukkitTask task : intervalTasks.values()) task.cancel();
        for (BukkitTask task : scheduleTasks.values()) task.cancel();
        for (List<BukkitTask> tasks : broadcastTasks.values()) {
            for (BukkitTask t : tasks) t.cancel();
        }
        intervalTasks.clear();
        scheduleTasks.clear();
        broadcastTasks.clear();
        nextIntervalSpawnMillis.clear();
        nextScheduledSpawnMillis.clear();
    }

    public Long getNextIntervalSpawnMillis(String bossId) {
        return nextIntervalSpawnMillis.get(bossId);
    }

    public Long getNextScheduledSpawnMillis(String bossId) {
        return nextScheduledSpawnMillis.get(bossId);
    }

    public Long getNextSpawnMillis(String bossId) {
        Long a = nextIntervalSpawnMillis.get(bossId);
        Long b = nextScheduledSpawnMillis.get(bossId);
        if (a == null) return b;
        if (b == null) return a;
        return Math.min(a, b);
    }
}
