package github.xhserver123.chunkrefresher.utils;

/**
 * 视距换算工具（纯函数，便于单元测试）。
 *
 * <p>玩家真正能看到/加载的范围受两端共同限制：服务端为该玩家下发的视距
 * （{@code Player#getViewDistance()}，上限由服务端配置决定）与客户端自己设置的渲染距离
 * （{@code Player#getClientViewDistance()}）。因此取两者较小值作为基础视距，
 * 再叠加配置的额外环数。</p>
 */
public final class ViewDistance {

    /**
     * 工具类不允许实例化。
     */
    private ViewDistance() {
    }

    /**
     * 计算“视距 + N”模式下的刷新半径（区块）。
     *
     * @param clientViewDistance 客户端渲染距离（区块）
     * @param serverViewDistance 服务端为该玩家下发的视距（区块）
     * @param extra              额外附加的环数，负数按 0 处理
     * @return 刷新半径（区块），至少为 1
     */
    public static int resolveRadius(int clientViewDistance, int serverViewDistance, int extra) {
        int base = Math.max(1, Math.min(clientViewDistance, serverViewDistance));
        return base + Math.max(0, extra);
    }
}
