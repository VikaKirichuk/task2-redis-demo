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


    private final Map<String, String> airlineOrgMap = new ConcurrentHashMap<>();;

    @Value("${iagl.tenantId:default-tenant}")
    private String iaglTenantId;

    private volatile RedissonClient redisClient;
    private volatile boolean usingRedis = false;
    private RTopic topic;

    // викликається коли Redis піднявся
    public void onRedisAvailable(RedissonClient client) {
        this.redisClient = client;
        this.usingRedis = true;
        airlineOrgMap.clear();
        log.info("Redis connected. Local cache cleared.");

        topic = redisClient.getTopic("permissionManager:airlineConfig:refresh:tenantAirlineId");
        topic.addListener(String.class, (channel, tenantAirlineId) -> {
            airlineOrgMap.remove(tenantAirlineId);
            log.info("Cache invalidated for: {}", tenantAirlineId);
        });
    }

    @Override
    public String getAirlineConfiguration(String airlineId) {
        return airlineOrgMap.computeIfAbsent(iaglTenantId + airlineId, k -> {
            // спрощений stub замість реального виклику
            log.info("Fetching config for airlineId={}", airlineId);
            String hardcodedValue = "AirlineOrg{airlineId=" + airlineId + ", tenant=" + iaglTenantId + "}";
            log.info("Returning: {}", hardcodedValue);
            return hardcodedValue;
        });
    }
    public void onRedisDown() {
        this.usingRedis = false;
        this.redisClient = null;
        airlineOrgMap.clear();
        log.info("Redis went down. Switched to local cache.");
    }
}