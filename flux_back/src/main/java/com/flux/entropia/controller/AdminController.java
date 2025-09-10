package com.flux.entropia.controller;

import com.flux.entropia.dto.CanvasInitialPositionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for Redis cache management operations.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final RedisTemplate<String, String> redisStringTemplate;

    /**
     * Clear the golden spawn point cache.
     */
    @PostMapping("/cache/clear")
    public String clearCache() {
        try {
            redisStringTemplate.delete("golden_spawn_point");
            return "Cache cleared successfully";
        } catch (Exception e) {
            log.error("Failed to clear cache", e);
            return "Failed to clear cache: " + e.getMessage();
        }
    }
}