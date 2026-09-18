package github.xhserver123.chunkrefresher.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Folia（区域化多线程服务端）使用的调度器实现。
 *
 * <p>区块数据的读写必须发生在拥有该玩家的区域线程上，因此任务通过
 * {@code EntityScheduler} 调度；在普通 Paper 上不会走到这里。</p>
 */
public final class RegionisedSchedulerHook implements SchedulerHook {

    /** Folia 独有的标记类：能加载到说明当前服务端是 Folia 系。 */
    private static final String FOLIA_MARKER_CLASS = "io.papermc.paper.threadedregions.RegionizedServer";

    /**
     * 检测当前服务端是否为 Folia 系（区域化线程）。
     *
     * @return 是 Folia 返回 true
     */
    public static boolean isSupported() {
        try {
            Class.forName(FOLIA_MARKER_CLASS, false, RegionisedSchedulerHook.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Folia 的初始延迟必须大于等于 1 tick，这里统一收敛；返回 null 表示实体已失效。</p>
     */
    @Override
    public @NotNull TaskHandle scheduleRepeating(@NotNull Plugin plugin, @NotNull Entity entity,
                                                 @NotNull Runnable action, long initialDelayTicks, long periodTicks) {
        long delay = Math.max(1L, initialDelayTicks);
        long period = Math.max(1L, periodTicks);
        ScheduledTask task = entity.getScheduler().runAtFixedRate(plugin, scheduled -> action.run(), null, delay, period);
        return new RegionisedTaskHandle(task);
    }

    /**
     * {@link ScheduledTask} 的句柄包装。
     */
    private static final class RegionisedTaskHandle implements TaskHandle {

        private final ScheduledTask task;

        /**
         * @param task Folia 任务；可能为 null（实体已被移除）
         */
        private RegionisedTaskHandle(ScheduledTask task) {
            this.task = task;
        }

        @Override
        public void cancel() {
            if (task != null) {
                task.cancel();
            }
        }

        @Override
        public boolean isCancelled() {
            return task == null || task.isCancelled();
        }
    }
}
