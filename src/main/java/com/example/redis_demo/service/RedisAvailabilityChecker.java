package com.example.redis_demo.service;

import com.example.redis_demo.config.RedissonConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Log4j2
@RequiredArgsConstructor
public class RedisAvailabilityChecker {

    private final RedissonConfig redissonConfig;
    private final AirlineConfigServiceImpl airlineConfigService;
    private volatile RedissonClient redisClient;
    private final AtomicBoolean redisAvailable = new AtomicBoolean(false);

    @Scheduled(fixedDelay = 10000)
    public void checkRedisAvailability() {
        log.info("=== Scheduler tick. Redis available: {} ===", redisAvailable.get());

        if (redisAvailable.get()) {
            // Redis був підключений — перевіряємо чи він ще живий
            try {
                redisClient.getBucket("health-check").get();
                log.info("Redis is still alive.");
            } catch (Exception e) {
                log.warn("Redis went down! Switching to local cache...");
                redisAvailable.set(false);
                redisClient = null;
                airlineConfigService.onRedisDown();
            }
            return;
        }

        // Redis не підключений — намагаємось підключитись
        try {
            RedissonClient client = redissonConfig.createRedissonClient();
            client.getBucket("health-check").get();
            log.info("Redis is now available! Switching...");
            RedissonClient oldClient = this.redisClient;
            this.redisClient = client;
            redisAvailable.set(true);
            airlineConfigService.onRedisAvailable(client);
            if (oldClient != null) {
                try { oldClient.shutdown(); } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            log.warn("Redis still unavailable: {}", e.getMessage());
        }
    }
}