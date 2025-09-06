package com.flux.entropia.controller;

import com.flux.entropia.common.ApiResponse;
import com.flux.entropia.dto.CreateMessageDTO;
import com.flux.entropia.dto.MessageDetailDTO;
import com.flux.entropia.dto.MessageNodeDTO;
import com.flux.entropia.service.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * REST controller for handling message-related API requests.
 * (Based on Task B-3)
 */
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /**
     * GET /api/messages : Get all message cells within a specific grid area.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<MessageNodeDTO>>> getMessagesInGrid(
        @RequestParam int startRow,
        @RequestParam int endRow,
        @RequestParam int startCol,
        @RequestParam int endCol
    ) {
        List<MessageNodeDTO> nodes = messageService.getMessagesInGrid(startRow, endRow, startCol, endCol);
        return ResponseEntity.ok(ApiResponse.success(nodes));
    }

    /**
     * GET /api/messages/{id} : Get the detailed information for a single node.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MessageDetailDTO>> getMessageDetail(@PathVariable Long id) {
        MessageDetailDTO detail = messageService.getMessageDetailById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message node not found with id: " + id));
        return ResponseEntity.ok(ApiResponse.success(detail));
    }

    /**
     * POST /api/messages : Create a new message node.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<MessageNodeDTO>> createMessage(
        @Valid @RequestBody CreateMessageDTO createMessageDTO,
        HttpServletRequest request
    ) {
        // It's crucial to get the real client IP, considering proxies.
        String ipAddress = getClientIpAddress(request);
        MessageNodeDTO createdNode = messageService.createMessage(createMessageDTO, ipAddress);
        return new ResponseEntity<>(ApiResponse.success(createdNode), HttpStatus.CREATED);
    }

    /**
     * Utility method to extract the client's IP address from the request,
     * considering common proxy headers like X-Forwarded-For.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || "unknown".equalsIgnoreCase(xfHeader)) {
            return request.getRemoteAddr();
        }
        // The X-Forwarded-For header can contain a comma-separated list of IPs.
        // The first one is the original client.
        return xfHeader.split(",")[0];
    }
}