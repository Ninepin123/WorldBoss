# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build

```bash
mvn clean package
```

Output JAR: `target/WorldBoss-1.0-SNAPSHOT.jar`. Requires Java 21, Maven 3.9+.

No tests exist. No linting is configured.

## Project Overview

A **Paper 1.21 Minecraft plugin** for scheduled World Boss encounters with damage leaderboards (DecentHolograms), loot chests, and Discord notifications. Hard depends on **MythicMobs** and **DecentHolograms**. Soft depends on **DiscordSRV**.

Language: Java 21. UI text is in Traditional Chinese (zh_TW).

## Architecture

Entry point: `WorldBoss.java` (JavaPlugin). All systems are initialized in `onEnable()` and wired via getter methods.

### Data Flow

```
ConfigManager (config.yml) → BossData (immutable per load)
       ↓
BossManager → SpawnScheduler → MythicBukkit.spawnMob()
       ↓                          ↓
DamageListener ──→ DamageTracker ──→ HologramManager (DecentHolograms)
                                       ↓
BossDeathListener → DamageLeaderboard (history persistence)
                  → RewardManager (top 3 players get rolled drops in inventory)
                  → LootChestManager (spawn chest with full drop table)
                  → DiscordNotificationService (spawn/death/despawn embeds)
                  → resets DamageTracker, triggers next spawn cycle
```

### Dual Reward System

Two independent reward systems run on boss death, both using the same drop table from LootConfig:
- **RewardManager**: Distributes rolled drops directly to the top 3 damage dealers' inventories (`rollDrops()` — each item rolls independently, not mutually exclusive).
- **LootChestManager**: Spawns a physical chest in the world. Only players who dealt damage to that boss can open it (enforced by `ChestInteractListener`). Chest contents use the full drop table (`getDrops()`), not rolled.

### Key Design Patterns

- **ConfigManager** reads/writes `config.yml` via Bukkit's `FileConfiguration`. All mutations call `plugin.saveConfig()` immediately. After any config change, call `plugin.applyConfigChanges()` which triggers `configManager.reload()` + restarts schedulers + reinitializes holograms + reloads DiscordNotificationService.
- **BossData** is immutable — reconstructed on every `ConfigManager.reload()`. Never mutate directly; always go through ConfigManager setters.
- **Drop items** are stored separately in `plugins/WorldBoss/drops/<bossId>.yml` (not in config.yml). Items are serialized as base64 via `SerializationUtil`.
- **History leaderboard** is persisted in `plugins/WorldBoss/data/<bossId>_history.yml`.
- **Chat input routing**: An inner `ChatListener` in `WorldBoss.java` intercepts `AsyncPlayerChatEvent` and routes messages to `SettingsListener.handleChatInput()` or `LootListener.handleChanceInput()` based on pending input state. Both handlers switch to the main thread via `runTask`.

### GUI System

Two GUI pairs (renderer + listener):

- **SettingsGUI + SettingsListener** (`gui/`): Main menu → Boss edit menu. SettingsGUI renders the inventory; SettingsListener handles all click and chat input logic (10 input types tracked via `PendingInput` map). The `openGUIs` map tracks which menu a player has open (`"main"` or a bossId).
- **LootGUI + LootListener** (`loot/`): Drop item configuration. Players place items directly into the inventory. Bottom row (slots 45-53) is the control area. Left-click to edit chance, right-click to remove, click with item on cursor to add (default 50%).

GUI navigation: Main Menu → Boss Edit Menu → LootGUI. Each pair tracks its own `openGUIs` map. When transitioning between them, the source removes the player from its map before the target adds them.

### Commands

- `/wb list|info` — player commands (permission: `worldboss.use`, default true)
- `/wbadmin spawn|despawn|reload|drops|setdrop|setmob|settings` — admin commands (permission: `worldboss.admin`, default op)

`/wbadmin settings` opens the SettingsGUI main menu. `/wbadmin setmob` supports Tab completion of MythicMob IDs. Tab completion for boss IDs on most subcommands.

### Discord Notifications

`DiscordNotificationService` sends configurable embed messages via DiscordSRV (soft dependency — graceful no-op if absent):
- Boss spawn, death (includes top 3 damage dealers), and despawn notifications.
- Config lives under `discord.*` in config.yml: `enabled`, `channel-id`, per-event embed settings (`color`, `title`, `description` with `%boss_name%`/`%boss_id%` placeholders).

### Boss Spawn Scheduling

`SpawnScheduler` uses `runTaskLater` for both interval and scheduled spawns:
- **Interval**: Fixed delay countdown, auto-restarts after despawn/death.
- **Scheduled**: Computes exact seconds until next `DayOfWeek HH:mm`, schedules a one-shot delayed task.

### Config YAML Structure

```yaml
boss-settings:
  bosses:
    <bossId>:
      mythicmob-id: "..."
      display-name: "&c..."
      spawn-location: { world, x, y, z }
      spawn: { interval, schedule: ["DAY HH:mm"] }
      despawn-timeout: 180
leaderboard:
  <bossId>:
    realtime: { enabled, location, display-count, title, format }
    history: { enabled, location, display-count, title, format }
    hide-realtime-on-death: false
loot:
  <bossId>:
    chest-location: { world, x, y, z }
```

Drop items: `drops/<bossId>.yml` with `items.<index>.{item-bytes, display-name, chance}`.
