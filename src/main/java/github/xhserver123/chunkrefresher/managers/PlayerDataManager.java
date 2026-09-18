package github.xhserver123.chunkrefresher.managers;

import github.xhserver123.chunkrefresher.model.PlayerRefreshData;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 玩家刷新状态管理器。
 *
 * <p>持有 {@link PlayerRefreshData} 集合，负责创建、丢弃（玩家退出）与整体重置
 * （插件禁用 / 配置重载）。</p>
 */
public final class PlayerDataManager {

    private final Map<UUID, PlayerRefreshData> dataMap = new HashMap<>();

    /**
     * 获取玩家状态，不存在时创建。
     *
     * @param player 玩家
     * @return 该玩家的状态对象
     */
    public @NotNull PlayerRefreshData getOrCreate(@NotNull Player player) {
        return dataMap.computeIfAbsent(player.getUniqueId(), PlayerRefreshData::new);
    }

    /**
     * 丢弃玩家状态并取消其进行中的任务（玩家退出时调用）。
     *
     * @param playerId 玩家 UUID
     */
    public void discard(@NotNull UUID playerId) {
        PlayerRefreshData data = dataMap.remove(playerId);
        if (data != null) {
            data.cancelTask();
        }
    }

    /**
     * 取消全部进行中的任务并清空状态（插件禁用 / 重载时调用）。
     */
    public void reset() {
        for (PlayerRefreshData data : dataMap.values()) {
            data.cancelTask();
        }
        dataMap.clear();
    }
}
