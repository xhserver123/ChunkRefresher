package github.xhserver123.chunkrefresher.config;

import org.jetbrains.annotations.Nullable;

/**
 * 默认刷新范围模式（config.yml 的 {@code refresh.default-mode}）。
 */
public enum RefreshMode {

    /** 按玩家实际视距刷新：半径 = min(服务端下发视距, 客户端渲染距离) + refresh.view-distance-extra。 */
    VIEW_DISTANCE("view-distance"),

    /** 按固定半径刷新：使用 refresh.default-radius。 */
    FIXED("fixed");

    private final String configName;

    RefreshMode(String configName) {
        this.configName = configName;
    }

    /**
     * @return 配置文件中的写法
     */
    public String getConfigName() {
        return configName;
    }

    /**
     * 解析配置值，无法识别时返回 null 由调用方决定回退策略。
     *
     * @param raw 配置原始值
     * @return 对应模式；无法识别时为 null
     */
    public static @Nullable RefreshMode fromString(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().toLowerCase(java.util.Locale.ROOT);
        for (RefreshMode mode : values()) {
            if (mode.configName.equals(normalized) || mode.name().equalsIgnoreCase(normalized)) {
                return mode;
            }
        }
        return null;
    }
}
