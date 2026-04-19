package dev.nexus.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import dev.nexus.velocity.redis.RedisManager;
import redis.clients.jedis.Jedis;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Dynamically registers and unregisters minion servers with the Velocity proxy.
 *
 * <p>Also mirrors server state to Redis under {@code nexus:servers} (Hash: serverName → host:port)
 * so hub servers can verify which servers Velocity knows about.
 *
 * @since 1.0.0
 */
public class ServerRegistry {

    private final ProxyServer proxy;
    private final RedisManager redis;
    private final Logger logger;

    /**
     * @param proxy  Velocity proxy instance
     * @param redis  shared Redis manager
     * @param logger plugin logger
     */
    public ServerRegistry(ProxyServer proxy, RedisManager redis, Logger logger) {
        this.proxy = proxy;
        this.redis = redis;
        this.logger = logger;
    }

    /**
     * Registers a minion server with the Velocity proxy.
     *
     * <p>If a server with the same name already exists it is replaced.
     *
     * @param serverName Velocity server name (e.g. {@code "minion-revenant-1"})
     * @param host       minion hostname or IP
     * @param port       minion port
     */
    public void register(String serverName, String host, int port) {
        ServerInfo info = new ServerInfo(serverName, new InetSocketAddress(host, port));
        proxy.registerServer(info);

        try (Jedis jedis = redis.getResource()) {
            jedis.hset("nexus:servers", serverName, host + ":" + port);
        }

        logger.info("Registered server: " + serverName + " (" + host + ":" + port + ")");
    }

    /**
     * Unregisters a minion server from the Velocity proxy.
     *
     * @param serverName Velocity server name to remove
     */
    public void unregister(String serverName) {
        Optional<RegisteredServer> existing = proxy.getServer(serverName);
        existing.ifPresent(s -> proxy.unregisterServer(s.getServerInfo()));

        try (Jedis jedis = redis.getResource()) {
            jedis.hdel("nexus:servers", serverName);
        }

        logger.info("Unregistered server: " + serverName);
    }
}
