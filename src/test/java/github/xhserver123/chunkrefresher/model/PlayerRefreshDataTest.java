package github.xhserver123.chunkrefresher.model;

import github.xhserver123.chunkrefresher.scheduler.TaskHandle;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link PlayerRefreshData} 的单元测试：验证冷却计算与任务句柄的生命周期。
 */
class PlayerRefreshDataTest {

    /**
     * 冷却期间 remaining 应为正且不超过冷却总时长；未刷新过则无冷却。
     */
    @Test
    void cooldownShouldBeCountedFromLastRefresh() {
        PlayerRefreshData data = new PlayerRefreshData(UUID.randomUUID());
        // 从未刷新过：没有冷却
        assertTrue(data.getRemainingCooldownMillis(10_000L) == 0L);

        data.markRefreshed();
        long remaining = data.getRemainingCooldownMillis(10_000L);
        assertTrue(remaining > 0L && remaining <= 10_000L);
        // 冷却配置为 0 时立即结束
        assertTrue(data.getRemainingCooldownMillis(0L) == 0L);
    }

    /**
     * 绑定任务后处于刷新中；取消任务后恢复可再次刷新。
     */
    @Test
    void taskHandleShouldControlRefreshingFlag() {
        PlayerRefreshData data = new PlayerRefreshData(UUID.randomUUID());
        AtomicBoolean cancelled = new AtomicBoolean(false);

        TaskHandle handle = new TaskHandle() {
            @Override
            public void cancel() {
                cancelled.set(true);
            }

            @Override
            public boolean isCancelled() {
                return cancelled.get();
            }
        };

        assertFalse(data.isRefreshing());
        data.setTask(handle);
        assertTrue(data.isRefreshing());

        data.cancelTask();
        assertTrue(cancelled.get(), "cancelTask 必须取消底层任务");
        assertFalse(data.isRefreshing());
    }
}
