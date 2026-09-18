package github.xhserver123.chunkrefresher.listeners;

import github.xhserver123.chunkrefresher.managers.PlayerDataManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

/**
 * 玩家退出监听：取消该玩家进行中的刷新任务，并释放其状态对象。
 *
 * <p>使用 MONITOR 优先级：只做收尾清理，不需要影响其它插件的处理逻辑。</p>
 */
public final class PlayerQuitListener implements Listener {

    private final PlayerDataManager playerDataManager;

    /**
     * 构造监听器。
     *
     * @param playerDataManager 玩家状态管理器
     */
    public PlayerQuitListener(PlayerDataManager playerDataManager) {
        this.playerDataManager = playerDataManager;
    }

    /**
     * 玩家退出时清理状态。
     *
     * @param event 玩家退出事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        playerDataManager.discard(event.getPlayer().getUniqueId());
    }
}
