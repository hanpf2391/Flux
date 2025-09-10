<template>
  <div class="flux-container">
    <!-- Stats Corner -->
    <div class="stats-corner">
      在线: {{ store.onlineUsers }} | 总数: {{ store.totalMessages }} | 视区: {{ store.viewportInfoCount }}
    </div>

    <!-- Tool Corner -->
    <div class="tool-corner">
      <div class="coordinate-display">
        {{ coordinateText }}
      </div>

      <!-- 色盘容器 -->
      <div class="palette-container" v-if="currentTool === 'paint' && isPaletteOpen">
        <div class="palette-header">
          <span class="palette-title">颜色选择</span>
          <div 
            class="palette-close"
            @click="exitActiveTool"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </div>
        </div>
        <ColorPalette 
          v-model="activeColor"
          @change="handleColorChange"
        />
      </div>
      
      <div
        class="tool-icon palette-icon"
        :style="paletteStyle"
        :class="{ 'is-active': currentTool === 'paint' }"
        @click="handlePaletteClick"
      >
        🎨
      </div>

      <div
        class="tool-icon eraser-icon"
        :class="{ 'is-active': currentTool === 'erase' }"
        @click="handleEraserClick"
      >
        <svg viewBox="0 0 1024 1024" xmlns="http://www.w3.org/2000/svg" width="24" height="24"><path d="M898.2 393.4L629.8 125.2c-23.4-23.4-61.4-23.4-84.8 0L129.2 541c-23.4 23.4-23.4 61.4 0 84.8l268.4 268.4c23.4 23.4 61.4 23.4 84.8 0l415.8-415.8c23.4-23.4 23.4-61.4 0-85zM348.8 820.6L80.4 552.2c-7.8-7.8-7.8-20.5 0-28.3l415.8-415.8c7.8-7.8 20.5-7.8 28.3 0l268.4 268.4c7.8 7.8 7.8 20.5 0 28.3L377 820.6c-7.8 7.8-20.4 7.8-28.2 0z"/><path d="M728.2 200.2c-7.8-7.8-20.5-7.8-28.3 0L302.8 597.3c-7.8 7.8-7.8 20.5 0 28.3l113.2 113.2c7.8 7.8 20.5 7.8 28.3 0l415.8-415.8c7.8-7.8 7.8-20.5 0-28.3L728.2 200.2z"/></svg>
      </div>

      <div
        class="tool-icon home-icon"
        @click="handleHomeClick"
        title="回到原点 (0,0)"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="24" height="24">
          <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
          <polyline points="9 22 9 12 15 12 15 22"></polyline>
        </svg>
      </div>

      
      </div>

    <!-- The main canvas for interaction -->
    <div
      ref="canvasRef"
      class="flux-canvas"
      :style="canvasStyle"
      @mousedown="handleCanvasMouseDown"
      @mouseup="isMouseDown = false"
      @mouseleave="isMouseDown = false"
    >
      <!-- The world that moves and zooms -->
      <div
        class="world-container"
        :style="worldStyle"
      >
        <!-- Render each visible grid cell -->
        <GridCell
          v-for="cell in visibleCells"
          :key="`${cell.rowIndex},${cell.colIndex}`"
          :style="getCellStyle(cell)"
          :cell-data="getCellData(cell.rowIndex, cell.colIndex)"
          :row-index="cell.rowIndex"
          :col-index="cell.colIndex"
          @update-text="(payload, callbacks) => handleUpdateText(cell.rowIndex, cell.colIndex, payload, callbacks)"
          @update-color="() => handleUpdateColor(cell.rowIndex, cell.colIndex)"
          @drag-paint="() => handleDragPaint(cell.rowIndex, cell.colIndex)"
        ></GridCell>
      </div>
    </div>

    <!-- Heatmap Minimap -->
    <HeatmapMinimap 
      :viewport="viewport"
      :canvas-ref="canvasRef"
      style="display: block;"
    />
    
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue';
import { storeToRefs } from 'pinia';
import { debounce } from 'lodash-es';
import { useCanvas } from '../composables/useCanvas';
import { useFluxStore, CELL_SIZE } from '../store/fluxStore';
import { useWebSocket } from '../composables/useWebSocket';
import { createMessage, getStats } from '../api/message';
import { getInitialPosition } from '../api/canvas';
import { isAxiosError } from 'axios';
import GridCell, { type ErrorType } from '../components/GridCell.vue';
import ColorPalette from '../components/ColorPalette.vue';
import HeatmapMinimap from '../components/HeatmapMinimap.vue';
import type { CreateMessageDTO, CanvasInitialPositionDTO } from '../types';

// --- SETUP ---
const canvasRef = ref<HTMLElement | null>(null);
const store = useFluxStore();
const { viewport } = useCanvas(canvasRef);
useWebSocket(); // Connect and listen for real-time updates

const { currentTool, activeColor } = storeToRefs(store);
const { setCurrentTool, setActiveColor } = store;

const isMouseDown = ref(false);
const isPaletteOpen = ref(false);


// --- COMPUTED ---
const worldStyle = computed(() => ({
  transform: `translate(${viewport.x}px, ${viewport.y}px) scale(${viewport.zoom})`,
}));

const canvasStyle = computed(() => {
  const gridSize = CELL_SIZE * viewport.zoom;
  const cursorMap = {
    text: 'default',
    paint: 'pointer', // Per design spec FR-1.3
    erase: 'crosshair', // A crosshair is a reasonable eraser cursor
  };
  return {
    backgroundSize: `${gridSize}px ${gridSize}px`,
    backgroundPosition: `${viewport.x}px ${viewport.y}px`,
    cursor: cursorMap[currentTool.value] || 'default',
  };
});

const paletteStyle = computed(() => ({
  backgroundColor: activeColor.value || '#FFC0CB',
}));

const coordinateText = computed(() => {
  if (!canvasRef.value) return '(0, 0)';
  const centerX = Math.floor((-viewport.x + canvasRef.value.clientWidth / 2) / viewport.zoom / CELL_SIZE);
  const centerY = Math.floor((-viewport.y + canvasRef.value.clientHeight / 2) / viewport.zoom / CELL_SIZE);
  return `(${centerX}, ${centerY})`;
});

const visibleCells = computed(() => {
  if (!canvasRef.value) return [];

  const rect = canvasRef.value.getBoundingClientRect();
  const viewWidth = rect.width / viewport.zoom;
  const viewHeight = rect.height / viewport.zoom;
  const worldX = -viewport.x / viewport.zoom;
  const worldY = -viewport.y / viewport.zoom;

  const buffer = 2;
  const startRow = Math.floor(worldY / CELL_SIZE) - buffer;
  const endRow = Math.ceil((worldY + viewHeight) / CELL_SIZE) + buffer;
  const startCol = Math.floor(worldX / CELL_SIZE) - buffer;
  const endCol = Math.ceil((worldX + viewWidth) / CELL_SIZE) + buffer;

  const cellsToRender = [];
  for (let r = startRow; r < endRow; r++) {
    for (let c = startCol; c < endCol; c++) {
      cellsToRender.push({ rowIndex: r, colIndex: c });
    }
  }
  return cellsToRender;
});


// --- METHODS ---
const getCellStyle = (cell: { rowIndex: number; colIndex: number }) => ({
  transform: `translate(${cell.colIndex * CELL_SIZE}px, ${cell.rowIndex * CELL_SIZE}px)`,
  width: `${CELL_SIZE}px`,
  height: `${CELL_SIZE}px`,
});

const getCellData = (row: number, col: number) => {
  return store.cellsCache.get(`${row},${col}`);
};

const exitActiveTool = () => {
  setCurrentTool('text');
  setActiveColor('#FFC0CB'); // FR-2.3: Reset color on exit
  isPaletteOpen.value = false;
};

const handlePaletteClick = () => {
  if (currentTool.value === 'paint') {
    isPaletteOpen.value = !isPaletteOpen.value;
  } else {
    setCurrentTool('paint');
    isPaletteOpen.value = true;
  }
};

const handleEraserClick = () => {
  if (currentTool.value === 'erase') {
    exitActiveTool();
  } else {
    setCurrentTool('erase');
  }
};

const handleHomeClick = () => {
  if (canvasRef.value) {
    // 计算到原点 (0,0) 的 viewport 位置
    // 世界坐标 (0,0) 对应的像素位置是 (0, 0)
    // 我们需要将这个点定位到画布中心
    const targetWorldX = 0;
    const targetWorldY = 0;
    
    // 设置 viewport 使得原点位于画布中心
    viewport.x = canvasRef.value.clientWidth / 2 - targetWorldX * viewport.zoom;
    viewport.y = canvasRef.value.clientHeight / 2 - targetWorldY * viewport.zoom;
    
      }
};



const handleColorChange = (newColor: string) => {
  if (newColor) {
    setActiveColor(newColor);
  }
};

const handlePaletteShow = () => {
  // 已废弃，不再需要
};

const handlePaletteHide = () => {
  // 已废弃，不再需要
};

const handleUpdate = async (rowIndex: number, colIndex: number, payload: Partial<CreateMessageDTO>, callbacks?: { onError: (type: ErrorType) => void }) => {
  try {
    const newMessage: CreateMessageDTO = {
      ...payload,
      rowIndex,
      colIndex,
    };
    await createMessage(newMessage);
    
    // Update heatmap chunk value after successful update
    store.updateChunkHeatValue(rowIndex, colIndex, 1);
    
    // Update viewport stats immediately after successful update
    if (canvasRef.value) {
      // Cancel any pending debounced calls
      if (viewportFetchTimeout) {
        clearTimeout(viewportFetchTimeout);
      }
      // Fetch immediately for real-time update
      store.fetchViewportInfoCount(viewport, canvasRef.value);
      store.fetchGridForViewport(viewport, canvasRef.value);
    }
  } catch (error) {
    console.error("Failed to update cell via API:", error);
    if (callbacks) {
      if (isAxiosError(error) && error.response?.status === 409) {
        callbacks.onError('conflict');
      } else {
        callbacks.onError('generic');
      }
    }
  }
};

const handleUpdateText = (
  rowIndex: number,
  colIndex: number,
  payload: { content: string; baseVersionId: number | null },
  callbacks: { onError: (type: ErrorType) => void }
) => {
  const currentCell = getCellData(rowIndex, colIndex);
  const updatePayload = {
    ...payload,
    bgColor: currentCell?.bgColor || null, // Preserve existing background color
  };
  handleUpdate(rowIndex, colIndex, updatePayload, callbacks);
};

const handleUpdateColor = (rowIndex: number, colIndex: number) => {
  const newBgColor = currentTool.value === 'paint' ? activeColor.value : null;
  const currentCell = getCellData(rowIndex, colIndex);

  if (currentCell?.bgColor === newBgColor) return;

  // Always preserve existing content when updating color
  const updatePayload = {
    bgColor: newBgColor,
    content: currentCell?.content || null, // Preserve existing content
    baseVersionId: currentCell?.id ?? null
  };

  handleUpdate(rowIndex, colIndex, updatePayload);
};

const handleCanvasMouseDown = (event: MouseEvent) => {
  // 只有在非Ctrl键按下时才设置鼠标按下状态
  if (!event.ctrlKey && !event.metaKey) {
    isMouseDown.value = true;
  }
};

const handleDragPaint = (rowIndex: number, colIndex: number) => {
  if (isMouseDown.value) {
    handleUpdateColor(rowIndex, colIndex);
  }
};

const handleKeyDown = (event: KeyboardEvent) => {
  if (event.key === 'Escape') {
    exitActiveTool();
  }
};

// --- WATCHERS & LIFECYCLE ---
watch(isPaletteOpen, (isOpen) => {
  // Handle palette show/hide in the dedicated event handlers
});

let viewportFetchTimeout: NodeJS.Timeout | null = null;

const debouncedFetch = () => {
  if (viewportFetchTimeout) {
    clearTimeout(viewportFetchTimeout);
  }
  viewportFetchTimeout = setTimeout(() => {
    if (canvasRef.value) {
      store.fetchGridForViewport(viewport, canvasRef.value);
      store.fetchViewportInfoCount(viewport, canvasRef.value);
    }
  }, 500);
};

watch(viewport, debouncedFetch, { deep: true });

onMounted(async () => {
  // Fetch initial position for intelligent user positioning
  try {
    const initialPosition: CanvasInitialPositionDTO = await getInitialPosition();
    
    // Calculate viewport coordinates to center the initial position
    if (canvasRef.value) {
      const targetWorldX = initialPosition.colIndex * CELL_SIZE;
      const targetWorldY = initialPosition.rowIndex * CELL_SIZE;
      
      // Set viewport to center on the initial position
      viewport.x = canvasRef.value.clientWidth / 2 - targetWorldX;
      viewport.y = canvasRef.value.clientHeight / 2 - targetWorldY;
      
          }
  } catch (error) {
    console.error("Failed to get initial position, using default center:", error);
    // Fallback to center of canvas if API fails
    if (canvasRef.value) {
      viewport.x = canvasRef.value.clientWidth / 2;
      viewport.y = canvasRef.value.clientHeight / 2;
    }
  }
  
  debouncedFetch();
  window.addEventListener('keydown', handleKeyDown);
  
  try {
    const stats = await getStats();
    store.updateSystemStats({
      onlineCount: stats.onlineUsers,
      totalMessages: stats.totalMessages,
      visibleMessages: stats.visibleMessages
    });
  } catch (error) {
    console.error("Failed to fetch initial stats:", error);
  }
});

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeyDown);
  if (viewportFetchTimeout) {
    clearTimeout(viewportFetchTimeout);
  }
});

</script>

<style scoped>
.flux-container {
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  position: relative;
  background-color: #f0f2f5;
}

.flux-canvas {
  width: 100%;
  height: 100%;
  background-image:
    linear-gradient(#e0e0e0 1px, transparent 1px),
    linear-gradient(to right, #e0e0e0 1px, transparent 1px);
  border-left: 1px solid #e0e0e0;
  border-top: 1px solid #e0e0e0;
}

.world-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 1px;
  height: 1px;
  transform-origin: 0 0;
}

.tool-corner {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 500;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
}

.palette-container {
  background: white;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  border: 1px solid #e4e7ed;
  width: 320px;
  max-height: 500px;
  overflow-y: auto;
  animation: slideIn 0.3s ease-out;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateX(20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.palette-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.palette-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.palette-close {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  color: #606266;
}

.palette-close:hover {
  background-color: #f5f7fa;
  color: #409EFF;
}

.coordinate-display {
  background-color: rgba(15, 15, 15, 0.8);
  backdrop-filter: blur(5px);
  padding: 8px 15px;
  border-radius: 8px;
  font-family: 'Courier New', Courier, monospace;
  font-size: 1rem;
  color: #ffffff;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.tool-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background-color: #ffffff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
  transition: all 0.2s ease;
  border: 2px solid transparent;
}

.tool-icon:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}

.tool-icon.is-active {
  border-color: #409EFF;
}

.palette-icon {
  font-size: 20px;
}

.eraser-icon svg {
  fill: #303133;
}

.eraser-icon.is-active svg {
  fill: #409EFF;
}

.home-icon svg {
  stroke: #303133;
}

.home-icon:hover svg {
  stroke: #409EFF;
}

.heatmap-icon svg {
  stroke: #303133;
}

.heatmap-icon:hover svg {
  stroke: #409EFF;
}

.heatmap-icon.is-active svg {
  stroke: #409EFF;
}


.stats-corner {
  position: fixed;
  top: 20px;
  left: 20px;
  z-index: 500;
  background-color: rgba(15, 15, 15, 0.8);
  backdrop-filter: blur(5px);
  padding: 8px 15px;
  border-radius: 8px;
  color: #ffffff;
  font-family: 'Courier New', Courier, monospace;
  font-size: 0.9rem;
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: top 0.3s ease;
}

</style>