// DTO for message cells, used for grid display and WebSocket updates
export interface MessageNodeDTO {
  id: number;
  rowIndex: number;
  colIndex: number;
  content: string | null;
  bgColor: string | null;
}

// DTO for hovered message detail (can be kept for future use, e.g., showing history)
export interface MessageDetailDTO {
  content: string;
  createdAt: string; // Assuming string format from backend
}

// DTO for creating or updating a message cell
export interface CreateMessageDTO {
  rowIndex: number;
  colIndex: number;
  content?: string;
  bgColor?: string | null;
  baseVersionId?: number | null; // Keep for optimistic locking on text
}

// DTO for application statistics
export interface StatsDTO {
  totalMessages: number;
  onlineUsers: number;
  visibleMessages: number;
}

// DTO for canvas initial position response
export interface CanvasInitialPositionDTO {
  rowIndex: number;
  colIndex: number;
  isDefault: boolean;
  message: string;
}

// DTO for heatmap chunk data response
export interface HeatmapChunkDTO {
  chunkSize: number;
  data: HeatmapDataDTO[];
}

export interface HeatmapDataDTO {
  gridY: number;
  gridX: number;
  heatValue: number;
}
