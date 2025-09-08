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
    public MessageNodeDTO createMessage(CreateMessageDTO dto, String ipAddress) {
        checkIpRateLimit(ipAddress);

        // Since the table has a UNIQUE constraint on (row_index, col_index),
        // we can use a "select-then-insert/update" approach.
        LambdaQueryWrapper<Message> queryWrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getRowIndex, dto.rowIndex())
                .eq(Message::getColIndex, dto.colIndex());
        Message existingMessage = messageMapper.selectOne(queryWrapper);

        Long existingId = (existingMessage != null) ? existingMessage.getId() : null;

        // Optimistic Locking Check
        if (!Objects.equals(existingId, dto.baseVersionId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The cell has been updated by another user.");
        }

        Message messageToSave;
        if (existingMessage == null) {
            // --- This is a new cell, INSERT it ---
            messageToSave = new Message();
            messageToSave.setRowIndex(dto.rowIndex());
            messageToSave.setColIndex(dto.colIndex());
        } else {
            // --- This is an existing cell, UPDATE it ---
            messageToSave = existingMessage;
        }

        // Set common fields
        messageToSave.setContent(StringEscapeUtils.escapeHtml4(dto.content()));
        messageToSave.setBgColor(dto.bgColor());
        messageToSave.setIpAddress(ipAddress);

        if (messageToSave.getId() == null) {
            messageMapper.insert(messageToSave);
        } else {
            messageMapper.updateById(messageToSave);
        }

        // Broadcast the new state to all clients
        MessageNodeDTO newCellState = new MessageNodeDTO(messageToSave.getId(), messageToSave.getRowIndex(), messageToSave.getColIndex(), messageToSave.getContent(), messageToSave.getBgColor());
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
