package me.ninepin.worldboss.boss;

import org.bukkit.Location;

import java.util.List;

public class BossData {

    private final String id;
    private final String mythicMobId;
    private final String displayName;
    private final Location spawnLocation;
    private final int spawnInterval;
    private final List<ScheduledTime> scheduledTimes;
    private final int despawnTimeout;

    public BossData(String id, String mythicMobId, String displayName, Location spawnLocation,
                    int spawnInterval, List<ScheduledTime> scheduledTimes, int despawnTimeout) {
        this.id = id;
        this.mythicMobId = mythicMobId;
        this.displayName = displayName;
        this.spawnLocation = spawnLocation;
        this.spawnInterval = spawnInterval;
        this.scheduledTimes = scheduledTimes;
        this.despawnTimeout = despawnTimeout;
    }

    public String getId() {
        return id;
    }

    public String getMythicMobId() {
        return mythicMobId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public int getSpawnInterval() {
        return spawnInterval;
    }

    public boolean hasIntervalSpawn() {
        return spawnInterval > 0;
    }

    public List<ScheduledTime> getScheduledTimes() {
        return scheduledTimes;
    }

    public boolean hasScheduledSpawn() {
        return scheduledTimes != null && !scheduledTimes.isEmpty();
    }

    public int getDespawnTimeout() {
        return despawnTimeout;
    }

    public static class ScheduledTime {
        private final java.time.DayOfWeek dayOfWeek;
        private final int hour;
        private final int minute;

        public ScheduledTime(java.time.DayOfWeek dayOfWeek, int hour, int minute) {
            this.dayOfWeek = dayOfWeek;
            this.hour = hour;
            this.minute = minute;
        }

        public java.time.DayOfWeek getDayOfWeek() {
            return dayOfWeek;
        }

        public int getHour() {
            return hour;
        }

        public int getMinute() {
            return minute;
        }
    }
}
