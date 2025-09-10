import { defineStore } from 'pinia';
import { computed, ref } from 'vue'; // Changed reactive to ref
import type { MessageNodeDTO, MessageDetailDTO } from '../types';
import { getMessageDetail, getMessagesInGrid, getViewportStats } from '../api/message';
import type { Viewport } from '../composables/useCanvas';

// Define the fixed size of our grid cells
export const CELL_SIZE = 150; // e.g., 150x150 pixels
// Define the size of a chunk (in cells) for fetching
const CHUNK_SIZE = 10; // e.g., a 10x10 grid of cells

export const useFluxStore = defineStore('flux', () => {
  console.log('[fluxStore.ts] Setting up Flux store...');
  // ===================================================================
  // STATE
  // ===================================================================

  // Cache for all cell data, keyed by a "row,col" string for efficient lookup
  const cellsCache = ref(new Map<string, MessageNodeDTO>());
  // Ensure cache is clear on store initialization, especially for HMR scenarios.
  cellsCache.value.clear();
  const cells = computed(() => Array.from(cellsCache.value.values()));

  // A set to record which chunks have already been fetched from the backend
  const fetchedChunks = new Set<string>();

  const hoveredMessage = ref<MessageDetailDTO | null>(null); // CORRECT: Use ref for primitive/null values

  // Stats
  const onlineUsers = ref(0);
  const totalMessages = ref(0);
  const visibleMessages = ref(0);
  const viewportInfoCount = ref(0);

  // ===================================================================
  // TOOLS STATE
  // ===================================================================
  const currentTool = ref<'text' | 'paint' | 'erase'>('text');
  const activeColor = ref<string | null>(null);

  // ===================================================================
  // ACTIONS
  // ===================================================================

  function setOnlineUsers(count: number) {
    onlineUsers.value = count;
  }

  function setTotalMessages(count: number) {
    totalMessages.value = count;
  }

  function updateSystemStats(stats: { onlineCount: number; totalMessages: number; visibleMessages: number }) {
    setOnlineUsers(stats.onlineCount);
    setTotalMessages(stats.totalMessages);
    visibleMessages.value = stats.visibleMessages;
  }

  let viewportFetchTimeout: NodeJS.Timeout | null = null;
  
  async function fetchViewportInfoCount(viewport: Viewport, canvasElement: HTMLElement | null) {
    if (!canvasElement) return;

    // Clear existing timeout
    if (viewportFetchTimeout) {
      clearTimeout(viewportFetchTimeout);
    }

    // Set new timeout to fetch after user stops moving/scaling
    viewportFetchTimeout = setTimeout(async () => {
      const rect = canvasElement.getBoundingClientRect();
      const viewWidth = rect.width / viewport.zoom;
      const viewHeight = rect.height / viewport.zoom;
      const worldX = -viewport.x / viewport.zoom;
      const worldY = -viewport.y / viewport.zoom;

      const startRow = Math.floor(worldY / CELL_SIZE);
      const endRow = Math.ceil((worldY + viewHeight) / CELL_SIZE);
      const startCol = Math.floor(worldX / CELL_SIZE);
      const endCol = Math.ceil((worldX + viewWidth) / CELL_SIZE);

      try {
        const count = await getViewportStats({ startRow, endRow, startCol, endCol });
        viewportInfoCount.value = count;
      } catch (error) {
        console.error("Failed to fetch viewport stats:", error);
        viewportInfoCount.value = 0; // Reset on error
      }
    }, 500); // 500ms delay after user stops
  }

  /**
   * The core smart fetching logic for the infinite grid.
   */
  async function fetchGridForViewport(viewport: Viewport, canvasElement: HTMLElement | null) {
    if (!canvasElement) {
      console.warn("fetchGridForViewport called with null canvasElement. Aborting.");
      return;
    }

    const rect = canvasElement.getBoundingClientRect();

    // Calculate the visible range of rows and columns
    const viewWidth = rect.width / viewport.zoom;
    const viewHeight = rect.height / viewport.zoom;
    const worldX = -viewport.x / viewport.zoom;
    const worldY = -viewport.y / viewport.zoom;

    const startRow = Math.floor(worldY / CELL_SIZE);
    const endRow = Math.ceil((worldY + viewHeight) / CELL_SIZE);
    const startCol = Math.floor(worldX / CELL_SIZE);
    const endCol = Math.ceil((worldX + viewWidth) / CELL_SIZE);

    // Determine the chunks that need to be loaded
    const requiredChunks = new Set<string>();
    for (let r = startRow; r <= endRow; r++) {
      for (let c = startCol; c <= endCol; c++) {
        const chunkRow = Math.floor(r / CHUNK_SIZE);
        const chunkCol = Math.floor(c / CHUNK_SIZE);
        requiredChunks.add(`${chunkRow},${chunkCol}`);
      }
    }

    // Fetch all chunks that haven't been fetched yet
    for (const chunkKey of requiredChunks) {
      if (fetchedChunks.has(chunkKey)) {
        continue; // Skip already fetched chunks
      }

      // Optimistically mark the chunk as fetched to prevent re-fetching the same chunk on every interaction.
      fetchedChunks.add(chunkKey);

      const [chunkRow, chunkCol] = chunkKey.split(',').map(Number);
      const fetchParams = {
        startRow: chunkRow * CHUNK_SIZE,
        endRow: (chunkRow + 1) * CHUNK_SIZE - 1,
        startCol: chunkCol * CHUNK_SIZE,
        endCol: (chunkCol + 1) * CHUNK_SIZE - 1,
      };

      try {
        console.log(`Fetching chunk: ${chunkKey}`, fetchParams);
        const fetchedCells = await getMessagesInGrid(fetchParams);

        // Add new cells to the cache
        fetchedCells.forEach(cell => {
          cellsCache.value.set(`${cell.rowIndex},${cell.colIndex}`, cell);
        });

      } catch (error) {
        console.error(`Failed to fetch chunk ${chunkKey}:`, error);
        // If a chunk fails, remove it from the set so it can be retried later if needed.
        fetchedChunks.delete(chunkKey);
        throw error; // Re-throw the error to make it visible in the console.
      }
    }
  }

  async function fetchMessageDetail(id: number) {
    try {
      const detail = await getMessageDetail(id);
      hoveredMessage.value = detail; // CORRECT: Assign to .value
    } catch (error) {
      console.error(`Failed to fetch message detail for id ${id}:`, error);
      // Clear the message if fetching fails
      hoveredMessage.value = null; // CORRECT: Assign to .value
    }
  }

  function clearHoveredMessage() {
    hoveredMessage.value = null; // CORRECT: Assign to .value
  }

  /**
   * Adds or updates a cell in the cache. Used for WebSocket updates.
   */
  function updateCell(newCell: MessageNodeDTO) {
    // Create a new Map to ensure Vue's reactivity system picks up the change.
    const newCache = new Map(cellsCache.value);
    newCache.set(`${newCell.rowIndex},${newCell.colIndex}`, newCell);
    cellsCache.value = newCache;
  }

  /**
   * Deletes a cell from the cache. Used for WebSocket delete messages.
   */
  function deleteCell(rowIndex: number, colIndex: number) {
    // Create a new Map to ensure Vue's reactivity system picks up the change.
    const newCache = new Map(cellsCache.value);
    const key = `${rowIndex},${colIndex}`;
    newCache.delete(key);
    cellsCache.value = newCache;
  }
  function setCurrentTool(tool: 'text' | 'paint' | 'erase') {
    currentTool.value = tool;
  }

  function setActiveColor(color: string | null) {
    activeColor.value = color;
  }
  return {
    // State
    cellsCache, // Expose the cache directly
    cells,
    hoveredMessage,
    currentTool,
    activeColor,
    onlineUsers,
    totalMessages,
    visibleMessages,
    viewportInfoCount,
    // Actions
    fetchGridForViewport,
    fetchViewportInfoCount,
    fetchMessageDetail,
    clearHoveredMessage,
    updateCell,
    deleteCell,
    setCurrentTool,
    setActiveColor,
    setOnlineUsers,
    setTotalMessages,
    updateSystemStats,
  };
});