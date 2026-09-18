package github.xhserver123.chunkrefresher.scheduler;

/**
 * 调度器返回的任务句柄抽象。
 *
 * <p>普通 Paper 与 Folia 的任务类型不同（{@code BukkitTask} / {@code ScheduledTask}），
 * 通过该接口统一取消与状态查询，业务代码无需关心具体实现。</p>
 */
public interface TaskHandle {

    /**
     * 取消任务；重复调用应当是安全的。
     */
    void cancel();

    /**
     * @return 任务是否已被取消（或从未成功调度）
     */
    boolean isCancelled();
}
