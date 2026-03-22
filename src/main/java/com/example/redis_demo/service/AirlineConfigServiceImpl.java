package com.example.redis_demo.service;

import lombok.extern.log4j.Log4j2;
import org.redisson.api.RedissonClient;
import org.redisson.api.RTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
@Service
@Log4j2
public class AirlineConfigServiceImpl implements AirlineConfigService {

    private final Map<String, String> airlineConfigCache = new ConcurrentHashMap<>();

    @Value("${iagl.tenantId:default-tenant}")
    private String tenantId;

    private RTopic airlineConfigRefreshTopic;

    public void onRedisAvailable(RedissonClient client) {
        airlineConfigCache.clear();
        log.info("Redis connected. Local cache cleared.");

        airlineConfigRefreshTopic = client.getTopic("permissionManager:airlineConfig:refresh:tenantAirlineId");
        airlineConfigRefreshTopic.addListener(String.class, (channel, tenantAirlineId) -> {
            airlineConfigCache.remove(tenantId + tenantAirlineId);
            log.info("Cache invalidated for: {}", tenantAirlineId);
        });
    }

    public void onRedisDown() {
        if (airlineConfigRefreshTopic != null) {
            airlineConfigRefreshTopic.removeAllListeners();
            airlineConfigRefreshTopic = null;
        }
        airlineConfigCache.clear();
        log.warn("Redis went down. Switched to local cache.");
    }

    @Override
    public String getAirlineConfiguration(String airlineId) {
        return airlineConfigCache.computeIfAbsent(tenantId + airlineId, k -> {
            log.info("Fetching config for airlineId={}", airlineId);
            String airlineConfig = "AirlineOrg{airlineId=" + airlineId + ", tenant=" + tenantId + "}";
            log.info("Returning: {}", airlineConfig);
            return airlineConfig;
        });
    }
}