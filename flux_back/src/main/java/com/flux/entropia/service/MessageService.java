package com.flux.entropia.service;

import com.flux.entropia.dto.CreateMessageDTO;
import com.flux.entropia.dto.MessageDetailDTO;
import com.flux.entropia.dto.MessageNodeDTO;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for message-related business logic.
 * (Adapted for grid system)
 */
public interface MessageService {

    /**
     * Retrieves all message cells within a given rectangular grid area.
     *
     * @param startRow The starting row index of the grid area.
     * @param endRow   The ending row index of the grid area.
     * @param startCol The starting column index of the grid area.
     * @param endCol   The ending column index of the grid area.
     * @return A list of {@link MessageNodeDTO} objects within the area.
     */
    List<MessageNodeDTO> getMessagesInGrid(int startRow, int endRow, int startCol, int endCol);

    /**
     * Retrieves the detailed information for a single message cell.
     *
     * @param id The ID of the message.
     * @return An {@link Optional} containing the {@link MessageDetailDTO} if found, otherwise empty.
     */
    Optional<MessageDetailDTO> getMessageDetailById(Long id);

    /**
     * Creates a new message cell, performs validation, and persists it.
     *
     * @param dto       The DTO containing the new message data.
     * @param ipAddress The IP address of the creator.
     * @return The DTO of the newly created cell, to be broadcasted.
     */
    MessageNodeDTO createOrUpdateMessage(CreateMessageDTO dto, String ipAddress);

    /**
     * Gets the total number of messages in the database.
     *
     * @return The total message count.
     */
    long getTotalMessageCount();

    long countDistinctCoordinatesInGrid(int startRow, int endRow, int startCol, int endCol);

    /**
     * Gets the total number of messages in the database.
     *
     * @return The total message count.
     */
    long getVisibleMessageCount();

}
