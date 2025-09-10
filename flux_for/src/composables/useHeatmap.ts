// src/composables/useHeatmap.ts
import { ref, reactive, computed } from 'vue';

// 热力图更新函数类型
export type HeatmapUpdateFunction = (rowIndex: number, colIndex: number, increment?: number) => void;
import { getHeatmapChunks } from '../api/heatmap';
import { CHUNK_SIZE } from '../config/heatmap';
import type { Viewport } from './useCanvas';
import type { HeatmapChunkDTO } from '../types';
import { useFluxStore } from '../store/fluxStore';

export interface ChunkCoordinate {
  gridX: number;
  gridY: number;
}

export function useHeatmap() {
  const store = useFluxStore();
  
  // 全局热力数据缓存
  const heatmapData = reactive<Map<string, number>>(new Map());
  const loadedChunks = reactive<Set<string>>(new Set());
  const isLoading = ref(false);
  const lastError = ref<string | null>(null);
  const lastRequestTime = ref<number>(0);
  const requestThrottle = 1000; // 1秒节流
  const lastRequestedChunks = reactive<Set<string>>(new Set()); // 记录上次请求的区块
  const heatmapUpdateCounter = ref(0); // 用于触发响应式更新

  /**
   * 根据视口计算需要加载的区块ID列表
   */
  const getRequiredChunks = (viewport: Viewport, canvasSize: { width: number; height: number }): ChunkCoordinate[] => {
    const chunks: ChunkCoordinate[] = [];
    
    // 计算视口在世界坐标中的范围
    const worldLeft = -viewport.x / viewport.zoom;
    const worldTop = -viewport.y / viewport.zoom;
    const worldRight = worldLeft + canvasSize.width / viewport.zoom;
    const worldBottom = worldTop + canvasSize.height / viewport.zoom;
    
    // 转换为单元格坐标，再转换为区块坐标
    const startCellX = Math.floor(worldLeft / store.CELL_SIZE);
    const endCellX = Math.ceil(worldRight / store.CELL_SIZE);
    const startCellY = Math.floor(worldTop / store.CELL_SIZE);
    const endCellY = Math.ceil(worldBottom / store.CELL_SIZE);
    
    // 转换为区块坐标
    const startChunkX = Math.floor(startCellX / CHUNK_SIZE);
    const endChunkX = Math.ceil(endCellX / CHUNK_SIZE);
    const startChunkY = Math.floor(startCellY / CHUNK_SIZE);
    const endChunkY = Math.ceil(endCellY / CHUNK_SIZE);
    
    // 生成区块列表
    for (let gridY = startChunkY; gridY <= endChunkY; gridY++) {
      for (let gridX = startChunkX; gridX <= endChunkX; gridX++) {
        chunks.push({ gridX, gridY });
      }
    }
    
    return chunks;
  };

  /**
   * 生成区块ID字符串
   */
  const chunkIdToString = (chunk: ChunkCoordinate): string => {
    return `${chunk.gridX},${chunk.gridY}`;
  };

  /**
   * 解析区块ID字符串
   */
  const stringToChunkId = (str: string): ChunkCoordinate => {
    const [gridX, gridY] = str.split(',').map(Number);
    return { gridX, gridY };
  };

  /**
   * 请求区块数据
   */
  const requestChunks = async (chunkIds: string[]) => {
    if (chunkIds.length === 0) return;
    
    // 过滤掉已经加载的区块
    const newChunkIds = chunkIds.filter(id => !loadedChunks.has(id));
    
    if (newChunkIds.length === 0) return;
    
    isLoading.value = true;
    lastError.value = null;
    
    try {
      const chunksParam = newChunkIds.join(';');
      const result: HeatmapChunkDTO = await getHeatmapChunks(chunksParam);
      
      // 更新热力数据缓存
      result.data.forEach(chunk => {
        const chunkId = chunkIdToString({ gridX: chunk.gridX, gridY: chunk.gridY });
        heatmapData.set(chunkId, chunk.heatValue);
        loadedChunks.add(chunkId);
      });
      
          } catch (error) {
      console.error('Failed to load heatmap chunks:', error);
      lastError.value = error instanceof Error ? error.message : 'Unknown error';
    } finally {
      isLoading.value = false;
    }
  };

  /**
   * 根据视口加载热力数据
   */
  const loadHeatmapForViewport = async (viewport: Viewport, canvasSize: { width: number; height: number }) => {
    const requiredChunks = getRequiredChunks(viewport, canvasSize);
    const chunkIds = requiredChunks.map(chunkIdToString);
    
    // 检查是否有新的区块需要加载
    const newChunks = chunkIds.filter(id => !lastRequestedChunks.has(id));
    
    // 如果没有新区块，跳过请求
    if (newChunks.length === 0) {
      return;
    }
    
    // 节流控制
    const now = Date.now();
    if (now - lastRequestTime.value < requestThrottle) {
      return;
    }
    
    lastRequestTime.value = now;
    
    // 更新已请求的区块记录
    newChunks.forEach(id => lastRequestedChunks.add(id));
    
        await requestChunks(newChunks);
  };

  /**
   * 获取指定区块的热力值
   */
  const getHeatValue = (chunk: ChunkCoordinate): number | undefined => {
    const chunkId = chunkIdToString(chunk);
    return heatmapData.get(chunkId);
  };

  /**
   * 更新指定区块的热力值
   */
  const updateChunkHeatValue = (rowIndex: number, colIndex: number, increment: number = 1) => {
    // 计算区块坐标
    const chunkX = Math.floor(colIndex / CHUNK_SIZE);
    const chunkY = Math.floor(rowIndex / CHUNK_SIZE);
    const chunkId = chunkIdToString({ gridX: chunkX, gridY: chunkY });
    
    // 更新热力值
    const currentHeatValue = heatmapData.get(chunkId) || 0;
    const newHeatValue = Math.max(0, currentHeatValue + increment);
    heatmapData.set(chunkId, newHeatValue);
    
    // 触发响应式更新
    heatmapUpdateCounter.value++;
    
      };

  /**
   * 清除缓存
   */
  const clearCache = () => {
    heatmapData.clear();
    loadedChunks.clear();
    lastRequestedChunks.clear();
    lastError.value = null;
  };

  return {
    heatmapData: computed(() => heatmapData),
    loadedChunks: computed(() => loadedChunks),
    isLoading: computed(() => isLoading.value),
    lastError: computed(() => lastError.value),
    heatmapUpdateCounter: computed(() => heatmapUpdateCounter.value),
    getRequiredChunks,
    loadHeatmapForViewport,
    requestChunks,
    getHeatValue,
    updateChunkHeatValue,
    chunkIdToString,
    stringToChunkId,
    clearCache
  };
}