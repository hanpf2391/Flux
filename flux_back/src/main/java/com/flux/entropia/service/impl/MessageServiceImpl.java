package com.flux.entropia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import java.util.Objects;
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
            .ge(Message::getRowIndex, startRow)
            .le(Message::getRowIndex, endRow)
            .ge(Message::getColIndex, startCol)
            .le(Message::getColIndex, endCol);

        return messageMapper.selectList(queryWrapper).stream()
            .map(msg -> new MessageNodeDTO(msg.getId(), msg.getRowIndex(), msg.getColIndex(), msg.getContent(), msg.getBgColor()))
            .collect(Collectors.toList());
    }

    @Override
    public Optional<MessageDetailDTO> getMessageDetailById(Long id) {
        Message message = messageMapper.selectById(id);
        return Optional.ofNullable(message)
            .map(msg -> new MessageDetailDTO(msg.getContent(), msg.getCreatedAt()));
    }

    @Override
    @Transactional
    public MessageNodeDTO createOrUpdateMessage(CreateMessageDTO dto, String ipAddress) {
        checkIpRateLimit(ipAddress);

        Message existingMessage = messageMapper.selectLatestForCell(dto.rowIndex(), dto.colIndex());

        Message messageToSave;

        // Case 1: This is an UPDATE request for an existing cell
        if (dto.baseVersionId() != null) {
            if (existingMessage == null) {
                // The cell the user was editing was deleted by someone else.
                throw new ResponseStatusException(HttpStatus.CONFLICT, "The cell you are trying to edit no longer exists.");
            }
            if (!Objects.equals(existingMessage.getId(), dto.baseVersionId())) {
                // The cell was updated by someone else. Optimistic lock fails.
                throw new ResponseStatusException(HttpStatus.CONFLICT, "The cell has been updated by another user.");
            }
            // It's a valid update.
            messageToSave = existingMessage;
        }
        // Case 2: This is a CREATE request for a new cell
        else {
            if (existingMessage != null) {
                // Someone else created a cell here while the user was typing.
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Another user has just created a cell at this position.");
            }
            // It's a valid creation.
            messageToSave = new Message();
            messageToSave.setRowIndex(dto.rowIndex());
            messageToSave.setColIndex(dto.colIndex());
        }

        // Apply changes and save
        // Sanitize content to prevent XSS
        String sanitizedContent = (dto.content() != null) ? StringEscapeUtils.escapeHtml4(dto.content()) : "";
        messageToSave.setContent(sanitizedContent);
        messageToSave.setBgColor(dto.bgColor());
        messageToSave.setIpAddress(ipAddress);

        // Handle empty content creation (should be treated as deletion)
        if (messageToSave.getId() == null && sanitizedContent.isEmpty() && dto.bgColor() == null) {
            // Don't create empty cells
            return null;
        }

        if (messageToSave.getId() == null) {
            // This is a new cell creation
            messageMapper.insert(messageToSave);
            
            // Broadcast the new state
            MessageNodeDTO newCellState = new MessageNodeDTO(messageToSave.getId(), messageToSave.getRowIndex(), messageToSave.getColIndex(), messageToSave.getContent(), messageToSave.getBgColor());
            webSocketHandler.broadcast(new WebSocketMessage<>("CELL_UPDATED", newCellState));
            
            // Broadcast updated statistics
            webSocketHandler.broadcastSystemStats();
            
            return newCellState;
        } else {
            // Handle case where content is cleared but color might be set
            if (sanitizedContent.isEmpty() && dto.bgColor() == null) {
                // If both content and color are cleared, delete the cell
                messageMapper.deleteById(messageToSave.getId());
                
                // Broadcast deletion
                webSocketHandler.broadcast(new WebSocketMessage<>("CELL_DELETED", new MessageNodeDTO(messageToSave.getId(), dto.rowIndex(), dto.colIndex(), null, null)));
                
                // Broadcast updated statistics
                webSocketHandler.broadcastSystemStats();
                
                return new MessageNodeDTO(messageToSave.getId(), dto.rowIndex(), dto.colIndex(), null, null);
            } else {
                // For updates, create a new record to preserve history
                Message newMessage = new Message();
                newMessage.setRowIndex(dto.rowIndex());
                newMessage.setColIndex(dto.colIndex());
                newMessage.setContent(sanitizedContent);
                // Preserve existing background color if new one is not provided
                newMessage.setBgColor(dto.bgColor() != null ? dto.bgColor() : existingMessage.getBgColor());
                newMessage.setIpAddress(ipAddress);
                
                messageMapper.insert(newMessage);
                
                // Broadcast the new state
                MessageNodeDTO newCellState = new MessageNodeDTO(newMessage.getId(), newMessage.getRowIndex(), newMessage.getColIndex(), newMessage.getContent(), newMessage.getBgColor());
                webSocketHandler.broadcast(new WebSocketMessage<>("CELL_UPDATED", newCellState));
                
                // Broadcast updated statistics
                webSocketHandler.broadcastSystemStats();
                
                return newCellState;
            }
        }
    }

    @Override
    public long getTotalMessageCount() {
        return messageMapper.countDistinctCoordinates();
    }

    @Override
    public long countDistinctCoordinatesInGrid(int startRow, int endRow, int startCol, int endCol) {
        return messageMapper.countDistinctCoordinatesInGrid(startRow, endRow, startCol, endCol);
    }

    @Override
    public long getVisibleMessageCount() {
        return messageMapper.countDistinctCoordinates();
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
