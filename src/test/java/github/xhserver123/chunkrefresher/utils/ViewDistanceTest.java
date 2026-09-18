package github.xhserver123.chunkrefresher.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link ViewDistance} 的单元测试：验证“视距 + N”半径的取值规则。
 */
class ViewDistanceTest {

    /**
     * 取客户端与服务端视距的较小值，再叠加额外环数。
     */
    @Test
    void shouldUseSmallerViewDistancePlusExtra() {
        // 服务端限制更紧：8 + 2
        assertEquals(10, ViewDistance.resolveRadius(16, 8, 2));
        // 客户端限制更紧：6 + 2
        assertEquals(8, ViewDistance.resolveRadius(6, 12, 2));
        // 两端一致：10 + 2
        assertEquals(12, ViewDistance.resolveRadius(10, 10, 2));
    }

    /**
     * 边界：视距至少按 1 计（客户端尚未上报时 Paper 返回 2），附加环数不为负。
     */
    @Test
    void shouldHandleEdgeValues() {
        assertEquals(3, ViewDistance.resolveRadius(2, 2, 1));
        assertEquals(2, ViewDistance.resolveRadius(2, 2, 0));
        // 负数附加圈数按 0 处理
        assertEquals(2, ViewDistance.resolveRadius(2, 2, -5));
        // 视距为 0 时按 1 计，再叠加额外圈数
        assertEquals(3, ViewDistance.resolveRadius(0, 0, 2));
    }
}
