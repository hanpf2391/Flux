<template>
  <div class="flux-container">
    <!-- Coordinate Display -->
    <div class="coordinate-display">
      {{ coordinateText }}
    </div>

    <!-- The main canvas for interaction -->
    <div
      ref="canvasRef"
      class="flux-canvas"
      :style="canvasStyle"
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
          @update-cell="(payload, callbacks) => handleUpdateCell(cell.rowIndex, cell.colIndex, payload, callbacks)"
        ></GridCell>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue';
import { storeToRefs } from 'pinia';
import { debounce } from 'lodash-es';
import { useCanvas } from '../composables/useCanvas';
import { useFluxStore, CELL_SIZE } from '../store/fluxStore';
import { useWebSocket } from '../composables/useWebSocket';
import { createMessage } from '../api/message';
import { isAxiosError } from 'axios';
import GridCell, { type ErrorType } from '../components/GridCell.vue';

// --- SETUP ---
const canvasRef = ref<HTMLElement | null>(null);
const store = useFluxStore();
const { viewport } = useCanvas(canvasRef);
useWebSocket(); // Connect and listen for real-time updates

// --- COMPUTED ---
const worldStyle = computed(() => ({
  transform: `translate(${viewport.x}px, ${viewport.y}px) scale(${viewport.zoom})`,
}));

const canvasStyle = computed(() => {
  const gridSize = CELL_SIZE * viewport.zoom;
  return {
    backgroundSize: `${gridSize}px ${gridSize}px`,
    backgroundPosition: `${viewport.x}px ${viewport.y}px`,
  };
});

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

const handleUpdateCell = async (
  rowIndex: number,
  colIndex: number,
  payload: { content: string; baseVersionId: number | null },
  callbacks: { onError: (type: ErrorType) => void }
) => {
  try {
    const newMessage = {
      ...payload,
      rowIndex,
      colIndex,
    };
    await createMessage(newMessage);
  } catch (error) {
    console.error("Failed to update cell via API:", error);
    // Check if it's an Axios error and if the status is 409 Conflict
    if (isAxiosError(error) && error.response?.status === 409) {
      callbacks.onError('conflict');
    } else {
      callbacks.onError('generic');
    }
  }
};

// --- WATCHERS & LIFECYCLE ---
const debouncedFetch = debounce(() => {
  if (canvasRef.value) {
    store.fetchGridForViewport(viewport, canvasRef.value);
  }
}, 250);

watch(viewport, debouncedFetch, { deep: true });

onMounted(() => {
  debouncedFetch();
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
  cursor: default;
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

.coordinate-display {
  position: fixed;
  top: 20px;
  right: 20px;
  background-color: rgba(15, 15, 15, 0.8);
  backdrop-filter: blur(5px);
  padding: 8px 15px;
  border-radius: 8px;
  font-family: 'Courier New', Courier, monospace;
  font-size: 1rem;
  color: #ffffff;
  z-index: 500;
  border: 1px solid rgba(255, 255, 255, 0.1);
}
</style>
