# WorldBoss

WorldBoss 是一個 Paper 伺服器用的世界 Boss 外掛，整合 MythicMobs 生成 Boss，並使用 DecentHolograms 顯示即時與歷史傷害排行榜。外掛也支援掉落物設定 GUI、定時生成、倒數廣播，以及選用的 DiscordSRV 通知。

## 功能

- 使用 MythicMobs 的 mob ID 生成世界 Boss
- 支援固定間隔與指定星期時間生成
- Boss 存活逾時後自動 despawn
- 統計玩家對 Boss 造成的傷害
- DecentHolograms 即時傷害排行榜與歷史排行榜
- GUI 管理 Boss 設定與掉落物
- 可設定每個掉落物的掉落機率
- 可選用 DiscordSRV 發送生成、死亡與消失通知

## 需求

- Java 21
- Paper 1.21.x
- MythicMobs
- DecentHolograms
- DiscordSRV，可選
- Maven，用於自行建置

## 建置

在專案根目錄執行：

```bash
mvn clean package
```

建置完成後，外掛 jar 會輸出到：

```text
target/WorldBoss-1.0-SNAPSHOT.jar
```

## 安裝

1. 先安裝並啟用 MythicMobs 與 DecentHolograms。
2. 將 `target/WorldBoss-1.0-SNAPSHOT.jar` 放進伺服器的 `plugins` 資料夾。
3. 啟動或重啟伺服器。
4. 編輯 `plugins/WorldBoss/config.yml`。
5. 執行 `/wbadmin reload` 重新載入設定。

## 指令

玩家指令：

```text
/wb list
/wb info <bossId>
```

管理員指令：

```text
/wbadmin settings
/wbadmin spawn <bossId>
/wbadmin despawn <bossId>
/wbadmin reload
/wbadmin drops <bossId>
/wbadmin setdrop <bossId> <chance>
/wbadmin setmob <bossId> <mythicMobId>
```

## 權限

```text
worldboss.use
worldboss.admin
```

`worldboss.use` 預設開放給所有玩家，`worldboss.admin` 預設只有 OP 可用。

## 基本設定

`config.yml` 內可設定 Boss、生成位置、生成時間、排行榜、廣播訊息、獎勵與 Discord 通知。

Boss 設定範例：

```yaml
boss-settings:
  bosses:
    example_boss:
      mythicmob-id: "SkeletalKnight"
      display-name: "&c世界 Boss"
      spawn-location:
        world: "world"
        x: 100.0
        y: 64.0
        z: 200.0
      spawn:
        interval: 3600
        schedule:
          - "THURSDAY 00:00"
          - "SATURDAY 20:00"
      despawn-timeout: 180
```

排行榜設定範例：

```yaml
leaderboard:
  example_boss:
    realtime:
      enabled: true
      location:
        world: "world"
        x: 105.0
        y: 70.0
        z: 200.0
      display-count: 10
      title: "&6&l即時傷害排行"
      format: "&e#%rank% &f%player% &7- &c%damage%"
    history:
      enabled: true
      location:
        world: "world"
        x: 110.0
        y: 70.0
        z: 200.0
      display-count: 10
      title: "&b&l歷史傷害排行"
      format: "&e#%rank% &f%player% &7- &c%damage%"
    hide-realtime-on-death: true
```

## 掉落物設定

使用 `/wbadmin drops <bossId>` 可開啟掉落物 GUI。

- 將物品放入 GUI 可新增掉落物
- 點擊掉落物可調整機率
- 右鍵或指定操作可移除掉落物，依 GUI 提示為準
- 也可以手持物品執行 `/wbadmin setdrop <bossId> <chance>` 快速新增掉落物

`chance` 使用百分比，範圍為 `0` 到 `100`。

## DiscordSRV

若伺服器已安裝 DiscordSRV，可在設定中啟用：

```yaml
discord:
  enabled: true
  channel-id: "000000000000000000"
  channels:
    respawn: "000000000000000000"
    death: "000000000000000000"
    despawn: "000000000000000000"
```

請將 `channels.respawn`、`channels.death`、`channels.despawn` 分別改成重生、死亡、消失通知要發送的 Discord 頻道 ID。若未設定個別頻道，會回退使用舊版的 `channel-id`。

## 注意事項

- `mythicmob-id` 必須存在於 MythicMobs 設定中。
- `spawn-location.world` 必須是伺服器已載入的世界名稱。
- 排行榜位置需設定在有效世界中，否則 hologram 無法建立。
- 修改設定後請使用 `/wbadmin reload` 重新載入。
