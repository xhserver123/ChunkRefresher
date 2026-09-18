package github.xhserver123.chunkrefresher.managers;

import github.xhserver123.chunkrefresher.ChunkRefresherPlugin;
import github.xhserver123.chunkrefresher.config.MessageConfig;
import github.xhserver123.chunkrefresher.config.PluginConfig;
import github.xhserver123.chunkrefresher.config.RefreshMode;
import github.xhserver123.chunkrefresher.model.PlayerRefreshData;
import github.xhserver123.chunkrefresher.scheduler.TaskHandle;
import github.xhserver123.chunkrefresher.tasks.ChunkRefreshTask;
import github.xhserver123.chunkrefresher.utils.ChunkArea;
import github.xhserver123.chunkrefresher.utils.ViewDistance;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

/**
 * 刷新流程编排：范围解析、冷却与并发校验、任务调度。
 *
 * <p>命令类只负责参数解析与权限判断，真正的业务逻辑集中在这里，便于复用与测试。</p>
 *
 * <p>两种刷新范围：</p>
 * <ul>
 *     <li><b>视距模式</b>（默认）：半径 = min(服务端下发视距, 客户端渲染距离) + {@code view-distance-extra}，
 *         即“把玩家附近能看到的区块，再往外多刷 2 圈”；</li>
 *     <li><b>固定半径模式</b>：使用命令给出的半径（{@code /refreshchunk 4}）或配置的 {@code default-radius}。</li>
 * </ul>
 */
public final class RefreshManager {

    private final ChunkRefresherPlugin plugin;
    private final PluginConfig config;
    private final MessageConfig messages;
    private final PlayerDataManager playerDataManager;

    /**
     * 构造刷新管理器。
     *
     * @param plugin            插件主类实例
     * @param config            配置缓存
     * @param messages          消息缓存
     * @param playerDataManager 玩家状态管理器
     */
    public RefreshManager(ChunkRefresherPlugin plugin, PluginConfig config, MessageConfig messages,
                          PlayerDataManager playerDataManager) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.playerDataManager = playerDataManager;
    }

    /**
     * 按 config.yml 的 {@code refresh.default-mode} 启动一次刷新（无参数执行命令时使用）。
     *
     * @param player         发起刷新的玩家
     * @param ignoreCooldown 是否忽略冷却（拥有 bypass 权限时为 true）
     * @return 成功启动任务返回 true
     */
    public boolean startDefaultRefresh(@NotNull Player player, boolean ignoreCooldown) {
        if (config.getDefaultMode() == RefreshMode.FIXED) {
            return startFixedRadiusRefresh(player, config.getDefaultRadius(), ignoreCooldown);
        }
        return startViewDistanceRefresh(player, config.getViewDistanceExtra(), ignoreCooldown);
    }

    /**
     * 按玩家实际视距启动刷新：覆盖玩家当前能看到的区块，并额外多刷 {@code extra} 圈。
     *
     * <p>视距取“服务端下发的视距”与“客户端渲染距离”的较小值，因为玩家实际能看到的部分是两者的交集。</p>
     *
     * @param player         发起刷新的玩家
     * @param extra          额外附加的环数
     * @param ignoreCooldown 是否忽略冷却
     * @return 成功启动任务返回 true
     */
    public boolean startViewDistanceRefresh(@NotNull Player player, int extra, boolean ignoreCooldown) {
        int clientViewDistance = player.getClientViewDistance();
        int serverViewDistance = player.getViewDistance();
        int viewDistance = Math.max(1, Math.min(clientViewDistance, serverViewDistance));
        int radius = ViewDistance.resolveRadius(clientViewDistance, serverViewDistance, extra);
        return startRefresh(player, radius, ignoreCooldown, true, viewDistance, Math.max(0, extra));
    }

    /**
     * 按固定半径启动刷新。
     *
     * @param player         发起刷新的玩家
     * @param radius         半径（区块）
     * @param ignoreCooldown 是否忽略冷却
     * @return 成功启动任务返回 true
     */
    public boolean startFixedRadiusRefresh(@NotNull Player player, int radius, boolean ignoreCooldown) {
        return startRefresh(player, radius, ignoreCooldown, false, 0, 0);
    }

    /**
     * 启动刷新的统一入口。
     *
     * <p>必须由主线程（Folia 下为玩家所在区域线程）调用，因为会读取玩家位置并调度任务。</p>
     *
     * @param player         发起刷新的玩家
     * @param radius         期望半径（区块）
     * @param ignoreCooldown 是否忽略冷却
     * @param viewMode       是否为视距模式（决定使用的消息模板与占位符）
     * @param viewDistance   视距模式下的基础视距
     * @param extra          视距模式下的额外环数
     * @return 成功启动任务返回 true
     */
    private boolean startRefresh(@NotNull Player player, int radius, boolean ignoreCooldown, boolean viewMode,
                                 int viewDistance, int extra) {
        PlayerRefreshData data = playerDataManager.getOrCreate(player);

        // 同一玩家同时只允许一个刷新任务，避免大量重复发包
        if (data.isRefreshing()) {
            messages.send(player, "already-running");
            return false;
        }

        // 冷却校验
        if (config.isCooldownEnabled() && !ignoreCooldown) {
            long remaining = data.getRemainingCooldownMillis(config.getCooldownMillis());
            if (remaining > 0L) {
                messages.send(player, "cooldown",
                        "seconds", String.format(Locale.ROOT, "%.1f", remaining / 1000.0D));
                return false;
            }
        }

        // 硬上限兜底：即使调用方没有收敛，也不会超过 absolute-max-radius
        int effectiveRadius = config.clampHardRadius(radius);
        if (effectiveRadius != radius) {
            messages.send(player, "radius-clamped", "max", String.valueOf(effectiveRadius));
        }

        World world = player.getWorld();
        Chunk centerChunk = player.getLocation().getChunk();
        int centerChunkX = centerChunk.getX();
        int centerChunkZ = centerChunk.getZ();
        List<int[]> offsets = ChunkArea.squareOffsets(effectiveRadius);

        data.markRefreshed();
        if (viewMode) {
            messages.send(player, "refreshing-view",
                    "radius", String.valueOf(effectiveRadius),
                    "view", String.valueOf(viewDistance),
                    "extra", String.valueOf(extra),
                    "total", String.valueOf(offsets.size()),
                    "world", world.getName(),
                    "chunk-x", String.valueOf(centerChunkX),
                    "chunk-z", String.valueOf(centerChunkZ));
        } else {
            messages.send(player, "refreshing",
                    "radius", String.valueOf(effectiveRadius),
                    "total", String.valueOf(offsets.size()),
                    "world", world.getName(),
                    "chunk-x", String.valueOf(centerChunkX),
                    "chunk-z", String.valueOf(centerChunkZ));
        }

        ChunkRefreshTask task = new ChunkRefreshTask(player, world, centerChunkX, centerChunkZ, offsets,
                messages, config.getChunksPerTick(), config.isShowProgress(), data::clearTask);

        // 首 tick 延迟 1 tick、每 tick 执行一次；由玩家实体确定执行线程
        TaskHandle handle = plugin.getSchedulerHook().scheduleRepeating(plugin, player, task, 1L, 1L);
        task.attach(handle);
        data.setTask(handle);
        return true;
    }
}
