package github.xhserver123.chunkrefresher.commands;

import github.xhserver123.chunkrefresher.ChunkRefresherPlugin;
import github.xhserver123.chunkrefresher.config.MessageConfig;
import github.xhserver123.chunkrefresher.config.PluginConfig;
import github.xhserver123.chunkrefresher.managers.RefreshManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * {@code /refreshchunk} 命令。
 *
 * <p>用法：</p>
 * <ul>
 *     <li>{@code /refreshchunk} —— 按默认模式刷新（默认“视距 + 2”，即玩家附近全部视距再多刷 2 圈）</li>
 *     <li>{@code /refreshchunk view} —— 强制按当前视距 + 配置的额外圈数刷新</li>
 *     <li>{@code /refreshchunk <半径>} —— 按固定半径（区块）刷新，{@code 1} 表示 3×3</li>
 *     <li>{@code /refreshchunk here} —— 只刷新当前所在区块</li>
 *     <li>{@code /refreshchunk reload} —— 热重载 config.yml 与 messages.yml</li>
 *     <li>{@code /refreshchunk help} —— 显示帮助</li>
 * </ul>
 *
 * <p>权限在代码中校验（而不是写在 plugin.yml 的 permission 字段里），这样可以输出自定义的提示文本。</p>
 */
public final class RefreshChunkCommand implements CommandExecutor, TabCompleter {

    /** 发起刷新所需的权限。 */
    private static final String PERMISSION_USE = "chunkrefresher.use";
    /** 热重载所需的权限。 */
    private static final String PERMISSION_RELOAD = "chunkrefresher.reload";
    /** 忽略冷却所需的权限。 */
    private static final String PERMISSION_BYPASS_COOLDOWN = "chunkrefresher.bypass.cooldown";
    /** 突破 max-radius 限制所需的权限。 */
    private static final String PERMISSION_BYPASS_LIMIT = "chunkrefresher.bypass.limit";

    private static final String SUB_HELP = "help";
    private static final String SUB_RELOAD = "reload";
    private static final String SUB_HERE = "here";
    private static final String SUB_VIEW = "view";

    /** 半径补全候选。 */
    private static final List<String> RADIUS_SUGGESTIONS = List.of("0", "2", "4", "8", "16");

    private final ChunkRefresherPlugin plugin;
    private final PluginConfig config;
    private final MessageConfig messages;
    private final RefreshManager refreshManager;

    /**
     * 构造命令执行器。
     *
     * @param plugin         插件主类实例
     * @param config         配置缓存
     * @param messages       消息缓存
     * @param refreshManager 刷新流程管理器
     */
    public RefreshChunkCommand(ChunkRefresherPlugin plugin, PluginConfig config, MessageConfig messages,
                               RefreshManager refreshManager) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.refreshManager = refreshManager;
    }

    /**
     * 处理 {@code /refreshchunk}。
     *
     * @param sender  命令发送者
     * @param command 命令对象
     * @param label   实际使用的别名
     * @param args    参数
     * @return 始终返回 true（错误用法也由本类给出提示）
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String[] args) {
        if (args.length == 1 && SUB_HELP.equalsIgnoreCase(args[0])) {
            messages.sendHelp(sender);
            return true;
        }
        if (args.length == 1 && SUB_RELOAD.equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission(PERMISSION_RELOAD)) {
                messages.send(sender, "no-permission");
                return true;
            }
            plugin.reloadPlugin();
            messages.send(sender, "reload-success");
            return true;
        }
        if (args.length > 1) {
            messages.send(sender, "usage");
            return true;
        }

        // 刷新操作必须由玩家发起：需要以玩家所在位置为中心
        if (!(sender instanceof Player player)) {
            messages.send(sender, "player-only");
            return true;
        }
        if (!player.hasPermission(PERMISSION_USE)) {
            messages.send(player, "no-permission");
            return true;
        }

        int radius;
        if (args.length == 0) {
            // 无参数：交由配置决定（默认为视距 + 额外圈数），不做 max-radius 收敛，
            // 否则“刷新玩家附近全部视距”会被小半径配置截断
            refreshManager.startDefaultRefresh(player, player.hasPermission(PERMISSION_BYPASS_COOLDOWN));
            return true;
        } else if (SUB_VIEW.equalsIgnoreCase(args[0])) {
            // 显式视距模式
            refreshManager.startViewDistanceRefresh(player, config.getViewDistanceExtra(),
                    player.hasPermission(PERMISSION_BYPASS_COOLDOWN));
            return true;
        } else if (SUB_HERE.equalsIgnoreCase(args[0])) {
            radius = 0;
        } else {
            try {
                radius = Integer.parseInt(args[0]);
            } catch (NumberFormatException ex) {
                messages.send(player, "invalid-radius", "input", args[0]);
                return true;
            }
            if (radius < 0) {
                messages.send(player, "invalid-radius", "input", args[0]);
                return true;
            }
        }

        // 固定半径：普通玩家受 max-radius 限制，持有 bypass 权限的玩家仍受硬上限约束
        int limited = config.clampRadius(radius);
        if (limited != radius && !player.hasPermission(PERMISSION_BYPASS_LIMIT)) {
            radius = limited;
            messages.send(player, "radius-clamped", "max", String.valueOf(limited));
        }

        refreshManager.startFixedRadiusRefresh(player, radius, player.hasPermission(PERMISSION_BYPASS_COOLDOWN));
        return true;
    }

    /**
     * 为 {@code /refreshchunk} 提供补全。
     *
     * @param sender  命令发送者
     * @param command 命令对象
     * @param alias   命令别名
     * @param args    已输入参数
     * @return 候选列表
     */
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
                                      @NotNull String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }

        List<String> candidates = new ArrayList<>();
        candidates.add(SUB_HERE);
        candidates.add(SUB_VIEW);
        candidates.add(SUB_HELP);
        if (sender.hasPermission(PERMISSION_RELOAD)) {
            candidates.add(SUB_RELOAD);
        }
        candidates.addAll(RADIUS_SUGGESTIONS);

        // 只保留匹配当前输入前缀的候选，避免无关提示
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String candidate : candidates) {
            if (candidate.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                result.add(candidate);
            }
        }
        return result;
    }
}
