<template>
  <div
    class="grid-cell"
    :class="statusClass"
    @click="startEditing"
  >
    <!-- Conflict State Overlay -->
    <div v-if="status === 'conflict'" class="conflict-overlay" :title="conflictTooltipText">
      <span class="conflict-icon">!</span>
    </div>

    <textarea
      v-if="isEditing"
      ref="textareaRef"
      v-model="editableContent"
      class="cell-editor"
      :readonly="status === 'saving'"
      @blur="handleSave"
      @keydown.enter.prevent="handleSave"
      @keydown.esc.prevent="cancelEditing"
    ></textarea>
    <div v-else class="cell-content">
      {{ cellData?.content }}
    </div>

    <!-- The remote editing indicator is now a simple border on the main div via CSS -->

  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick, computed } from 'vue';
import type { PropType } from 'vue';
import type { MessageNodeDTO } from '../types';
import { useWebSocket } from '../composables/useWebSocket';
import { useLockStore } from '../store/lockStore';

// --- Types ---
type EditStatus = 'idle' | 'editing' | 'saving' | 'error' | 'conflict';
export type ErrorType = 'generic' | 'conflict';

// --- Props & Emits ---
const props = defineProps({
  cellData: {
    type: Object as PropType<MessageNodeDTO | undefined>,
    default: undefined,
  },
  rowIndex: {
    type: Number,
    required: true,
  },
  colIndex: {
    type: Number,
    required: true,
  },
});

const emit = defineEmits<{
  (e: 'update-cell', payload: { content: string; baseVersionId: number | null }, callbacks: { onError: (type: ErrorType) => void }): void;
}>();

// --- Composables ---
const { sendMessage } = useWebSocket();
const lockStore = useLockStore();

// --- State ---
const status = ref<EditStatus>('idle');
const editableContent = ref('');
const baseVersionId = ref<number | null>(null);
const textareaRef = ref<HTMLTextAreaElement | null>(null);

// --- Computed ---
// isEditing is true when the user is actively typing or the system is processing their input.
const isEditing = computed(() => status.value === 'editing' || status.value === 'saving');
const isRemoteEditing = computed(() => lockStore.isLocked(props.rowIndex, props.colIndex));

const statusClass = computed(() => ({
  'is-saving': status.value === 'saving',
  'is-error': status.value === 'error' || status.value === 'conflict',
  'is-remote-editing': isRemoteEditing.value && !isEditing.value,
}));

const conflictTooltipText = "保存失败: 在您编辑时，此单元格已被他人修改。请按 ESC 键刷新。";

// --- Watchers ---
watch(
  () => props.cellData,
  (newData) => {
    const newContent = newData?.content || '';
    if (status.value === 'saving' && newContent === editableContent.value) {
      status.value = 'idle';
    }
    // If we are in idle, or a conflict/error state, update the local content to the latest from the store.
    // This also allows re-editing from conflict/error states.
    if (status.value === 'idle' || status.value === 'error' || status.value === 'conflict') {
      editableContent.value = newContent;
      if (status.value === 'conflict') {
        // If we were in conflict, and new data arrives, it means we've been updated.
        // The user can now try editing again, so we reset to idle after a short delay.
        setTimeout(() => {
          if (status.value === 'conflict') status.value = 'idle';
        }, 2000);
      }
    }
  },
  { deep: true, immediate: true }
);

// --- Methods ---
const notifyEditingStatus = (isEditing: boolean) => {
  sendMessage({ type: isEditing ? 'USER_IS_EDITING' : 'USER_STOPPED_EDITING', payload: { rowIndex: props.rowIndex, colIndex: props.colIndex } });
};

const startEditing = async () => {
  // Allow re-entry from idle, error, or conflict states.
  // Prevent re-entry if already actively editing or saving.
  if (status.value === 'editing' || status.value === 'saving') return;
  
  baseVersionId.value = props.cellData?.id || null;
  editableContent.value = props.cellData?.content || '';
  status.value = 'editing'; // Set status to editing when entering
  notifyEditingStatus(true);

  await nextTick();
  textareaRef.value?.focus();
  textareaRef.value?.select();
};

const handleSave = () => {
  if (status.value !== 'editing' && status.value !== 'conflict') return;

  // If trying to save from a conflict state, re-capture the latest base version ID
  if (status.value === 'conflict') {
    baseVersionId.value = props.cellData?.id || null;
  }

  notifyEditingStatus(false);
  const originalContent = props.cellData?.content || '';
  const newContent = editableContent.value;

  if (newContent === originalContent) {
    status.value = 'idle';
    return;
  }

  status.value = 'saving';
  emit('update-cell', 
    { content: newContent, baseVersionId: baseVersionId.value }, 
    {
      onError: (type) => {
        status.value = type === 'conflict' ? 'conflict' : 'error';
      },
    }
  );
};

const cancelEditing = () => {
  // Allow canceling from editing, saving, error, or conflict states.
  if (status.value === 'idle') return;
  notifyEditingStatus(false);
  status.value = 'idle';
  editableContent.value = props.cellData?.content || '';
};

</script>

<style scoped>
.grid-cell {
  position: absolute;
  box-sizing: border-box;
  border: 2px solid transparent; /* Add transparent border to prevent layout shift */
  border-right-color: #e0e0e0;
  border-bottom-color: #e0e0e0;
  background-color: #ffffff;
  transition: background-color 0.2s ease, border-color 0.3s ease;
  overflow: hidden;
}

.grid-cell:hover {
  background-color: #f5f5f5;
}

.cell-content {
  padding: 8px;
  font-size: 14px;
  white-space: pre-wrap;
  word-break: break-word;
  width: 100%;
  height: 100%;
  box-sizing: border-box;
}

.cell-editor {
  width: 100%;
  height: 100%;
  padding: 8px;
  margin: 0;
  border: none; /* Editor border is now handled by the parent .grid-cell */
  outline: none;
  font-family: inherit;
  font-size: 14px;
  resize: none;
  box-sizing: border-box;
  background-color: #f0f8ff;
}

/* The border of the parent .grid-cell now indicates the state */
.is-saving {
  border-color: #f39c12;
  animation: breathing-border 1.5s infinite ease-in-out;
}

.is-error,
.is-conflict {
  border-color: #e74c3c;
  animation: none;
}

/* Style for cells being edited by other users */
.is-remote-editing {
  border-color: #3498db; /* Blue border as a soft indicator */
}

.conflict-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  display: flex;
  justify-content: flex-end;
  align-items: flex-start;
  padding: 4px;
}

.conflict-icon {
  color: #e74c3c;
  font-weight: bold;
  font-size: 16px;
  cursor: help;
  pointer-events: auto;
}

@keyframes breathing-border {
  0%, 100% {
    box-shadow: 0 0 5px 0 rgba(243, 156, 18, 0.5);
  }
  50% {
    box-shadow: 0 0 12px 3px rgba(243, 156, 18, 0.9);
  }
}
</style>