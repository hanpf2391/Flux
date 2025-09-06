package com.flux.entropia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flux.entropia.config.FluxProperties;
import com.flux.entropia.dto.CreateMessageDTO;
import com.flux.entropia.dto.MessageDetailDTO;
import com.flux.entropia.dto.MessageNodeDTO;
import com.flux.entropia.entity.Message;
import com.flux.entropia.mapper.MessageMapper;
import com.flux.entropia.service.MessageService;
import com.flux.entropia.websocket.FluxWebSocketHandler;
import com.flux.entropia.websocket.WebSocketMessage;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;
    private final FluxWebSocketHandler webSocketHandler;
    private final FluxProperties fluxProperties;

    // A simple in-memory store for IP rate limiting.
    private final ConcurrentHashMap<String, Instant> ipRequestTimestamps = new ConcurrentHashMap<>();

    // Use @Lazy to break the circular dependency: Service -> Handler -> Service
    public MessageServiceImpl(MessageMapper messageMapper, @Lazy FluxWebSocketHandler webSocketHandler, FluxProperties fluxProperties) {
        this.messageMapper = messageMapper;
        this.webSocketHandler = webSocketHandler;
        this.fluxProperties = fluxProperties;
    }

    @Override
    public List<MessageNodeDTO> getMessagesInGrid(int startRow, int endRow, int startCol, int endCol) {
        LambdaQueryWrapper<Message> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
            // IMPORTANT: Only select the latest version of each cell
            .eq(Message::getIsLatest, true)
            .ge(Message::getRowIndex, startRow)
            .le(Message::getRowIndex, endRow)
            .ge(Message::getColIndex, startCol)
            .le(Message::getColIndex, endCol);

        return messageMapper.selectList(queryWrapper).stream()
            .map(msg -> new MessageNodeDTO(msg.getId(), msg.getRowIndex(), msg.getColIndex(), msg.getContent()))
            .collect(Collectors.toList());
    }

    @Override
    public Optional<MessageDetailDTO> getMessageDetailById(Long id) {
        // This method might need to be adapted depending on how you want to show history.
        // For now, it just gets a specific version by its unique ID.
        Message message = messageMapper.selectById(id);
        return Optional.ofNullable(message)
            .map(msg -> new MessageDetailDTO(msg.getContent(), msg.getCreatedAt()));
    }

    @Override
    @Transactional
    public MessageNodeDTO createMessage(CreateMessageDTO dto, String ipAddress) {
        checkIpRateLimit(ipAddress);

        // --- Optimistic Locking Check ---
        // Step 1: Get the current latest version from the database.
        Message currentLatest = messageMapper.selectLatestForCell(dto.rowIndex(), dto.colIndex());
        Long currentLatestId = (currentLatest != null) ? currentLatest.getId() : null;

        // Step 2: Compare with the base version from the client.
        // If they don't match, it means someone else has updated the cell in the meantime.
        if (!java.util.Objects.equals(currentLatestId, dto.baseVersionId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The cell has been updated by another user.");
        }

        // --- Proceed with Save ---
        // Step 3: Set the is_latest flag to 0 for all existing versions of this cell.
        // This is safe even if currentLatest was null.
        messageMapper.unsetLatestFlagForCell(dto.rowIndex(), dto.colIndex());

        // Step 4: Insert the new version with the is_latest flag set to 1.
        Message newMessage = new Message();
        newMessage.setContent(StringEscapeUtils.escapeHtml4(dto.content()));
        newMessage.setRowIndex(dto.rowIndex());
        newMessage.setColIndex(dto.colIndex());
        newMessage.setIpAddress(ipAddress);
        newMessage.setIsLatest(true); // Mark this new entry as the latest version

        messageMapper.insert(newMessage);

        // Step 5: Broadcast the new latest cell state to all clients.
        MessageNodeDTO newCellState = new MessageNodeDTO(newMessage.getId(), newMessage.getRowIndex(), newMessage.getColIndex(), newMessage.getContent());
        webSocketHandler.broadcast(new WebSocketMessage<>("CELL_UPDATED", newCellState));

        return newCellState;
    }

    /**
     * Checks if a given IP address is allowed to post.
     * Throws an exception if the rate limit is exceeded.
     * @param ipAddress The IP address to check.
     */
    private void checkIpRateLimit(String ipAddress) {
        Instant now = Instant.now();
        Instant lastRequest = ipRequestTimestamps.get(ipAddress);
        Duration rateLimitDuration = Duration.ofMillis(fluxProperties.getRateLimit().getDurationMs());

        if (lastRequest != null && Duration.between(lastRequest, now).compareTo(rateLimitDuration) < 0) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "You are posting too frequently. Please wait a moment.");
        }
        ipRequestTimestamps.put(ipAddress, now);
    }
}