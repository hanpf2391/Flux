import { defineStore } from 'pinia';
import { ref, readonly } from 'vue';

/**
 * Creates a string identifier for a cell coordinate.
 * @param rowIndex The row index.
 * @param colIndex The column index.
 * @returns A string in the format "row,col".
 */
const getCoordKey = (rowIndex: number, colIndex: number): string => `${rowIndex},${colIndex}`;

/**
 * This store manages the state of cells that are being edited by other users.
 */
export const useLockStore = defineStore('locks', () => {
  // A Set to store the coordinates of remotely locked cells.
  const lockedCells = ref(new Set<string>());

  /**
   * Adds a cell to the set of locked cells.
   * @param rowIndex The row index of the cell to lock.
   * @param colIndex The column index of the cell to lock.
   */
  function addLock(rowIndex: number, colIndex: number) {
    const key = getCoordKey(rowIndex, colIndex);
    if (!lockedCells.value.has(key)) {
      const newSet = new Set(lockedCells.value);
      newSet.add(key);
      lockedCells.value = newSet; // Ensure reactivity by creating a new Set
    }
  }

  /**
   * Removes a cell from the set of locked cells.
   * @param rowIndex The row index of the cell to unlock.
   * @param colIndex The column index of the cell to unlock.
   */
  function removeLock(rowIndex: number, colIndex: number) {
    const key = getCoordKey(rowIndex, colIndex);
    if (lockedCells.value.has(key)) {
      const newSet = new Set(lockedCells.value);
      newSet.delete(key);
      lockedCells.value = newSet; // Ensure reactivity
    }
  }

  /**
   * Checks if a specific cell is currently locked by another user.
   * @param rowIndex The row index of the cell to check.
   * @param colIndex The column index of the cell to check.
   * @returns True if the cell is locked, false otherwise.
   */
  function isLocked(rowIndex: number, colIndex: number): boolean {
    return lockedCells.value.has(getCoordKey(rowIndex, colIndex));
  }

  return {
    lockedCells: readonly(lockedCells), // Expose as readonly to prevent direct mutation
    addLock,
    removeLock,
    isLocked,
  };
});
