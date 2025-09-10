package com.flux.entropia.controller;

import com.flux.entropia.common.ApiResponse;
import com.flux.entropia.dto.CanvasInitialPositionDTO;
import com.flux.entropia.service.CanvasService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for handling canvas-related API requests.
 * Includes intelligent initial positioning functionality.
 */
@RestController
@RequestMapping("/api/canvas")
@RequiredArgsConstructor
public class CanvasController {

    private final CanvasService canvasService;

    /**
     * GET /api/canvas/initial-position : Get the intelligent initial position for new users.
     * Returns the optimal "hotspot" location based on recent activity and content density.
     */
    @GetMapping("/initial-position")
    public ResponseEntity<ApiResponse<CanvasInitialPositionDTO>> getInitialPosition() {
        CanvasInitialPositionDTO position = canvasService.getInitialPosition();
        return ResponseEntity.ok(ApiResponse.success(position));
    }
}