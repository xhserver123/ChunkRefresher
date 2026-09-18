package github.xhserver123.chunkrefresher.tasks;

import github.xhserver123.chunkrefresher.config.MessageConfig;
import github.xhserver123.chunkrefresher.scheduler.TaskHandle;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * 分批刷新区块的任务。
 *
 * <p>每 tick 处理最多 {@code chunks-per-tick} 个区块，调用
 * {@code World#refreshChunk(int, int)} 让服务端把区块重新发送给跟踪它的客户端，
 * 从而触发客户端重新渲染。分批执行是为了削峰：一次性刷新上百个区块会造成明显的网络与
 * 客户端卡顿。</p>
 *
 * <p>线程边界：本任务由 {@link github.xhserver123.chunkrefresher.scheduler.SchedulerHook}
 * 调度在玩家所属线程（普通 Paper 为主线程，Folia 为该玩家所在的区域线程）上执行。</p>
 */
public final class ChunkRefreshTask implements Runnable {

    private final UUID playerId;
    private final World world;
    private final int centerChunkX;
    private final int centerChunkZ;
    private final List<int[]> offsets;
    private final MessageConfig messages;
    private final int chunksPerTick;
    private final boolean showProgress;
    private final Runnable onComplete;
    private final long startMillis;

    /** 已处理的偏移下标。 */
    private int cursor;
    /** 成功重新发送的区块数。 */
    private int refreshed;
    /** 跳过（未加载 / 无跟踪玩家）的区块数。 */
    private int skipped;
    /** 任务句柄，调度成功后由 {@link #attach(TaskHandle)} 注入。 */
    private TaskHandle handle;

    /**
     * 构造刷新任务。
     *
     * @param player       发起刷新的玩家
     * @param world        目标世界（玩家发起时所在世界）
     * @param centerChunkX 中心区块 X
     * @param centerChunkZ 中心区块 Z
     * @param offsets      相对偏移列表（由近到远）
     * @param messages     消息缓存
     * @param chunksPerTick 每 tick 处理上限
     * @param showProgress 是否用动作栏显示进度
     * @param onComplete   任务结束（自然完成或提前取消）后的回调，用于清理玩家状态
     */
    public ChunkRefreshTask(Player player, World world, int centerChunkX, int centerChunkZ,
                            List<int[]> offsets, MessageConfig messages, int chunksPerTick, boolean showProgress,
                            Runnable onComplete) {
        this.playerId = player.getUniqueId();
        this.world = world;
        this.centerChunkX = centerChunkX;
        this.centerChunkZ = centerChunkZ;
        this.offsets = offsets;
        this.messages = messages;
        this.chunksPerTick = Math.max(1, chunksPerTick);
        this.showProgress = showProgress;
        this.onComplete = onComplete;
        this.startMillis = System.currentTimeMillis();
    }

    /**
     * 注入调度器返回的任务句柄。
     *
     * @param handle 任务句柄
     */
    public void attach(TaskHandle handle) {
        this.handle = handle;
    }

    /**
     * 每 tick 执行一批区块刷新。
     */
    @Override
    public void run() {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            // 玩家已下线：静默取消，玩家状态由退出监听器清理
            cancel();
            return;
        }

        int processed = 0;
        while (cursor < offsets.size() && processed < chunksPerTick) {
            int[] offset = offsets.get(cursor++);
            processed++;

            int chunkX = centerChunkX + offset[0];
            int chunkZ = centerChunkZ + offset[1];

            // 未加载的区块客户端本来就没有渲染，跳过可避免无意义的加载开销
            if (!world.isChunkLoaded(chunkX, chunkZ)) {
                skipped++;
                continue;
            }
            if (world.refreshChunk(chunkX, chunkZ)) {
                refreshed++;
            } else {
                skipped++;
            }
        }

        if (cursor >= offsets.size()) {
            finish(player);
            return;
        }
        if (showProgress) {
            messages.sendActionBar(player, "progress",
                    "done", String.valueOf(cursor),
                    "total", String.valueOf(offsets.size()));
        }
    }

    /**
     * 任务收尾：取消周期调度、通知玩家、清理玩家状态。
     *
     * @param player 发起刷新的玩家
     */
    private void finish(Player player) {
        cancel();
        long elapsed = System.currentTimeMillis() - startMillis;

        // 先清理状态再发消息，保证玩家立刻可以发起下一次刷新
        onComplete.run();
        messages.send(player, "finished",
                "refreshed", String.valueOf(refreshed),
                "skipped", String.valueOf(skipped),
                "total", String.valueOf(offsets.size()),
                "time", String.valueOf(elapsed));
    }

    /**
     * 取消本任务；重复调用安全。
     */
    public void cancel() {
        if (handle != null) {
            handle.cancel();
        }
    }
}
