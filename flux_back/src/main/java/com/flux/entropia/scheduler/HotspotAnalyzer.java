package com.flux.entropia.scheduler;

import com.flux.entropia.dto.CanvasInitialPositionDTO;
import com.flux.entropia.service.CanvasService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task for analyzing hotspot activity and updating the golden spawn point.
 * Runs every 5 minutes to pre-calculate the optimal initial position for new users.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HotspotAnalyzer {

    private final CanvasService canvasService;

    // Configuration parameters
    @Value("${canvas.hotspot.grid-size:200}")
    private int gridSize;
    
    @Value("${canvas.hotspot.time-window-days:7}")
    private int timeWindowDays;

    /**
     * Scheduled task that runs every 5 minutes to update the golden spawn point.
     * This implements the "offline pre-calculation" part of the architecture.
     */
    @Scheduled(fixedRate = 5 * 60 * 1000) // 5 minutes in milliseconds
    public void updateGoldenSpawnPoint() {
        try {
            log.info("Starting hotspot analysis to update golden spawn point");
            
            // Calculate the current hotspot position
            CanvasInitialPositionDTO hotspotPosition = canvasService.getInitialPosition();
            
            // Update the golden spawn point in the cache
            canvasService.updateGoldenSpawnPoint(hotspotPosition);
            
            log.info("Successfully updated golden spawn point: {}", hotspotPosition);
            
        } catch (Exception e) {
            log.error("Failed to update golden spawn point", e);
        }
    }
}