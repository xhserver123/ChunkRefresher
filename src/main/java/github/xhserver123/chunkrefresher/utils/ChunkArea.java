package github.xhserver123.chunkrefresher.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 区块区域计算工具。
 *
 * <p>把一个“以玩家所在区块为中心、半径 r 的正方形区域”展开成相对偏移列表，
 * 并按“由近到远”排序，让玩家脚下的区块最先被重新渲染，观感更自然。</p>
 */
public final class ChunkArea {

    /**
     * 工具类不允许实例化。
     */
    private ChunkArea() {
    }

    /**
     * 生成方形区域内所有区块的相对偏移（含中心）。
     *
     * @param radius 半径（区块）；0 表示仅中心一个区块
     * @return 相对偏移数组列表，每个元素为 {dx, dz}，按距离中心由近到远排序
     */
    public static List<int[]> squareOffsets(int radius) {
        int clamped = Math.max(0, radius);
        List<int[]> offsets = new ArrayList<>(sideLength(clamped) * sideLength(clamped));
        for (int dx = -clamped; dx <= clamped; dx++) {
            for (int dz = -clamped; dz <= clamped; dz++) {
                offsets.add(new int[]{dx, dz});
            }
        }
        // 距离平方升序：中心 → 外圈
        offsets.sort(Comparator.comparingInt(offset -> offset[0] * offset[0] + offset[1] * offset[1]));
        return offsets;
    }

    /**
     * 计算方形区域的边长（区块数）。
     *
     * @param radius 半径
     * @return 边长 {@code 2 * radius + 1}
     */
    public static int sideLength(int radius) {
        return Math.max(0, radius) * 2 + 1;
    }
}
