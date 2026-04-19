package dev.nexus.velocity.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Manages the Jedis connection pool for the Velocity plugin.
 *
 * @since 1.0.0
 */
public class RedisManager {

    private final JedisPool pool;

    /**
     * @param host     Redis server hostname or IP
     * @param port     Redis server port
     * @param password Redis password, or empty string for no authentication
     */
    public RedisManager(String host, int port, String password) {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(8);
        config.setMaxIdle(4);
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
     * @return an active Jedis connection; must be closed in a try-with-resources block
     */
    public Jedis getResource() {
        return pool.getResource();
    }

    /**
     * Drains and closes the pool. Call on proxy shutdown.
     */
    public void close() {
        if (pool != null && !pool.isClosed()) {
            pool.close();
        }
    }
}
