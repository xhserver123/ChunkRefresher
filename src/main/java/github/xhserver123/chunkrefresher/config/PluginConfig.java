package github.xhserver123.chunkrefresher.config;

import github.xhserver123.chunkrefresher.ChunkRefresherPlugin;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * config.yml 的内存缓存。
 *
 * <p>启动与 {@code /refreshchunk reload} 时调用 {@link #reload()} 重新解析，
 * 业务代码只读取本类字段，避免散落的 getConfig() 调用与硬编码默认值。</p>
 */
public final class PluginConfig {

    private final ChunkRefresherPlugin plugin;

    private RefreshMode defaultMode;
    private int viewDistanceExtra;
    private int defaultRadius;
    private int maxRadius;
    private int absoluteMaxRadius;
    private int chunksPerTick;
    private boolean showProgress;

    private boolean cooldownEnabled;
    private int cooldownSeconds;

    /**
     * 构造配置缓存。
     *
     * @param plugin 插件主类实例
     */
    public PluginConfig(ChunkRefresherPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 重新读取 config.yml 并刷新全部缓存字段，同时做合法性收敛，避免非法配置导致运行期异常。
     */
    public void reload() {
        FileConfiguration c = plugin.getConfig();

        // ---- 刷新行为 ----
        absoluteMaxRadius = Math.max(1, c.getInt("refresh.absolute-max-radius", 32));
        maxRadius = clamp(c.getInt("refresh.max-radius", 16), 0, absoluteMaxRadius);
        defaultRadius = clamp(c.getInt("refresh.default-radius", 1), 0, maxRadius);

        // 默认范围模式：view-distance（视距 + N）或 fixed（固定半径）
        String rawMode = c.getString("refresh.default-mode", RefreshMode.VIEW_DISTANCE.getConfigName());
        RefreshMode parsedMode = RefreshMode.fromString(rawMode);
        if (parsedMode == null) {
            plugin.getLogger().warning("config.yml 的 refresh.default-mode 取值非法：" + rawMode
                    + "，已回退为 " + RefreshMode.VIEW_DISTANCE.getConfigName());
            parsedMode = RefreshMode.VIEW_DISTANCE;
        }
        defaultMode = parsedMode;
        viewDistanceExtra = Math.max(0, c.getInt("refresh.view-distance-extra", 2));

        chunksPerTick = Math.max(1, c.getInt("refresh.chunks-per-tick", 32));
        showProgress = c.getBoolean("refresh.show-progress", true);

        // ---- 冷却 ----
        cooldownEnabled = c.getBoolean("cooldown.enabled", true);
        cooldownSeconds = Math.max(0, c.getInt("cooldown.seconds", 10));
    }

    /**
     * 数值收敛辅助方法。
     *
     * @param value 原始值
     * @param min   下限
     * @param max   上限
     * @return 收敛到 [min, max] 的值
     */
    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * @return 无参数执行命令时使用的默认范围模式
     */
    public RefreshMode getDefaultMode() {
        return defaultMode;
    }

    /**
     * @return 视距模式下额外附加的环数（默认 2，即“视距 + 2”）
     */
    public int getViewDistanceExtra() {
        return viewDistanceExtra;
    }

    /**
     * @return fixed 模式下无参数执行命令时使用的默认半径（区块），{@code 1} 表示 3×3 = 9 个区块
     */
    public int getDefaultRadius() {
        return defaultRadius;
    }

    /**
     * @return 普通玩家可用的最大半径（区块）
     */
    public int getMaxRadius() {
        return maxRadius;
    }

    /**
     * @return 硬上限：即使拥有 bypass 权限也不会超过该半径，防止误操作拖垮服务端
     */
    public int getAbsoluteMaxRadius() {
        return absoluteMaxRadius;
    }

    /**
     * @return 每 tick 最多重新发送的区块数量（削峰用）
     */
    public int getChunksPerTick() {
        return chunksPerTick;
    }

    /**
     * @return 是否用动作栏显示刷新进度
     */
    public boolean isShowProgress() {
        return showProgress;
    }

    /**
     * @return 冷却功能开关
     */
    public boolean isCooldownEnabled() {
        return cooldownEnabled;
    }

    /**
     * @return 冷却时间（秒）
     */
    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    /**
     * @return 冷却时间（毫秒）
     */
    public long getCooldownMillis() {
        return cooldownSeconds * 1000L;
    }

    /**
     * 将半径收敛到普通玩家允许的上限（{@code max-radius}）。
     *
     * @param radius 原始半径
     * @return 收敛后的半径
     */
    public int clampRadius(int radius) {
        return clamp(radius, 0, maxRadius);
    }

    /**
     * 将半径收敛到硬上限（{@code absolute-max-radius}，任何玩家都不会超过）。
     *
     * @param radius 原始半径
     * @return 收敛后的半径
     */
    public int clampHardRadius(int radius) {
        return clamp(radius, 0, absoluteMaxRadius);
    }
}
