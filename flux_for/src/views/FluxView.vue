<template>
  <div class="flux-container">
    <!-- Tool Corner -->
    <div class="tool-corner">
      <div class="coordinate-display">
        {{ coordinateText }}
      </div>

      <el-popover
        ref="colorPickerPopover"
        placement="left"
        trigger="manual"
        v-model:visible="isPaletteOpen"
        width="auto"
      >
        <template #reference>
          <div
            class="tool-icon palette-icon"
            :style="paletteStyle"
            @click="handlePaletteClick"
          >
            🎨
          </div>
        </template>
        <el-color-picker
          ref="colorPickerComponentRef"
          v-model="activeColor"
          color-format="hex"
          @active-change="handleColorChange"
        />
      </el-popover>

      <div
        class="tool-icon eraser-icon"
        :class="{ 'is-active': currentTool === 'erase' }"
        @click="handleEraserClick"
      >
        <svg viewBox="0 0 1024 1024" xmlns="http://www.w3.org/2000/svg" width="24" height="24"><path d="M898.2 393.4L629.8 125.2c-23.4-23.4-61.4-23.4-84.8 0L129.2 541c-23.4 23.4-23.4 61.4 0 84.8l268.4 268.4c23.4 23.4 61.4 23.4 84.8 0l415.8-415.8c23.4-23.4 23.4-61.4 0-85zM348.8 820.6L80.4 552.2c-7.8-7.8-7.8-20.5 0-28.3l415.8-415.8c7.8-7.8 20.5-7.8 28.3 0l268.4 268.4c7.8 7.8 7.8 20.5 0 28.3L377 820.6c-7.8 7.8-20.4 7.8-28.2 0z"/><path d="M728.2 200.2c-7.8-7.8-20.5-7.8-28.3 0L302.8 597.3c-7.8 7.8-7.8 20.5 0 28.3l113.2 113.2c7.8 7.8 20.5 7.8 28.3 0l415.8-415.8c7.8-7.8 7.8-20.5 0-28.3L728.2 200.2z"/></svg>
      </div>
    </div>

    <!-- The main canvas for interaction -->
    <div
      ref="canvasRef"
      class="flux-canvas"
      :style="canvasStyle"
      @mousedown="isMouseDown = true"
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue';
import { storeToRefs } from 'pinia';
import { debounce } from 'lodash-es';
import { useCanvas } from '../composables/useCanvas';
import { useFluxStore, CELL_SIZE } from '../store/fluxStore';
import { useWebSocket } from '../composables/useWebSocket';
import { createMessage } from '../api/message';
import { isAxiosError } from 'axios';
import GridCell, { type ErrorType } from '../components/GridCell.vue';
import type { CreateMessageDTO } from '../types';

// --- SETUP ---
const canvasRef = ref<HTMLElement | null>(null);
const store = useFluxStore();
const { viewport } = useCanvas(canvasRef);
useWebSocket(); // Connect and listen for real-time updates

const { currentTool, activeColor } = storeToRefs(store);
const { setCurrentTool, setActiveColor } = store;

const colorPickerComponentRef = ref();
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
  setActiveColor(null); // FR-2.3: Reset color on exit
  isPaletteOpen.value = false;
};

const handlePaletteClick = () => {
  isPaletteOpen.value = !isPaletteOpen.value;
  if (isPaletteOpen.value) {
    setCurrentTool('paint');
  } else {
    exitActiveTool();
  }
};

const handleEraserClick = () => {
  if (currentTool.value === 'erase') {
    exitActiveTool();
  } else {
    setCurrentTool('erase');
    isPaletteOpen.value = false; // Ensure palette is closed
  }
};

const handleColorChange = (newColor: string) => {
  if (newColor) {
    setActiveColor(newColor);
    setCurrentTool('paint');
  }
};

const handleUpdate = async (rowIndex: number, colIndex: number, payload: Partial<CreateMessageDTO>, callbacks?: { onError: (type: ErrorType) => void }) => {
  try {
    const newMessage: CreateMessageDTO = {
      ...payload,
      rowIndex,
      colIndex,
    };
    await createMessage(newMessage);
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
  handleUpdate(rowIndex, colIndex, payload, callbacks);
};

const handleUpdateColor = (rowIndex: number, colIndex: number) => {
  const newBgColor = currentTool.value === 'paint' ? activeColor.value : null;
  const currentCell = getCellData(rowIndex, colIndex);

  if (currentCell?.bgColor === newBgColor) return;

  handleUpdate(rowIndex, colIndex, { bgColor: newBgColor, baseVersionId: currentCell?.id ?? null });
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
  if (isOpen) {
    nextTick(() => {
      const pickerEl = colorPickerComponentRef.value?.$el;
      if (pickerEl) {
        const trigger = pickerEl.querySelector('.el-color-picker__trigger');
        trigger?.click();
      }
    });
  }
});

const debouncedFetch = debounce(() => {
  if (canvasRef.value) {
    store.fetchGridForViewport(viewport, canvasRef.value);
  }
}, 250);

watch(viewport, debouncedFetch, { deep: true });

onMounted(() => {
  debouncedFetch();
  window.addEventListener('keydown', handleKeyDown);
});

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeyDown);
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
</style>