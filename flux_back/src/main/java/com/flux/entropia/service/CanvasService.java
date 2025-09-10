package com.flux.entropia.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flux.entropia.dto.CanvasInitialPositionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;

/**
 * Service for handling canvas-related operations including intelligent initial positioning.
 * Implements the "offline pre-calculation + online high-speed query" architecture.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CanvasService {

    private final MessageService messageService;
    private final RedisTemplate<String, String> redisStringTemplate;
    private final ObjectMapper objectMapper;
    
    // Redis key for storing the golden spawn point
    private static final String GOLDEN_SPAWN_POINT_KEY = "golden_spawn_point";
    
    // Cache expiration time (5 minutes)
    private static final Duration CACHE_EXPIRATION = Duration.ofMinutes(5);
    
    // Configuration parameters (should be moved to application.yml)
    @Value("${canvas.hotspot.grid-size:200}")
    private int globalGridSize;
    
    @Value("${canvas.hotspot.time-window-days:7}")
    private int timeWindowDays;

    /**
     * Get the initial position for new users.
     * Implements cache-aside pattern with fallback logic.
     */
    @Transactional(readOnly = true)
    public CanvasInitialPositionDTO getInitialPosition() {
        try {
            // Try to get from cache first
            Optional<CanvasInitialPositionDTO> cachedPosition = getFromCache();
            if (cachedPosition.isPresent()) {
                log.debug("Retrieved initial position from cache: {}", cachedPosition.get());
                return cachedPosition.get();
            }
            
            // Fallback: calculate in real-time
            log.info("Cache miss, calculating initial position in real-time");
            CanvasInitialPositionDTO calculatedPosition = calculateHotspotPosition();
            
            // Cache the result for future requests
            cachePosition(calculatedPosition);
            
            return calculatedPosition;
            
        } catch (Exception e) {
            log.error("Failed to get initial position, returning default", e);
            return CanvasInitialPositionDTO.defaultPosition();
        }
    }

    /**
     * Get the golden spawn point from Redis cache.
     */
    private Optional<CanvasInitialPositionDTO> getFromCache() {
        try {
            String cachedJson = redisStringTemplate.opsForValue().get(GOLDEN_SPAWN_POINT_KEY);
            
            if (cachedJson != null) {
                CanvasInitialPositionDTO position = objectMapper.readValue(cachedJson, CanvasInitialPositionDTO.class);
                return Optional.of(position);
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse golden spawn point JSON from cache", e);
        } catch (Exception e) {
            log.warn("Failed to retrieve golden spawn point from cache", e);
        }
        
        return Optional.empty();
    }

    /**
     * Cache the calculated position.
     */
    private void cachePosition(CanvasInitialPositionDTO position) {
        try {
            String json = objectMapper.writeValueAsString(position);
            redisStringTemplate.opsForValue().set(GOLDEN_SPAWN_POINT_KEY, json, CACHE_EXPIRATION);
            log.debug("Cached initial position: {}", position);
            
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize golden spawn point to JSON", e);
        } catch (Exception e) {
            log.warn("Failed to cache golden spawn point", e);
        }
    }

    /**
     * Calculate the hotspot position using optimized SQL query.
     * This is the fallback logic when cache is not available.
     */
    private CanvasInitialPositionDTO calculateHotspotPosition() {
        try {
            return messageService.calculateHotspotPosition(globalGridSize, timeWindowDays);
        } catch (Exception e) {
            log.error("Failed to calculate hotspot position", e);
            return CanvasInitialPositionDTO.defaultPosition();
        }
    }

    /**
     * Update the golden spawn point.
     * This method is called by the scheduled hotspot analyzer.
     */
    public void updateGoldenSpawnPoint(CanvasInitialPositionDTO position) {
        try {
            cachePosition(position);
            log.info("Updated golden spawn point: {}", position);
        } catch (Exception e) {
            log.error("Failed to update golden spawn point", e);
        }
    }

    }