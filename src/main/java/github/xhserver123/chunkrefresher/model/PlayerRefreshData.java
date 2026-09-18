package github.xhserver123.chunkrefresher.model;

import github.xhserver123.chunkrefresher.scheduler.TaskHandle;

import java.util.UUID;

/**
 * 单个玩家的刷新状态（有状态对象，避免散落的 Map 与布尔值）。
 *
 * <p>保存两类信息：最近一次刷新的时间戳（用于冷却判定）与当前进行中的刷新任务句柄
 * （用于“同时只允许一个刷新任务”和退出/重载时取消任务）。</p>
 */
public final class PlayerRefreshData {

    private final UUID playerId;
    private long lastRefreshMillis;
    private TaskHandle currentTask;

    /**
     * 构造玩家状态。
     *
     * @param playerId 玩家 UUID
     */
    public PlayerRefreshData(UUID playerId) {
        this.playerId = playerId;
    }

    /**
     * @return 玩家 UUID
     */
    public UUID getPlayerId() {
        return playerId;
    }

    /**
     * @return 是否已有进行中的刷新任务
     */
    public boolean isRefreshing() {
        return currentTask != null && !currentTask.isCancelled();
    }

    /**
     * 记录本次刷新开始时间，用于冷却计算。
     */
    public void markRefreshed() {
        this.lastRefreshMillis = System.currentTimeMillis();
    }

    /**
     * 绑定进行中的任务句柄。
     *
     * @param task 任务句柄
     */
    public void setTask(TaskHandle task) {
        this.currentTask = task;
    }

    /**
     * 任务自然结束后的清理。
     */
    public void clearTask() {
        this.currentTask = null;
    }

    /**
     * 取消进行中的任务并清空句柄（用于退出、重载、禁用的强制清理）。
     */
    public void cancelTask() {
        if (currentTask != null) {
            currentTask.cancel();
            currentTask = null;
        }
    }

    /**
     * 计算剩余冷却时间。
     *
     * @param cooldownMillis 冷却总时长（毫秒）
     * @return 剩余毫秒数；已冷却完毕返回 0
     */
    public long getRemainingCooldownMillis(long cooldownMillis) {
        long elapsed = System.currentTimeMillis() - lastRefreshMillis;
        return Math.max(0L, cooldownMillis - elapsed);
    }
}
