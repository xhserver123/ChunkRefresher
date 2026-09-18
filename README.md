# ChunkRefresher

Minecraft **Paper 26.2（Minecraft Java 26.2，JDK 25）** 服务端插件：让玩家用命令**手动刷新（重新发送）周围区块**，触发客户端**重新渲染**地形。

**默认即开即用**：所有玩家默认都有权限（`chunkrefresher.use` 默认 `true`），且 `/refreshchunk` 不带参数时默认刷新**玩家附近全部视距 + 2 圈**的区块。

## 它做了什么

玩家执行 `/refreshchunk` 后，插件会：

1. 计算刷新范围（默认：视距模式，见下），列出待刷新区块并按**由近到远**排序；
2. 每 tick 处理最多 `refresh.chunks-per-tick` 个区块，对**已加载**的区块调用 Paper 的 `World#refreshChunk(int, int)`，把区块数据重新发送给跟踪它的客户端。客户端收到后会**重建该区块的渲染网格**，于是地形/光照“肉眼可见地刷新一次”；
3. 用动作栏显示进度，结束后汇报成功 / 跳过数量与耗时。

> 说明：`refreshChunk` 是把区块**重新发送**（不是重新生成地形）。主要用于变通解决间歇泉基岩版玩家区块不渲染

### 默认刷新范围：视距 + 2

视距模式下的半径按下面公式计算（`utils/ViewDistance`）：

```
半径 = min(服务端为该玩家下发的视距, 客户端渲染距离) + view-distance-extra（默认 2）
```

- 取两者较小值，因为玩家实际能看到的是**服务端下发范围**与**客户端渲染距离**的交集；
- `+2` 让刷新范围略大于可见范围，减少“转身后仍是旧网格”的情况；
- 视距模式**不受 `max-radius` 限制**，只受 `absolute-max-radius` 硬上限（默认 32）约束，保证“刷完整视距”这件事本身不会被小半径配置截断。

### 为什么要分批

按视距刷新一次可能涉及数百个区块（例如视距 10 → 半径 12 → 625 个区块），一次性重新发送会产生大量区块包并让客户端瞬间重建大量网格，容易造成卡顿。因此刷新被拆成“每 tick 一小批”执行（`chunks-per-tick` 可调），把尖峰摊平成一条平滑的曲线。

## 命令

| 命令 | 说明 | 权限 |
| --- | --- | --- |
| `/refreshchunk` | 按 `refresh.default-mode` 刷新（默认：视距 + 2） | `chunkrefresher.use`（默认所有玩家） |
| `/refreshchunk view` | 强制按当前视距 + 配置的额外圈数刷新 | 同上 |
| `/refreshchunk <半径>` | 按固定半径刷新；`1` = 3×3 = 9 个区块 | 同上 |
| `/refreshchunk here` | 只刷新当前所在区块（等价半径 0） | 同上 |
| `/refreshchunk reload` | 热重载 `config.yml` 与 `messages.yml`，并重置进行中任务与冷却 | `chunkrefresher.reload` |
| `/refreshchunk help` | 显示帮助 | — |

命令别名：`/rc`、`/chunkrefresh`、`/refreshchunks`。

> 刷新必须由**玩家**发起（需要玩家位置与视距作为刷新依据）

## 权限

| 权限节点 | 默认 | 说明 |
| --- | --- | --- |
| `chunkrefresher.use` | `true` | 允许使用 `/refreshchunk` 刷新区块——**默认所有玩家可用** |
| `chunkrefresher.reload` | `op` | 允许热重载配置 |
| `chunkrefresher.bypass.cooldown` | `op` | 无视刷新冷却 |
| `chunkrefresher.bypass.limit` | `op` | 忽略 `max-radius` 限制（仍受 `absolute-max-radius` 硬上限约束） |

## 配置文件

`config.yml`（首次启动自动生成）：

```yaml
refresh:
  default-mode: view-distance  # 默认模式：view-distance（视距 + N）或 fixed（固定半径）
  view-distance-extra: 2       # 视距模式下额外附加的圈数 → “视距 + 2”
  default-radius: 1            # fixed 模式下不带参数时的半径
  max-radius: 16               # /refreshchunk <半径> 时普通玩家的上限（视距模式不受限）
  absolute-max-radius: 32      # 硬上限：任何玩家、任何模式都不会超过
  chunks-per-tick: 32          # 每 tick 最多重新发送的区块数（削峰）
  show-progress: true          # 动作栏进度显示

cooldown:
  enabled: true                # 冷却开关
  seconds: 10                  # 冷却秒数
```

`messages.yml`：所有文本（前缀、提示、帮助）都可配置，格式为 **MiniMessage**，占位符统一使用 `%key%` 写法。视距模式使用 `refreshing-view`（可用 `%view%`、`%extra%`、`%radius%`），固定半径使用 `refreshing`。消息体内可用 `%prefix%` 引用前缀。

> 调优建议：视距模式的单次刷新较重（数百个区块包）。若玩家反馈卡顿，可调小 `chunks-per-tick`（更平滑但更慢）或调大 `cooldown.seconds`；反之可调大。

## 安装

1. 将 `chunkrefresher-<version>.jar` 放入服务端 `plugins/` 目录。
2. 重启服务端（或 `/reload confirm`）。
3. 首次启动后按需修改 `plugins/ChunkRefresher/config.yml` 与 `messages.yml`，再执行 `/refreshchunk reload`。

## 构建

无需本地编译，推送到 GitHub 后由 Actions 构建（`.github/workflows/build.yml`，JDK 25 + Gradle wrapper）：

- 手动触发 workflow，或在 `main` / `master` 分支 push；
- 产物：`build/libs/chunkrefresher-<version>.jar`（同时作为 artifact 上传）；
- 手动触发时填写 `tag`（如 `v1.0.0`）会额外创建 GitHub Release。

本地如需构建：`./gradlew build`（需要 JDK 25；依赖 `io.papermc.paper:paper-api:26.2.build.98-stable`）。

单元测试（`./gradlew test`，CI 中会执行）：`ChunkAreaTest`（区域规模与由近到远排序）、`ViewDistanceTest`（视距 + N 半径规则）、`PlayerRefreshDataTest`（冷却与任务句柄生命周期）。

## 实现要点

| 关注点 | 做法 |
| --- | --- |
| 线程边界 | 刷新任务通过调度器适配层执行：普通 Paper 用 `BukkitScheduler`（主线程），Folia 用 `EntityScheduler`（该玩家所在的区域线程），区块读写始终发生在正确线程 |
| 分批削峰 | `tasks/ChunkRefreshTask` 每 tick 处理固定数量的区块，动作栏显示进度 |
| 范围计算 | `utils/ViewDistance` 为纯函数（便于单测）：取服务端/客户端视距较小值，再叠加额外圈数 |
| 状态建模 | 每个玩家的冷却与任务句柄收敛在 `model/PlayerRefreshData` 中，由 `managers/PlayerDataManager` 统一管理 |
| 并发控制 | 同一玩家同时只允许一个刷新任务；玩家退出（`PlayerQuitListener`）与插件禁用/重载时都会取消任务并清理状态 |
| 配置驱动 | 所有数值、开关、文本均在 `config.yml` / `messages.yml`，`/refreshchunk reload` 热重载后重读缓存 |
| 安全 | 半径三级约束（视距/`max-radius` → `absolute-max-radius` 硬上限），权限节点校验，命令输入做整数与负数校验 |

## 目录结构

```
src/main/java/github/xhserver123/chunkrefresher/
├── ChunkRefresherPlugin.java        # 主类
├── commands/RefreshChunkCommand.java
├── config/PluginConfig.java         # config.yml 缓存
├── config/MessageConfig.java        # messages.yml 缓存与 MiniMessage 渲染
├── config/RefreshMode.java          # 默认范围模式枚举（view-distance / fixed）
├── listeners/PlayerQuitListener.java
├── managers/PlayerDataManager.java  # 玩家状态集合
├── managers/RefreshManager.java     # 刷新流程编排（视距 / 固定半径）
├── model/PlayerRefreshData.java     # 单玩家状态（冷却 + 任务句柄）
├── scheduler/                       # 调度器适配层（Paper / Folia）
├── tasks/ChunkRefreshTask.java      # 分批刷新任务
├── utils/ChunkArea.java             # 区域偏移计算
└── utils/ViewDistance.java          # 视距 + N 半径计算
src/main/resources/
├── plugin.yml
├── config.yml
└── messages.yml
src/test/java/...                    # JUnit 单元测试（区域 / 视距 / 玩家状态）
```

## 参考

- [froobynooby/SeeMore](https://github.com/froobynooby/SeeMore)：Paper 插件的命令 / 配置 / 调度器适配层组织方式（本插件的 `scheduler/` 分层参考其 `SchedulerHook` 思路，视距获取与其“按客户端渲染距离取视距”一致）

## 许可证

MIT，见 [LICENSE](LICENSE)
