package github.xhserver123.chunkrefresher.utils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ChunkArea} 的单元测试：验证区域规模、中心优先与由近到远排序。
 */
class ChunkAreaTest {

    /**
     * 半径 r 的区域应包含 (2r + 1)² 个区块。
     */
    @Test
    void squareOffsetsShouldCoverWholeSquare() {
        assertEquals(9, ChunkArea.squareOffsets(1).size());
        assertEquals(25, ChunkArea.squareOffsets(2).size());
        assertEquals(289, ChunkArea.squareOffsets(8).size());
        assertEquals(9, ChunkArea.sideLength(1) * ChunkArea.sideLength(1));
    }

    /**
     * 半径 0 表示只有玩家所在区块；负数按 0 处理。
     */
    @Test
    void zeroRadiusShouldOnlyContainCenter() {
        List<int[]> offsets = ChunkArea.squareOffsets(0);
        assertEquals(1, offsets.size());
        assertEquals(0, offsets.get(0)[0]);
        assertEquals(0, offsets.get(0)[1]);
        assertEquals(1, ChunkArea.squareOffsets(-5).size());
    }

    /**
     * 列表必须由近到远排序，且第一个元素是中心（玩家脚下优先刷新）。
     */
    @Test
    void offsetsShouldBeSortedFromCenterOutwards() {
        List<int[]> offsets = ChunkArea.squareOffsets(3);
        assertEquals(0, offsets.get(0)[0]);
        assertEquals(0, offsets.get(0)[1]);

        int previous = -1;
        for (int[] offset : offsets) {
            int distanceSquared = offset[0] * offset[0] + offset[1] * offset[1];
            assertTrue(distanceSquared >= previous, "偏移列表必须按距离升序排列");
            previous = distanceSquared;
        }
    }
}
