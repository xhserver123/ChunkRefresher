package github.xhserver123.chunkrefresher.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

/**
 * 普通 Paper / Spigot 服务端使用的调度器实现（主线程调度）。
 */
public final class BukkitSchedulerHook implements SchedulerHook {

    /**
     * {@inheritDoc}
     *
     * <p>普通 Paper 只有主线程，实体参数仅用于接口对齐。</p>
     */
    @Override
    public @NotNull TaskHandle scheduleRepeating(@NotNull Plugin plugin, @NotNull Entity entity,
                                                 @NotNull Runnable action, long initialDelayTicks, long periodTicks) {
        long delay = Math.max(0L, initialDelayTicks);
        long period = Math.max(1L, periodTicks);
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, action, delay, period);
        return new BukkitTaskHandle(task);
    }

    /**
     * {@link BukkitTask} 的句柄包装。
     */
    private static final class BukkitTaskHandle implements TaskHandle {

        private final BukkitTask task;

        /**
         * @param task 底层 Bukkit 任务
         */
        private BukkitTaskHandle(BukkitTask task) {
            this.task = task;
        }

        @Override
        public void cancel() {
            task.cancel();
        }

        @Override
        public boolean isCancelled() {
            return task.isCancelled();
        }
    }
}
