package com.example.redis_demo.config;

import lombok.extern.log4j.Log4j2;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Log4j2
public class RedissonConfig {

    @Value("${redis.masterAddress}")
    private String masterAddress;

    @Bean(destroyMethod = "")
    public RedissonClient redissonClient() {
        try {
            return createRedissonClient();
        } catch (Exception e) {
            log.warn("Redis unavailable on startup. Reason: {}", e.getMessage());
            return null;
        }
    }

    public RedissonClient createRedissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress(masterAddress)
                .setConnectTimeout(3000)
                .setTimeout(3000)
                .setRetryAttempts(1);
        return Redisson.create(config);
    }
}