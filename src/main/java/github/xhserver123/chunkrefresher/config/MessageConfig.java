package github.xhserver123.chunkrefresher.config;

import github.xhserver123.chunkrefresher.ChunkRefresherPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * messages.yml 的内存缓存与文本渲染工具。
 *
 * <p>文本格式为 MiniMessage（如 {@code <gray>文本</gray>}）；自定义占位符统一使用
 * {@code %key%} 写法，避免与 MiniMessage 的尖括号标签冲突。</p>
 */
public final class MessageConfig {

    /** 消息文件名。 */
    private static final String FILE_NAME = "messages.yml";

    private final ChunkRefresherPlugin plugin;
    private YamlConfiguration configuration;
    private String prefix;
    private List<String> helpLines;

    /**
     * 构造消息缓存。
     *
     * @param plugin 插件主类实例
     */
    public MessageConfig(ChunkRefresherPlugin plugin) {
        this.plugin = plugin;
        this.configuration = new YamlConfiguration();
        this.prefix = "";
        this.helpLines = Collections.emptyList();
    }

    /**
     * 重新读取 messages.yml（文件不存在时先释放默认文件），并刷新前缀与帮助文本缓存。
     */
    public void reload() {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) {
            // 默认文件随插件一起打包，首次运行释放到插件目录
            plugin.saveResource(FILE_NAME, false);
        }
        configuration = YamlConfiguration.loadConfiguration(file);
        prefix = configuration.getString("prefix", "");
        helpLines = new ArrayList<>(configuration.getStringList("help"));
    }

    /**
     * 读取 {@code messages.<key>} 原始文本。
     *
     * @param key 消息键
     * @return 原始文本；缺失时返回占位提示，便于排查配置
     */
    public @NotNull String raw(@NotNull String key) {
        return configuration.getString("messages." + key, "<red>缺少消息节点: messages." + key);
    }

    /**
     * 渲染一条消息为 Component（自动拼接 prefix，并替换 {@code %key%} 占位符）。
     *
     * @param key          消息键
     * @param placeholders 占位符键值对，按 {键, 值, 键, 值...} 顺序传入
     * @return 渲染后的文本组件
     */
    public @NotNull Component component(@NotNull String key, @NotNull String... placeholders) {
        String body = applyPlaceholders(raw(key), placeholders);
        // 支持在消息体内使用 %prefix% 引用前缀
        body = body.replace("%prefix%", prefix == null ? "" : prefix);
        return deserialize((prefix == null ? "" : prefix) + body);
    }

    /**
     * 向命令发送者发送一条消息。
     *
     * @param sender       接收者（玩家或控制台）
     * @param key          消息键
     * @param placeholders 占位符键值对
     */
    public void send(@NotNull CommandSender sender, @NotNull String key, @NotNull String... placeholders) {
        sender.sendMessage(component(key, placeholders));
    }

    /**
     * 通过动作栏向玩家发送一条消息。
     *
     * @param player       目标玩家
     * @param key          消息键
     * @param placeholders 占位符键值对
     */
    public void sendActionBar(@NotNull Player player, @NotNull String key, @NotNull String... placeholders) {
        player.sendActionBar(component(key, placeholders));
    }

    /**
     * 发送 messages.yml 中 {@code help} 列表的全部行。
     *
     * @param sender 接收者
     */
    public void sendHelp(@NotNull CommandSender sender) {
        for (String line : helpLines) {
            sender.sendMessage(deserialize((prefix == null ? "" : prefix) + line));
        }
    }

    /**
     * 将 {@code %key%} 占位符替换为实际值。
     *
     * @param text         原始文本
     * @param placeholders 占位符键值对（键值成对，奇数个时忽略最后一个）
     * @return 替换后的文本
     */
    private static String applyPlaceholders(String text, String... placeholders) {
        if (text == null) {
            return "";
        }
        String result = text;
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            result = result.replace("%" + placeholders[i] + "%", placeholders[i + 1]);
        }
        return result;
    }

    /**
     * 将 MiniMessage 文本解析为 Component；解析失败时退化为纯文本，避免因配置写错而抛异常。
     *
     * @param text MiniMessage 文本
     * @return 文本组件
     */
    private static @NotNull Component deserialize(String text) {
        String safe = text == null ? "" : text;
        try {
            return MiniMessage.miniMessage().deserialize(safe);
        } catch (RuntimeException ex) {
            return Component.text(safe);
        }
    }
}
