import type { CanvasInitialPositionDTO } from '../types';

/**
 * Get the intelligent initial position for new users.
 * Returns the optimal "hotspot" location based on recent activity and content density.
 */
export const getInitialPosition = async (): Promise<CanvasInitialPositionDTO> => {
  const response = await fetch('/api/canvas/initial-position');
  if (!response.ok) {
    throw new Error('Failed to get initial position');
  }
  const data = await response.json();
  return data.data;
};