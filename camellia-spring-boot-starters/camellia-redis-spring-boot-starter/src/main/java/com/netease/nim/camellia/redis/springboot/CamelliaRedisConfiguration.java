package com.netease.nim.camellia.redis.springboot;

import com.netease.nim.camellia.core.client.env.ProxyEnv;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.FileUtil;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.core.util.ResourceTableUtil;
import com.netease.nim.camellia.core.util.ResourceUtil;
import com.netease.nim.camellia.redis.CamelliaRedisEnv;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.jedis.JedisPoolFactory;
import com.netease.nim.camellia.redis.jediscluster.JedisClusterFactory;
import com.netease.nim.camellia.redis.resource.RedisResourceUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPoolConfig;

import java.net.URL;
import java.util.Set;


/**
 *
 * Created by caojiajun on 2020/3/31.
 */
@Configuration
@EnableConfigurationProperties({CamelliaRedisProperties.class})
public class CamelliaRedisConfiguration {

    @Bean
    @ConditionalOnMissingBean(value = {ProxyEnv.class})
    public ProxyEnv proxyEnv() {
        return ProxyEnv.defaultProxyEnv();
    }

    @Bean
    public CamelliaRedisTemplate camelliaRedisTemplate(CamelliaRedisProperties properties) {
        CamelliaRedisProperties.Type type = properties.getType();
        CamelliaRedisProperties.RedisConf redisConf = properties.getRedisConf();
        CamelliaRedisEnv redisEnv = camelliaRedisEnv(redisConf);
        if (type == CamelliaRedisProperties.Type.LOCAL) {
            CamelliaRedisProperties.Local local = properties.getLocal();
            CamelliaRedisProperties.Local.Type localType = local.getType();
            if (localType == CamelliaRedisProperties.Local.Type.SIMPLE) {
                String url = local.getResource();
                Resource resource = RedisResourceUtil.parseResourceByUrl(new Resource(url));
                return new CamelliaRedisTemplate(redisEnv, ResourceTableUtil.simpleTable(resource));
            } else if (localType == CamelliaRedisProperties.Local.Type.COMPLEX) {
                String jsonFile = local.getJsonFile();
                if (jsonFile == null) {
                    throw new IllegalArgumentException("missing jsonFile");
                }
                URL resource = Thread.currentThread().getContextClassLoader().getResource(jsonFile);
                if (resource == null) {
                    throw new IllegalArgumentException("not found " + jsonFile);
                }
                String path = resource.getPath();
                String fileContent = FileUtil.readFile(path);
                if (fileContent == null) {
                    throw new IllegalArgumentException(jsonFile + " read fail");
                }
                ResourceTable resourceTable = ReadableResourceTableUtil.parseTable(fileContent);
                Set<Resource> allResources = ResourceUtil.getAllResources(resourceTable);
                for (Resource redisResource : allResources) {
                    RedisResourceUtil.parseResourceByUrl(redisResource);
                }
                return new CamelliaRedisTemplate(redisEnv, resourceTable);
            } else {
                throw new UnsupportedOperationException("only support simple/complex");
            }
        } else if (type == CamelliaRedisProperties.Type.REMOTE) {
            CamelliaRedisProperties.Remote remote = properties.getRemote();
            String url = remote.getUrl();
            long bid = remote.getBid();
            String bgroup = remote.getBgroup();
            boolean monitor = remote.isMonitor();
            int connectTimeoutMillis = remote.getConnectTimeoutMillis();
            int readTimeoutMillis = remote.getReadTimeoutMillis();
            long checkIntervalMillis = remote.getCheckIntervalMillis();
            return new CamelliaRedisTemplate(redisEnv, url, bid, bgroup, monitor, checkIntervalMillis, connectTimeoutMillis, readTimeoutMillis);
        } else {
            throw new UnsupportedOperationException("only support local/remote");
        }
    }

    private CamelliaRedisEnv camelliaRedisEnv(CamelliaRedisProperties.RedisConf redisConf) {
        CamelliaRedisProperties.RedisConf.Jedis jedis = redisConf.getJedis();
        JedisPoolFactory jedisPoolFactory;
        if (jedis != null) {
            int timeout = jedis.getTimeout();
            JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
            jedisPoolConfig.setMaxIdle(jedis.getMaxIdle());
            jedisPoolConfig.setMinIdle(jedis.getMinIdle());
            jedisPoolConfig.setMaxTotal(jedis.getMaxActive());
            jedisPoolConfig.setMaxWaitMillis(jedis.getMaxWaitMillis());
            jedisPoolFactory = new JedisPoolFactory.DefaultJedisPoolFactory(jedisPoolConfig, timeout);
        } else {
            jedisPoolFactory = new JedisPoolFactory.DefaultJedisPoolFactory();
        }
        CamelliaRedisProperties.RedisConf.JedisCluster jedisCluster = redisConf.getJedisCluster();
        JedisClusterFactory jedisClusterFactory;
        if (jedisCluster != null) {
            int timeout = jedisCluster.getTimeout();
            int maxAttempts = jedisCluster.getMaxAttempts();
            JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
            jedisPoolConfig.setMaxIdle(jedisCluster.getMaxIdle());
            jedisPoolConfig.setMinIdle(jedisCluster.getMinIdle());
            jedisPoolConfig.setMaxTotal(jedisCluster.getMaxActive());
            jedisPoolConfig.setMaxWaitMillis(jedisCluster.getMaxWaitMillis());
            jedisClusterFactory = new JedisClusterFactory.DefaultJedisClusterFactory(jedisPoolConfig, timeout, timeout, maxAttempts);
        } else {
            jedisClusterFactory = new JedisClusterFactory.DefaultJedisClusterFactory();
        }
        return new CamelliaRedisEnv.Builder()
                .proxyEnv(proxyEnv())
                .jedisPoolFactory(jedisPoolFactory)
                .jedisClusterFactory(jedisClusterFactory)
                .build();
    }
}
