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

      <!-- 移动端热力图开关 -->
      <div
        v-if="isMobile"
        class="tool-icon heatmap-icon"
        :class="{ 'is-active': isHeatmapVisible }"
        @click="handleHeatmapToggle"
        title="显示/隐藏热力图"
      >
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="24" height="24">
          <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path>
          <polyline points="3.27 6.96 12 12.01 20.73 6.96"></polyline>
          <line x1="12" y1="22.08" x2="12" y2="12"></line>
          <circle cx="12" cy="12" r="3"></circle>
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
      @touchstart="handleCanvasTouchStart"
      @touchmove="handleCanvasTouchMove"
      @touchend="handleCanvasTouchEnd"
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
          :is-touch-click="isTouchClick"
          @update-text="(payload, callbacks) => handleUpdateText(cell.rowIndex, cell.colIndex, payload, callbacks)"
          @update-color="() => handleUpdateColor(cell.rowIndex, cell.colIndex)"
          @drag-paint="() => handleDragPaint(cell.rowIndex, cell.colIndex)"
        ></GridCell>
      </div>
    </div>

    <!-- Heatmap Minimap -->
    <HeatmapMinimap 
      v-if="!isMobile || isHeatmapVisible"
      :viewport="viewport"
      :canvas-ref="canvasRef"
      style="display: block;"
      :class="{ 'mobile-heatmap': isMobile }"
    />
    
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue';
import { storeToRefs } from 'pinia';
import { debounce } from 'lodash-es';
import { useCanvas } from '../composables/useCanvas';
import { useFluxStore, CELL_SIZE, MOBILE_CELL_SIZE } from '../store/fluxStore';
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
const isTouching = ref(false);
const isHeatmapVisible = ref(false); // 移动端热力图显示状态
let touchStartPoint = { x: 0, y: 0 };
let lastTouchDistance = 0;
const isMobile = ref(false);
let touchStartTime = 0;
let touchEndPoint = { x: 0, y: 0 };
const TOUCH_MOVE_THRESHOLD = 15; // 移动阈值，超过这个距离认为是滑动而不是点击
let isSliding = false; // 明确标记是否在滑动

// Check if mobile device
const checkMobile = () => {
  isMobile.value = window.innerWidth <= 768;
};


// --- COMPUTED ---
const worldStyle = computed(() => ({
  transform: `translate(${viewport.x}px, ${viewport.y}px) scale(${viewport.zoom})`,
}));

const currentCellSize = computed(() => isMobile.value ? MOBILE_CELL_SIZE : CELL_SIZE);

const canvasStyle = computed(() => {
  const gridSize = currentCellSize.value * viewport.zoom;
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
  const centerX = Math.floor((-viewport.x + canvasRef.value.clientWidth / 2) / viewport.zoom / currentCellSize.value);
  const centerY = Math.floor((-viewport.y + canvasRef.value.clientHeight / 2) / viewport.zoom / currentCellSize.value);
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
  const startRow = Math.floor(worldY / currentCellSize.value) - buffer;
  const endRow = Math.ceil((worldY + viewHeight) / currentCellSize.value) + buffer;
  const startCol = Math.floor(worldX / currentCellSize.value) - buffer;
  const endCol = Math.ceil((worldX + viewWidth) / currentCellSize.value) + buffer;

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
  transform: `translate(${cell.colIndex * currentCellSize.value}px, ${cell.rowIndex * currentCellSize.value}px)`,
  width: `${currentCellSize.value}px`,
  height: `${currentCellSize.value}px`,
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

const handleHeatmapToggle = () => {
  isHeatmapVisible.value = !isHeatmapVisible.value;
};

// 计算两点之间的距离
const getDistance = (x1: number, y1: number, x2: number, y2: number) => {
  return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
};

// 退出所有单元格的编辑状态
const exitAllCellEditing = () => {
  // 通过点击空白区域来强制退出所有编辑状态
  document.activeElement instanceof HTMLElement && document.activeElement.blur();
  
  // 如果有聚焦的文本框，让它失去焦点
  const focusedElement = document.querySelector('textarea:focus, input:focus');
  if (focusedElement instanceof HTMLElement) {
    focusedElement.blur();
  }
};

// 判断是否为点击（而非滑动）
const isTouchClick = () => {
  // 如果已经标记为滑动，直接返回false
  if (isSliding) return false;
  
  // 计算从开始到结束的总移动距离
  const moveDistance = getDistance(touchStartPoint.x, touchStartPoint.y, touchEndPoint.x, touchEndPoint.y);
  const touchDuration = Date.now() - touchStartTime;
  
  return moveDistance < TOUCH_MOVE_THRESHOLD && touchDuration < 500; // 500ms内移动距离小于阈值认为是点击
};



const handleColorChange = (newColor: string) => {
  if (newColor) {
    setActiveColor(newColor);
    // 在移动端选择颜色后自动关闭色盘
    if (isMobile.value) {
      isPaletteOpen.value = false;
    }
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
    content: currentCell?.content ?? undefined, // Convert null to undefined for DTO
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

const handleCanvasTouchStart = (event: TouchEvent) => {
  event.preventDefault();
  isTouching.value = true;
  touchStartTime = Date.now();
  isSliding = false; // 重置滑动标志
  
  if (event.touches.length > 0) {
    touchStartPoint = {
      x: event.touches[0].clientX,
      y: event.touches[0].clientY
    };
    touchEndPoint = { x: touchStartPoint.x, y: touchStartPoint.y };
  }
  
  if (event.touches.length === 2) {
    // 双指触摸 - 立即退出编辑状态
    exitAllCellEditing();
    // 记录初始距离用于缩放
    lastTouchDistance = getDistance(
      event.touches[0].clientX,
      event.touches[0].clientY,
      event.touches[1].clientX,
      event.touches[1].clientY
    );
  }
};

const handleCanvasTouchMove = (event: TouchEvent) => {
  event.preventDefault();
  if (!isTouching.value || !canvasRef.value) return;
  
  if (event.touches.length === 1) {
    // 单指触摸 - 平移
    const touch = event.touches[0];
    const deltaX = touch.clientX - touchStartPoint.x;
    const deltaY = touch.clientY - touchStartPoint.y;
    
    viewport.x += deltaX;
    viewport.y += deltaY;
    
    // 更新触摸结束点，但不要更新起点，以便正确计算移动距离
    touchEndPoint = {
      x: touch.clientX,
      y: touch.clientY
    };
    
    // 更新当前触摸点为新的起点，但保留原始起点用于点击判断
    const currentTouchPoint = {
      x: touch.clientX,
      y: touch.clientY
    };
    
    // 检查是否已经超过点击阈值，如果超过就标记为滑动并退出编辑状态
    const currentMoveDistance = getDistance(touchStartPoint.x, touchStartPoint.y, currentTouchPoint.x, currentTouchPoint.y);
    if (currentMoveDistance >= TOUCH_MOVE_THRESHOLD) {
      if (!isSliding) {
        // 第一次超过阈值，退出所有编辑状态
        exitAllCellEditing();
      }
      isSliding = true; // 标记为滑动
    }
    
    // 更新起点为当前位置，以便下一次移动计算
    touchStartPoint = currentTouchPoint;
  } else if (event.touches.length === 2) {
    // 双指触摸 - 缩放
    const currentDistance = getDistance(
      event.touches[0].clientX,
      event.touches[0].clientY,
      event.touches[1].clientX,
      event.touches[1].clientY
    );
    
    if (lastTouchDistance > 0) {
      const scale = currentDistance / lastTouchDistance;
      const newZoom = Math.max(0.1, Math.min(5, viewport.zoom * scale));
      
      // 计算缩放中心点
      const centerX = (event.touches[0].clientX + event.touches[1].clientX) / 2;
      const centerY = (event.touches[0].clientY + event.touches[1].clientY) / 2;
      
      // 调整viewport位置以保持缩放中心
      const rect = canvasRef.value.getBoundingClientRect();
      const worldX = (centerX - rect.left - viewport.x) / viewport.zoom;
      const worldY = (centerY - rect.top - viewport.y) / viewport.zoom;
      
      viewport.zoom = newZoom;
      
      viewport.x = centerX - rect.left - worldX * viewport.zoom;
      viewport.y = centerY - rect.top - worldY * viewport.zoom;
    }
    
    lastTouchDistance = currentDistance;
    
    // 双指操作时，标记为滑动并退出编辑状态
    if (!isSliding) {
      exitAllCellEditing();
    }
    isSliding = true;
  }
};

const handleCanvasTouchEnd = (event: TouchEvent) => {
  event.preventDefault();
  isTouching.value = false;
  lastTouchDistance = 0;
  
  // 如果是触摸结束，更新touchEndPoint为当前位置
  if (event.changedTouches.length > 0) {
    touchEndPoint = {
      x: event.changedTouches[0].clientX,
      y: event.changedTouches[0].clientY
    };
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

let viewportFetchTimeout: ReturnType<typeof setTimeout> | null = null;

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
  // Check if mobile device
  checkMobile();
  window.addEventListener('resize', checkMobile);
  
  // Fetch initial position for intelligent user positioning
  try {
    const initialPosition: CanvasInitialPositionDTO = await getInitialPosition();
    
    // Calculate viewport coordinates to center the initial position
    if (canvasRef.value) {
      const targetWorldX = initialPosition.colIndex * currentCellSize.value;
      const targetWorldY = initialPosition.rowIndex * currentCellSize.value;
      
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
  window.removeEventListener('resize', checkMobile);
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

/* 移动端适配 */
@media (max-width: 768px) {
  .flux-container {
    /* 允许平移操作，但禁用浏览器默认的缩放和双击缩放 */
    touch-action: pan-x pan-y;
  }
  
  .tool-corner {
    top: 10px;
    right: 10px;
    gap: 8px;
  }
  
  .stats-corner {
    top: 10px;
    left: 10px;
    padding: 6px 12px;
    font-size: 0.8rem;
  }
  
  .coordinate-display {
    padding: 6px 12px;
    font-size: 0.9rem;
  }
  
  .tool-icon {
    width: 36px;
    height: 36px;
  }
  
  .palette-container {
    width: 240px;
    max-height: 300px;
    padding: 8px;
  }
  
  .palette-header {
    margin-bottom: 12px;
    padding-bottom: 8px;
  }
  
  .palette-title {
    font-size: 13px;
  }
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
  fill: #409EFF;
}

/* 移动端热力图样式 */
:deep(.mobile-heatmap) {
  position: fixed !important;
  top: 60px !important;
  left: 10px !important;
  width: 140px !important;
  border-radius: 8px !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15) !important;
  background: rgba(255, 255, 255, 0.95) !important;
  z-index: 1000 !important;
}

:deep(.mobile-heatmap .minimap-header) {
  display: none !important;
}

:deep(.mobile-heatmap .minimap-content) {
  padding: 8px !important;
}

:deep(.mobile-heatmap .minimap-canvas) {
  border-radius: 4px !important;
}

:deep(.mobile-heatmap .heatmap-legend) {
  padding: 8px !important;
  background: rgba(255, 255, 255, 0.95) !important;
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