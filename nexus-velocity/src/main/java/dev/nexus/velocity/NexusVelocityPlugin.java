package dev.nexus.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.nexus.velocity.messaging.VelocityRedisListener;
import dev.nexus.velocity.redis.RedisManager;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Main plugin class for the Nexus Velocity proxy component.
 *
 * <p>Handles dynamic server registration and player routing by subscribing to the
 * Redis pub/sub channel {@code nexus:velocity} where the hub publishes control packets.
 *
 * @since 1.0.0
 */
@Plugin(
        id = "nexus-velocity",
        name = "NexusVelocity",
        version = "1.0.0-SNAPSHOT",
        description = "Nexus proxy-side plugin. Handles dynamic server registration and player routing.",
        authors = {"hcqrv"}
)
public class NexusVelocityPlugin {

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;

    private RedisManager redisManager;
    private ServerRegistry serverRegistry;
    private PlayerRouter playerRouter;
    private VelocityRedisListener redisListener;

    /**
     * @param proxy         Velocity proxy instance, injected by Guice
     * @param logger        SLF4J logger, injected by Guice
     * @param dataDirectory plugin data directory, injected by Guice
     */
    @Inject
    public NexusVelocityPlugin(ProxyServer proxy, Logger logger,
                                @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        Map<String, Object> config = loadConfig();

        String host = getStr(config, "127.0.0.1", "nexus", "redis", "host");
        int port = getInt(config, 6379, "nexus", "redis", "port");
        String password = getStr(config, "", "nexus", "redis", "password");

        java.util.logging.Logger jul = java.util.logging.Logger.getLogger("NexusVelocity");

        redisManager = new RedisManager(host, port, password);
        serverRegistry = new ServerRegistry(proxy, redisManager, jul);
        playerRouter = new PlayerRouter(proxy, jul);

        redisListener = new VelocityRedisListener(redisManager, serverRegistry, playerRouter, jul);
        redisListener.start();

        logger.info("NexusVelocity enabled.");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (redisListener != null) redisListener.stop();
        if (redisManager != null) redisManager.close();
        logger.info("NexusVelocity disabled.");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadConfig() {
        try {
            if (!Files.exists(dataDirectory)) Files.createDirectories(dataDirectory);
            Path configFile = dataDirectory.resolve("config.yml");
            if (!Files.exists(configFile)) {
                try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
                    if (in != null) Files.copy(in, configFile);
                }
            }
            try (InputStream in = Files.newInputStream(configFile)) {
                Map<String, Object> result = new Yaml().load(in);
                return result != null ? result : Map.of();
            }
        } catch (IOException e) {
            logger.error("Failed to load config.yml: {}", e.getMessage());
            return Map.of();
        }
    }

    @SuppressWarnings("unchecked")
    private String getStr(Map<String, Object> map, String def, String... path) {
        Object cur = map;
        for (String key : path) {
            if (!(cur instanceof Map<?, ?> m)) return def;
            cur = ((Map<String, Object>) m).get(key);
        }
        return cur instanceof String s ? s : def;
    }

    @SuppressWarnings("unchecked")
    private int getInt(Map<String, Object> map, int def, String... path) {
        Object cur = map;
        for (String key : path) {
            if (!(cur instanceof Map<?, ?> m)) return def;
            cur = ((Map<String, Object>) m).get(key);
        }
        return cur instanceof Number n ? n.intValue() : def;
    }
}
