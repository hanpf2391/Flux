import axios from 'axios';
import type { MessageNodeDTO, MessageDetailDTO, CreateMessageDTO, StatsDTO } from '../types';

// A generic ApiResponse type to match the backend wrapper
interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

// Defines the parameters for the getMessagesInGrid function
export interface GridParams {
  startRow: number;
  endRow: number;
  startCol: number;
  endCol: number;
}

/**
 * Fetches message cells within a specific grid area.
 * @param params The grid coordinates.
 * @returns A promise that resolves to an array of MessageNodeDTO objects.
 */
export const getMessagesInGrid = async (params: GridParams): Promise<MessageNodeDTO[]> => {
  try {
    const response = await axios.get<ApiResponse<MessageNodeDTO[]>>('/api/messages', { params });
    if (response.data && response.data.success) {
      return response.data.data;
    } else {
      throw new Error(response.data.message || 'Failed to fetch messages');
    }
  } catch (error) {
    console.error('Error fetching messages:', error);
    throw error;
  }
};

/**
 * Fetches the detailed information for a single message.
 * @param id The ID of the message.
 * @returns A promise that resolves to a MessageDetailDTO object.
 */
export const getMessageDetail = async (id: number): Promise<MessageDetailDTO> => {
  const response = await axios.get<ApiResponse<MessageDetailDTO>>(`/api/messages/${id}`);
  if (response.data && response.data.success) {
    return response.data.data;
  } else {
    throw new Error(response.data.message || `Failed to fetch message detail for id ${id}`);
  }
};

/**
 * Creates or updates a message cell.
 * @param messageData The data for the new message.
 * @returns A promise that resolves to the created message cell data.
 */
export const createMessage = async (messageData: CreateMessageDTO): Promise<MessageNodeDTO> => {
    const response = await axios.post<ApiResponse<MessageNodeDTO>>('/api/messages', messageData);
    if (response.data && response.data.success) {
        return response.data.data;
    }
    throw new Error(response.data.message || 'Failed to create message');
};

/**
 * Fetches application statistics.
 * @returns A promise that resolves to a StatsDTO object.
 */
export const getStats = async (): Promise<StatsDTO> => {
    const response = await axios.get<ApiResponse<StatsDTO>>('/api/stats');
    if (response.data && response.data.success) {
        return response.data.data;
    }
    throw new Error(response.data.message || 'Failed to fetch stats');
};

/**
 * Fetches viewport statistics.
 * @returns A promise that resolves to a number object.
 */
export const getViewportStats = async (params: GridParams): Promise<number> => {
    const response = await axios.get<ApiResponse<number>>('/api/stats/viewport', { params });
    if (response.data && response.data.success) {
        return response.data.data;
    }
    throw new Error(response.data.message || 'Failed to fetch viewport stats');
};