<!-- src/components/HeatmapMinimap.vue -->
<template>
  <div class="minimap-container">
    <div class="minimap-header">
      <span class="minimap-title">热力图</span>
    </div>
    
    <div class="minimap-content">
      <canvas
        ref="canvasRef"
        class="minimap-canvas"
        :width="MINIMAP_SIZE"
        :height="MINIMAP_SIZE"
        @click="handleCanvasClick"
      ></canvas>
      
      <!-- 视口指示器 -->
      <div 
        v-if="viewportIndicator"
        class="viewport-indicator"
        :style="viewportIndicatorStyle"
      ></div>
      
    </div>
    
    <!-- 热力图例 -->
    <div class="heatmap-legend">
      <div 
        v-for="(tier, key) in HEAT_TIERS" 
        :key="key"
        class="legend-item"
      >
        <div 
          class="legend-color" 
          :style="{ backgroundColor: tier.color }"
        ></div>
        <span class="legend-label">{{ tier.label }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue';
import { useFluxStore, CELL_SIZE } from '../store/fluxStore';
import type { Viewport } from '../composables/useCanvas';
import { useHeatmap, type ChunkCoordinate } from '../composables/useHeatmap';
import { 
  MINIMAP_SIZE, 
  MINIMAP_CHUNKS_VISIBLE, 
  CHUNK_SIZE as HEATMAP_CHUNK_SIZE, 
  getHeatColor 
} from '../config/heatmap';

// Props
interface Props {
  viewport: Viewport;
  canvasRef?: HTMLElement | null;
}

const props = withDefaults(defineProps<Props>(), {
  canvasRef: null
});


// Store and composables
const store = useFluxStore();
const mainCanvasRef = ref<HTMLElement | null>(null);
const heatmap = store.heatmapInstance;


// Refs
const canvasRef = ref<HTMLCanvasElement | null>(null);
const ctx = ref<CanvasRenderingContext2D | null>(null);
const animationFrameId = ref<number | null>(null);
const lastHeatmapUpdate = ref<number>(0);

// Computed
const viewportIndicator = computed(() => {
  if (!mainCanvasRef.value) return null;
  
  const canvas = mainCanvasRef.value;
  const centerX = (-props.viewport.x + canvas.clientWidth / 2) / props.viewport.zoom / CELL_SIZE;
  const centerY = (-props.viewport.y + canvas.clientHeight / 2) / props.viewport.zoom / CELL_SIZE;
  
  return { centerX, centerY };
});

const viewportIndicatorStyle = computed(() => {
  if (!viewportIndicator.value) return {};
  
  // 计算视口在小地图上的位置和大小
  const worldX = viewportIndicator.value.centerX * CELL_SIZE;
  const worldY = viewportIndicator.value.centerY * CELL_SIZE;
  
  // 转换为小地图坐标
  const minimapX = (worldX / (MINIMAP_CHUNKS_VISIBLE * HEATMAP_CHUNK_SIZE * CELL_SIZE)) * MINIMAP_SIZE;
  const minimapY = (worldY / (MINIMAP_CHUNKS_VISIBLE * HEATMAP_CHUNK_SIZE * CELL_SIZE)) * MINIMAP_SIZE;
  
  // 视口大小在小地图上的显示
  const viewWidthInCells = mainCanvasRef.value!.clientWidth / viewport.zoom / CELL_SIZE;
  const viewHeightInCells = mainCanvasRef.value!.clientHeight / viewport.zoom / CELL_SIZE;
  const indicatorWidth = (viewWidthInCells / (MINIMAP_CHUNKS_VISIBLE * HEATMAP_CHUNK_SIZE)) * MINIMAP_SIZE;
  const indicatorHeight = (viewHeightInCells / (MINIMAP_CHUNKS_VISIBLE * HEATMAP_CHUNK_SIZE)) * MINIMAP_SIZE;
  
  return {
    left: `${Math.max(0, Math.min(MINIMAP_SIZE - indicatorWidth, minimapX - indicatorWidth / 2))}px`,
    top: `${Math.max(0, Math.min(MINIMAP_SIZE - indicatorHeight, minimapY - indicatorHeight / 2))}px`,
    width: `${Math.max(4, Math.min(MINIMAP_SIZE, indicatorWidth))}px`,
    height: `${Math.max(4, Math.min(MINIMAP_SIZE, indicatorHeight))}px`
  };
});

// Methods - 动画循环版本
const drawMinimap = () => {
  if (!ctx.value || !mainCanvasRef.value) {
    return;
  }
  
    
  const canvas = ctx.value.canvas;
  const centerX = (-props.viewport.x + mainCanvasRef.value.clientWidth / 2) / props.viewport.zoom / CELL_SIZE;
  const centerY = (-props.viewport.y + mainCanvasRef.value.clientHeight / 2) / props.viewport.zoom / CELL_SIZE;
  
  // 清空画布
  ctx.value.clearRect(0, 0, MINIMAP_SIZE, MINIMAP_SIZE);
  
  // 先绘制一个测试背景，确保画布可见
  ctx.value.fillStyle = '#f0f0f0';
  ctx.value.fillRect(0, 0, MINIMAP_SIZE, MINIMAP_SIZE);
  
  // 绘制背景网格
  ctx.value.strokeStyle = '#e0e0e0';
  ctx.value.lineWidth = 1;
  const cellSize = MINIMAP_SIZE / MINIMAP_CHUNKS_VISIBLE;
  
  for (let i = 0; i <= MINIMAP_CHUNKS_VISIBLE; i++) {
    ctx.value.beginPath();
    ctx.value.moveTo(i * cellSize, 0);
    ctx.value.lineTo(i * cellSize, MINIMAP_SIZE);
    ctx.value.stroke();
    
    ctx.value.beginPath();
    ctx.value.moveTo(0, i * cellSize);
    ctx.value.lineTo(MINIMAP_SIZE, i * cellSize);
    ctx.value.stroke();
  }
  
  // 计算中心区块
  const centerChunkX = Math.floor(centerX / HEATMAP_CHUNK_SIZE);
  const centerChunkY = Math.floor(centerY / HEATMAP_CHUNK_SIZE);
  
  // 绘制热力数据
  const startChunkX = centerChunkX - Math.floor(MINIMAP_CHUNKS_VISIBLE / 2);
  const startChunkY = centerChunkY - Math.floor(MINIMAP_CHUNKS_VISIBLE / 2);
  
  let heatChunksDrawn = 0;
  for (let dy = 0; dy < MINIMAP_CHUNKS_VISIBLE; dy++) {
    for (let dx = 0; dx < MINIMAP_CHUNKS_VISIBLE; dx++) {
      const chunkX = startChunkX + dx;
      const chunkY = startChunkY + dy;
      const heatValue = heatmap.getHeatValue({ gridX: chunkX, gridY: chunkY });
      
      if (heatValue !== undefined && heatValue > 0) {
        const color = getHeatColor(heatValue);
        ctx.value.fillStyle = color;
        ctx.value.fillRect(dx * cellSize, dy * cellSize, cellSize, cellSize);
        heatChunksDrawn++;
      }
    }
  }
  
    
  // 请求新的热力数据（只有在进入新区块时才会发送请求）
  heatmap.loadHeatmapForViewport(props.viewport, {
    width: mainCanvasRef.value.clientWidth,
    height: mainCanvasRef.value.clientHeight
  });
  
  // 继续动画循环
  const newFrameId = requestAnimationFrame(drawMinimap);
  animationFrameId.value = newFrameId;
  };

const handleCanvasClick = (event: MouseEvent) => {
  if (!canvasRef.value || !mainCanvasRef.value) return;
  
  const rect = canvasRef.value.getBoundingClientRect();
  const x = event.clientX - rect.left;
  const y = event.clientY - rect.top;
  
  // 计算点击位置对应的世界坐标
  const centerX = (-props.viewport.x + mainCanvasRef.value.clientWidth / 2) / props.viewport.zoom / CELL_SIZE;
  const centerY = (-props.viewport.y + mainCanvasRef.value.clientHeight / 2) / props.viewport.zoom / CELL_SIZE;
  
  const centerChunkX = Math.floor(centerX / HEATMAP_CHUNK_SIZE);
  const centerChunkY = Math.floor(centerY / HEATMAP_CHUNK_SIZE);
  
  const startChunkX = centerChunkX - Math.floor(MINIMAP_CHUNKS_VISIBLE / 2);
  const startChunkY = centerChunkY - Math.floor(MINIMAP_CHUNKS_VISIBLE / 2);
  
  const clickedChunkX = startChunkX + Math.floor(x / (MINIMAP_SIZE / MINIMAP_CHUNKS_VISIBLE));
  const clickedChunkY = startChunkY + Math.floor(y / (MINIMAP_SIZE / MINIMAP_CHUNKS_VISIBLE));
  
  const targetWorldX = clickedChunkX * HEATMAP_CHUNK_SIZE * CELL_SIZE + (HEATMAP_CHUNK_SIZE * CELL_SIZE) / 2;
  const targetWorldY = clickedChunkY * HEATMAP_CHUNK_SIZE * CELL_SIZE + (HEATMAP_CHUNK_SIZE * CELL_SIZE) / 2;
  
  // 更新视口位置
  props.viewport.x = mainCanvasRef.value.clientWidth / 2 - targetWorldX * props.viewport.zoom;
  props.viewport.y = mainCanvasRef.value.clientHeight / 2 - targetWorldY * props.viewport.zoom;
  
  };

// Watchers
watch(() => props.canvasRef, (newRef) => {
  if (newRef) {
    mainCanvasRef.value = newRef;
      
    // 如果已经有了 context，重新开始绘制
    if (ctx.value && !animationFrameId.value) {
      drawMinimap();
    }
  }
}, { immediate: true });


// 监听热力数据变化并触发重绘
watch([() => heatmap.heatmapUpdateCounter?.value], () => {
  if (ctx.value) {
    // 取消现有的动画帧，避免重复绘制
    if (animationFrameId.value) {
      cancelAnimationFrame(animationFrameId.value);
      animationFrameId.value = null;
    }
    // 只重绘一次，不重新启动动画循环
    drawMinimapOnce();
  }
}, { deep: true });

// 单次重绘函数，不启动动画循环
const drawMinimapOnce = () => {
  if (!ctx.value || !mainCanvasRef.value) {
        return;
  }
  
    const canvas = ctx.value.canvas;
  const centerX = (-props.viewport.x + mainCanvasRef.value.clientWidth / 2) / props.viewport.zoom / CELL_SIZE;
  const centerY = (-props.viewport.y + mainCanvasRef.value.clientHeight / 2) / props.viewport.zoom / CELL_SIZE;
  
  // 清空画布
  ctx.value.clearRect(0, 0, MINIMAP_SIZE, MINIMAP_SIZE);
  
  // 绘制背景网格
  ctx.value.strokeStyle = '#e0e0e0';
  ctx.value.lineWidth = 1;
  const cellSize = MINIMAP_SIZE / MINIMAP_CHUNKS_VISIBLE;
  
  for (let i = 0; i <= MINIMAP_CHUNKS_VISIBLE; i++) {
    ctx.value.beginPath();
    ctx.value.moveTo(i * cellSize, 0);
    ctx.value.lineTo(i * cellSize, MINIMAP_SIZE);
    ctx.value.stroke();
    
    ctx.value.beginPath();
    ctx.value.moveTo(0, i * cellSize);
    ctx.value.lineTo(MINIMAP_SIZE, i * cellSize);
    ctx.value.stroke();
  }
  
  // 计算中心区块
  const centerChunkX = Math.floor(centerX / HEATMAP_CHUNK_SIZE);
  const centerChunkY = Math.floor(centerY / HEATMAP_CHUNK_SIZE);
  
  // 绘制热力数据
  const startChunkX = centerChunkX - Math.floor(MINIMAP_CHUNKS_VISIBLE / 2);
  const startChunkY = centerChunkY - Math.floor(MINIMAP_CHUNKS_VISIBLE / 2);
  
  let heatChunksDrawn = 0;
  for (let dy = 0; dy < MINIMAP_CHUNKS_VISIBLE; dy++) {
    for (let dx = 0; dx < MINIMAP_CHUNKS_VISIBLE; dx++) {
      const chunkX = startChunkX + dx;
      const chunkY = startChunkY + dy;
      const heatValue = heatmap.getHeatValue({ gridX: chunkX, gridY: chunkY });
      
      if (heatValue !== undefined && heatValue > 0) {
        const color = getHeatColor(heatValue);
        ctx.value.fillStyle = color;
        ctx.value.fillRect(dx * cellSize, dy * cellSize, cellSize, cellSize);
        heatChunksDrawn++;
      }
    }
  }
  };


// Lifecycle
onMounted(() => {
  if (canvasRef.value) {
    ctx.value = canvasRef.value.getContext('2d');
    
    // 等待下一个 tick，确保 mainCanvasRef 已经设置
    nextTick(() => {
      if (ctx.value && mainCanvasRef.value) {
        // 取消现有的动画帧（如果有）
        if (animationFrameId.value) {
          cancelAnimationFrame(animationFrameId.value);
          animationFrameId.value = null;
        }
        // 启动新的动画循环
        drawMinimap();
      }
    });
  }
});

onUnmounted(() => {
  if (animationFrameId.value) {
    cancelAnimationFrame(animationFrameId.value);
  }
});
</script>

<style scoped>
.minimap-container {
  position: fixed;
  top: 20px;
  left: 20px;
  width: 240px;
  z-index: 400;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 8px;
  overflow: hidden;
}

.minimap-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.95);
  border-bottom: 1px solid rgba(0, 0, 0, 0.1);
}

.minimap-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}


.minimap-content {
  position: relative;
  padding: 16px;
  display: flex;
  justify-content: center;
}

.minimap-canvas {
  cursor: pointer;
}

.viewport-indicator {
  position: absolute;
  border: 2px solid #409EFF;
  background: rgba(64, 158, 255, 0.1);
  pointer-events: none;
  border-radius: 2px;
}

.heatmap-legend {
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.95);
  border-top: 1px solid rgba(0, 0, 0, 0.1);
}

.legend-item {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.legend-item:last-child {
  margin-bottom: 0;
}

.legend-color {
  width: 16px;
  height: 16px;
  border-radius: 2px;
  margin-right: 8px;
  border: 1px solid #e0e0e0;
}

.legend-label {
  font-size: 12px;
  color: #606266;
}
</style>