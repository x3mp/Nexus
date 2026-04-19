package dev.nexus.hub;

import dev.nexus.api.NexusAPI;
import dev.nexus.hub.command.NexusCommand;
import dev.nexus.hub.config.NexusConfig;
import dev.nexus.hub.game.GameRegistry;
import dev.nexus.hub.listener.PlayerQuitListener;
import dev.nexus.hub.messaging.HubMessageListener;
import dev.nexus.hub.messaging.HubMessageSender;
import dev.nexus.hub.minion.MinionManager;
import dev.nexus.hub.minion.MinionRegistry;
import dev.nexus.hub.queue.MatchmakingTask;
import dev.nexus.hub.queue.QueueManager;
import dev.nexus.hub.redis.RedisManager;
import dev.nexus.hub.turret.TurretProvisioner;
import dev.nexus.hub.turret.TurretRegistry;
import dev.nexus.hub.turret.TurretSelector;
import dev.nexus.hub.turret.selector.LeastLoadedSelector;
import dev.nexus.hub.turret.selector.RandomSelector;
import dev.nexus.hub.turret.selector.RoundRobinSelector;
import dev.nexus.api.messaging.NexusChannel;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for the Nexus hub.
 *
 * <p>Wires up all components on enable and tears them down on disable.
 * Game plugins register themselves via {@code NexusAPI.registerGame(new MyGame())}
 * in their own {@code onEnable}; nexus-hub drains that list one tick after startup.
 *
 * @since 1.0.0
 */
public class NexusHubPlugin extends JavaPlugin {

    private static NexusHubPlugin instance;

    private NexusConfig nexusConfig;
    private RedisManager redisManager;
    private GameRegistry gameRegistry;
    private TurretRegistry turretRegistry;
    private MinionRegistry minionRegistry;
    private TurretProvisioner turretProvisioner;
    private TurretSelector turretSelector;
    private QueueManager queueManager;
    private MinionManager minionManager;
    private HubMessageSender messageSender;
    private MatchmakingTask matchmakingTask;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        nexusConfig = new NexusConfig(getConfig());

        redisManager = new RedisManager(
                nexusConfig.getRedisHost(),
                nexusConfig.getRedisPort(),
                nexusConfig.getRedisPassword(),
                nexusConfig.getRedisPoolSize());

        gameRegistry = new GameRegistry(getLogger());

        turretRegistry = new TurretRegistry(redisManager, getLogger());
        turretRegistry.loadFromConfig(nexusConfig.getTurrets());

        minionRegistry = new MinionRegistry(redisManager);

        turretSelector = buildSelector(nexusConfig.getTurretSelectorStrategy());
        messageSender = new HubMessageSender(redisManager, getLogger());

        minionManager = new MinionManager(
                gameRegistry, turretRegistry, turretSelector,
                getTurretProvisioner(), minionRegistry, messageSender,
                nexusConfig.getBootTimeoutSeconds(), getLogger());

        queueManager = new QueueManager(redisManager);

        matchmakingTask = new MatchmakingTask(
                gameRegistry, queueManager, minionManager, redisManager, nexusConfig);
        long tickTicks = nexusConfig.getMatchmakingTickIntervalMs() / 50;
        matchmakingTask.runTaskTimerAsynchronously(this, tickTicks, tickTicks);

        getServer().getMessenger().registerIncomingPluginChannel(this, NexusChannel.MAIN,
                new HubMessageListener(minionManager, minionRegistry, turretRegistry,
                        nexusConfig.getReturnServer(), getLogger()));
        getServer().getMessenger().registerOutgoingPluginChannel(this, NexusChannel.MAIN);

        getServer().getPluginManager().registerEvents(
                new PlayerQuitListener(queueManager), this);

        NexusCommand nexusCommand = new NexusCommand(
                queueManager, minionRegistry, minionManager, turretRegistry);
        var nexusBukkitCommand = getCommand("nexus");
        if (nexusBukkitCommand != null) {
            nexusBukkitCommand.setExecutor(nexusCommand);
            nexusBukkitCommand.setTabCompleter(nexusCommand);
        } else {
            getLogger().severe("Command 'nexus' not found in plugin.yml — commands will not work.");
        }

        registerPlaceholders();

        getServer().getScheduler().runTask(this, () -> {
            for (dev.nexus.api.game.NexusGame game : NexusAPI.getRegisteredGames()) {
                gameRegistry.register(game);
            }
            getLogger().info("Loaded " + NexusAPI.getRegisteredGames().size() + " game(s) from NexusAPI.");
        });

        getLogger().info("NexusHub enabled.");
    }

    @Override
    public void onDisable() {
        if (matchmakingTask != null) matchmakingTask.cancel();
        if (minionManager != null) minionManager.shutdown();
        if (redisManager != null) redisManager.close();
        instance = null;
        getLogger().info("NexusHub disabled.");
    }

    /**
     * Returns the singleton instance of this plugin.
     *
     * <p>Available after {@code onEnable} completes; {@code null} after {@code onDisable}.
     *
     * @return the plugin instance
     */
    public static NexusHubPlugin getInstance() {
        return instance;
    }

    /** @return the game registry holding all registered {@link dev.nexus.api.game.NexusGame} implementations */
    public GameRegistry getGameRegistry() { return gameRegistry; }

    /** @return the Redis manager */
    public RedisManager getRedisManager() { return redisManager; }

    /** @return the minion registry */
    public MinionRegistry getMinionRegistry() { return minionRegistry; }

    /** @return the turret registry */
    public TurretRegistry getTurretRegistry() { return turretRegistry; }

    /** @return the queue manager */
    public QueueManager getQueueManager() { return queueManager; }

    /**
     * Allows operators to inject a custom {@link TurretProvisioner} implementation.
     *
     * <p>Must be called before {@code onEnable} completes if using a custom provisioner.
     *
     * @param provisioner the provisioner to use
     */
    public void setTurretProvisioner(TurretProvisioner provisioner) {
        this.turretProvisioner = provisioner;
    }

    private TurretProvisioner getTurretProvisioner() {
        if (turretProvisioner != null) return turretProvisioner;
        // No-op fallback so the plugin loads without a provisioner configured
        return new TurretProvisioner() {
            @Override
            public dev.nexus.api.minion.Minion provision(String gameId,
                    dev.nexus.api.turret.Turret turret, String mapName) {
                getLogger().warning("No TurretProvisioner registered — provision() is a no-op");
                return null;
            }
            @Override
            public void kill(String minionId) {
                getLogger().warning("No TurretProvisioner registered — kill() is a no-op");
            }
        };
    }

    private TurretSelector buildSelector(String strategy) {
        return switch (strategy.toUpperCase()) {
            case "ROUND_ROBIN" -> new RoundRobinSelector();
            case "RANDOM" -> new RandomSelector();
            default -> new LeastLoadedSelector();
        };
    }

    private void registerPlaceholders() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new dev.nexus.hub.placeholder.NexusPlaceholderExpansion(
                    queueManager, minionRegistry, turretRegistry).register();
            getLogger().info("PlaceholderAPI expansion registered.");
        }
    }
}
