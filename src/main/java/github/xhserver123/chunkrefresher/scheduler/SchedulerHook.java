package github.xhserver123.chunkrefresher.scheduler;

import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * 调度器适配层。
 *
 * <p>参考 SeeMore 的 SchedulerHook 设计：普通 Paper 使用 {@code BukkitScheduler}，
 * Folia 使用实体调度器（{@code EntityScheduler}），保证任务始终在该玩家所在的区域线程上执行。</p>
 */
public interface SchedulerHook {

    /**
     * 以固定周期在“该实体所属的线程”上调度任务。
     *
     * @param plugin            插件实例
     * @param entity            任务归属实体（玩家），用于确定执行线程
     * @param action            周期执行的动作
     * @param initialDelayTicks 首次执行延迟（tick）
     * @param periodTicks       执行周期（tick）
     * @return 可取消的任务句柄
     */
    @NotNull TaskHandle scheduleRepeating(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable action,
                                          long initialDelayTicks, long periodTicks);

    /**
     * 依据当前服务端类型选择合适的调度器实现。
     *
     * @return 调度器实现
     */
    static @NotNull SchedulerHook create() {
        if (RegionisedSchedulerHook.isSupported()) {
            return new RegionisedSchedulerHook();
        }
        return new BukkitSchedulerHook();
    }
}
