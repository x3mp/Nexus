package dev.nexus.hub.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Manages the Jedis connection pool for the hub plugin.
 *
 * <p>Call {@link #getResource()} to borrow a connection; always close it in a try-with-resources
 * block. Call {@link #close()} on plugin disable to drain the pool.
 *
 * @since 1.0.0
 */
public class RedisManager {

    private final JedisPool pool;

    /**
     * @param host     Redis server hostname or IP
     * @param port     Redis server port
     * @param password Redis password, or empty string for no authentication
     * @param poolSize maximum number of pooled connections
     */
    public RedisManager(String host, int port, String password, int poolSize) {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(poolSize);
        config.setMaxIdle(poolSize / 2);
        config.setMinIdle(1);
        config.setTestOnBorrow(true);

        if (password != null && !password.isEmpty()) {
            pool = new JedisPool(config, host, port, 2000, password);
        } else {
            pool = new JedisPool(config, host, port, 2000);
        }
    }

    /**
     * Borrows a Jedis connection from the pool.
     *
     * <p>Must be used in a try-with-resources block:
     * <pre>{@code
     * try (Jedis jedis = redisManager.getResource()) {
     *     jedis.set("key", "value");
     * }
     * }</pre>
     *
     * @return an active Jedis connection
     */
    public Jedis getResource() {
        return pool.getResource();
    }

    /**
     * Drains and closes the connection pool. Call on plugin disable.
     */
    public void close() {
        if (pool != null && !pool.isClosed()) {
            pool.close();
        }
    }
}
