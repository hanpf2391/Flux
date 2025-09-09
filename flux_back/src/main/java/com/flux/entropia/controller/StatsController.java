package com.flux.entropia.controller;

import com.flux.entropia.common.ApiResponse;
import com.flux.entropia.dto.StatsDTO;
import com.flux.entropia.service.MessageService;
import com.flux.entropia.websocket.FluxWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final MessageService messageService;
    private final FluxWebSocketHandler webSocketHandler;

    @GetMapping
    public ResponseEntity<ApiResponse<StatsDTO>> getStats() {
        long totalMessages = messageService.getTotalMessageCount();
        int onlineUsers = webSocketHandler.getOnlineUserCount();
        long visibleMessages = messageService.getVisibleMessageCount();
        StatsDTO stats = new StatsDTO(totalMessages, onlineUsers, visibleMessages);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/viewport")
    public ResponseEntity<ApiResponse<Long>> getViewportStats(
            @RequestParam int startRow,
            @RequestParam int endRow,
            @RequestParam int startCol,
            @RequestParam int endCol) {
        long count = messageService.countDistinctCoordinatesInGrid(startRow, endRow, startCol, endCol);
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
