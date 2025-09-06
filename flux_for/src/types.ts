// DTO for message cells, used for grid display and WebSocket updates
export interface MessageNodeDTO {
  id: number;
  rowIndex: number;
  colIndex: number;
  content: string;
}

// DTO for hovered message detail (can be kept for future use, e.g., showing history)
export interface MessageDetailDTO {
  content: string;
  createdAt: string; // Assuming string format from backend
}

// DTO for creating or updating a message cell
export interface CreateMessageDTO {
  content: string;
  rowIndex: number;
  colIndex: number;
}
