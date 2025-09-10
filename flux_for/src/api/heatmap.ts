// src/api/heatmap.ts
import axios from 'axios';
import type { HeatmapChunkDTO } from '../types';

// A generic ApiResponse type to match the backend wrapper
interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

export async function getHeatmapChunks(chunks: string): Promise<HeatmapChunkDTO> {
  try {
    const response = await axios.get<ApiResponse<HeatmapChunkDTO>>('/api/heatmap/chunks', {
      params: { chunks }
    });
    return response.data.data;
  } catch (error) {
    console.error('Failed to fetch heatmap chunks:', error);
    throw error;
  }
}