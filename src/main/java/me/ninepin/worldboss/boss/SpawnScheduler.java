package me.ninepin.worldboss.boss;

import me.ninepin.worldboss.service.BossService;
import me.ninepin.worldboss.service.ConfigService;
import me.ninepin.worldboss.WorldBoss;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class SpawnScheduler {

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    private final WorldBoss plugin;
    private final BossService bossService;
    private final ConfigService configService;
    private final Map<String, Long> nextIntervalSpawnMillis = new HashMap<>();
    private final Map<String, Long> nextScheduledSpawnMillis = new HashMap<>();
    private final Set<String> firedCountdowns = new HashSet<>();
    private BukkitTask timerTask;

    public SpawnScheduler(WorldBoss plugin, BossService bossService, ConfigService configService) {
        this.plugin = plugin;
        this.bossService = bossService;
        this.configService = configService;
    }

    public void startTimer() {
        if (timerTask != null) return;
        timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void stopTimer() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }

    public void startCountdown(BossData bossData) {
        cancelBoss(bossData.getId());

        if (bossData.hasIntervalSpawn()) {
            nextIntervalSpawnMillis.put(bossData.getId(),
                    System.currentTimeMillis() + bossData.getSpawnInterval() * 1000L);
        }

        if (bossData.hasScheduledSpawn()) {
            long next = calculateNextScheduledMillis(bossData);
            if (next > 0) {
                nextScheduledSpawnMillis.put(bossData.getId(), next);
            }
        }

        firedCountdowns.removeIf(key -> key.startsWith(bossData.getId() + ":"));
    }

    private void tick() {
        long now = System.currentTimeMillis();

        Set<String> bossIds = new HashSet<>();
        bossIds.addAll(nextIntervalSpawnMillis.keySet());
        bossIds.addAll(nextScheduledSpawnMillis.keySet());

        for (String bossId : bossIds) {
            Long nextSpawn = getNextSpawnMillis(bossId);
            if (nextSpawn == null) continue;

            long remainingMillis = nextSpawn - now;
            long remainingSec = (remainingMillis + 999) / 1000;

            BossData bossData = configService.getBoss(bossId);
            if (bossData != null && remainingMillis > 0) {
                checkCountdownBroadcasts(bossId, bossData, remainingSec);
            }

            if (remainingMillis <= 0) {
                Long intervalTime = nextIntervalSpawnMillis.get(bossId);
                Long scheduledTime = nextScheduledSpawnMillis.get(bossId);

                if (intervalTime != null && now >= intervalTime) {
                    nextIntervalSpawnMillis.remove(bossId);
                }
                if (scheduledTime != null && now >= scheduledTime) {
                    nextScheduledSpawnMillis.remove(bossId);
                }

                firedCountdowns.removeIf(key -> key.startsWith(bossId + ":"));

                if (!bossService.isBossActive(bossId) && bossData != null) {
                    bossService.spawnBoss(bossData);
                }
            }
        }
    }

    private void checkCountdownBroadcasts(String bossId, BossData bossData, long remainingSec) {
        List<Integer> countdownTimes = configService.getCountdownTimes();
        String messageTemplate = configService.getCountdownMessage();
        String bossName = bossData.getDisplayName();

        for (int secondsLeft : countdownTimes) {
            String key = bossId + ":" + secondsLeft;
            if (remainingSec == secondsLeft && !firedCountdowns.contains(key)) {
                firedCountdowns.add(key);
                String timeStr = secondsLeft + "秒";
                String raw = messageTemplate
                        .replace("%boss_name%", bossName)
                        .replace("%time%", timeStr);
                Bukkit.broadcast(SERIALIZER.deserialize(raw));
            }
        }
    }

    private long calculateNextScheduledMillis(BossData bossData) {
        LocalDateTime now = LocalDateTime.now();
        long minSeconds = Long.MAX_VALUE;

        for (BossData.ScheduledTime st : bossData.getScheduledTimes()) {
            LocalDateTime next = getNextOccurrence(now, st);
            long seconds = ChronoUnit.SECONDS.between(now, next);
            if (seconds > 0 && seconds < minSeconds) {
                minSeconds = seconds;
            }
        }

        if (minSeconds == Long.MAX_VALUE) return -1;
        return System.currentTimeMillis() + minSeconds * 1000L;
    }

    private LocalDateTime getNextOccurrence(LocalDateTime now, BossData.ScheduledTime st) {
        LocalDateTime target = now.with(st.getDayOfWeek())
                .withHour(st.getHour()).withMinute(st.getMinute()).withSecond(0).withNano(0);
        if (!target.isAfter(now)) {
            target = target.plusWeeks(1);
        }
        return target;
    }

    public void cancelBoss(String bossId) {
        nextIntervalSpawnMillis.remove(bossId);
        nextScheduledSpawnMillis.remove(bossId);
        firedCountdowns.removeIf(key -> key.startsWith(bossId + ":"));
    }

    public void cancelAll() {
        nextIntervalSpawnMillis.clear();
        nextScheduledSpawnMillis.clear();
        firedCountdowns.clear();
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
