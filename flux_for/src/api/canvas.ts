import apiClient from '../utils/api';
import type { CanvasInitialPositionDTO } from '../types';

// A generic ApiResponse type to match the backend wrapper
interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

/**
 * Get the intelligent initial position for new users.
 * Returns the optimal "hotspot" location based on recent activity and content density.
 */
export const getInitialPosition = async (): Promise<CanvasInitialPositionDTO> => {
  const response = await apiClient.get<ApiResponse<CanvasInitialPositionDTO>>('/canvas/initial-position');
  if (response.data && response.data.success) {
    return response.data.data;
  } else {
    throw new Error(response.data.message || 'Failed to get initial position');
  }
};