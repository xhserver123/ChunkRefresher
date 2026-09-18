package github.xhserver123.chunkrefresher;

import github.xhserver123.chunkrefresher.commands.RefreshChunkCommand;
import github.xhserver123.chunkrefresher.config.MessageConfig;
import github.xhserver123.chunkrefresher.config.PluginConfig;
import github.xhserver123.chunkrefresher.listeners.PlayerQuitListener;
import github.xhserver123.chunkrefresher.managers.PlayerDataManager;
import github.xhserver123.chunkrefresher.managers.RefreshManager;
import github.xhserver123.chunkrefresher.scheduler.SchedulerHook;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * ChunkRefresher 主类。
 *
 * <p>插件提供 {@code /refreshchunk} 命令，让玩家手动把以自己为中心的区块重新发送给客户端，
 * 从而触发客户端重新渲染（重新构建区块网格）。核心调用是 Paper 的
 * {@code World#refreshChunk(int, int)}；区块数量较多时由 {@link github.xhserver123.chunkrefresher.tasks.ChunkRefreshTask}
 * 按 tick 分批执行，避免一次性发送过多区块造成卡顿。</p>
 *
 * <p>所有可调参数都在 config.yml，所有文本都在 messages.yml，禁止硬编码（项目协作规范要求）。</p>
 */
public final class ChunkRefresherPlugin extends JavaPlugin {

    /** 消息文件默认资源名。 */
    private static final String MESSAGES_RESOURCE = "messages.yml";

    private PluginConfig pluginConfig;
    private MessageConfig messageConfig;
    private PlayerDataManager playerDataManager;
    private RefreshManager refreshManager;
    private SchedulerHook schedulerHook;

    /**
     * 插件启用：加载配置与消息、初始化管理器、注册命令与监听器。
     */
    @Override
    public void onEnable() {
        // 首次启动时生成默认配置（已存在则不覆盖）
        saveDefaultConfig();
        if (!new java.io.File(getDataFolder(), MESSAGES_RESOURCE).exists()) {
            saveResource(MESSAGES_RESOURCE, false);
        }

        pluginConfig = new PluginConfig(this);
        pluginConfig.reload();

        messageConfig = new MessageConfig(this);
        messageConfig.reload();

        playerDataManager = new PlayerDataManager();
        refreshManager = new RefreshManager(this, pluginConfig, messageConfig, playerDataManager);

        // 依据服务端类型选择调度器（Folia 需要实体调度器，普通 Paper 使用 Bukkit 调度器）
        schedulerHook = SchedulerHook.create();

        registerCommand();
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(playerDataManager), this);

        getLogger().info("ChunkRefresher 已启用，调度器实现：" + schedulerHook.getClass().getSimpleName());
    }

    /**
     * 插件禁用：取消所有进行中的刷新任务，释放玩家状态。
     */
    @Override
    public void onDisable() {
        if (playerDataManager != null) {
            // 逐个取消任务并清空缓存，保证重载/卸载后没有残留任务
            playerDataManager.reset();
        }
        // 兜底：取消本插件遗留的所有 Bukkit 任务
        getServer().getScheduler().cancelTasks(this);
    }

    /**
     * 热重载：重新读取 config.yml 与 messages.yml，并清理内存缓存、进行中的任务与冷却状态。
     *
     * <p>对应规范 5.4：重载时必须让内存缓存、定时任务、游戏状态保持一致。</p>
     */
    public void reloadPlugin() {
        reloadConfig();
        pluginConfig.reload();
        messageConfig.reload();
        playerDataManager.reset();
    }

    /**
     * 注册 {@code /refreshchunk} 命令与补全。
     */
    private void registerCommand() {
        PluginCommand command = getCommand("refreshchunk");
        if (command == null) {
            getLogger().severe("未能在 plugin.yml 中找到 refreshchunk 命令，命令功能不可用。");
            return;
        }
        RefreshChunkCommand executor = new RefreshChunkCommand(this, pluginConfig, messageConfig, refreshManager);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    /**
     * @return 配置缓存
     */
    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    /**
     * @return 消息缓存
     */
    public MessageConfig getMessageConfig() {
        return messageConfig;
    }

    /**
     * @return 玩家状态管理器
     */
    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    /**
     * @return 刷新任务管理器
     */
    public RefreshManager getRefreshManager() {
        return refreshManager;
    }

    /**
     * @return 当前生效的调度器实现
     */
    public SchedulerHook getSchedulerHook() {
        return schedulerHook;
    }
}
