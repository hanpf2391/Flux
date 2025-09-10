<template>
  <div class="test-page">
    <h1>Initial Position Test</h1>
    <div class="test-controls">
      <button @click="fetchInitialPosition" :disabled="loading">
        {{ loading ? 'Loading...' : 'Fetch Initial Position' }}
      </button>
      <button @click="clearResult">Clear</button>
    </div>
    
    <div v-if="result" class="result">
      <h3>Result:</h3>
      <pre>{{ JSON.stringify(result, null, 2) }}</pre>
    </div>
    
    <div v-if="error" class="error">
      <h3>Error:</h3>
      <pre>{{ error }}</pre>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { getInitialPosition } from '../api/canvas';
import type { CanvasInitialPositionDTO } from '../types';

const loading = ref(false);
const result = ref<CanvasInitialPositionDTO | null>(null);
const error = ref<string | null>(null);

const fetchInitialPosition = async () => {
  loading.value = true;
  error.value = null;
  result.value = null;
  
  try {
    const position = await getInitialPosition();
    result.value = position;
    console.log('Initial position:', position);
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Unknown error';
    console.error('Failed to fetch initial position:', err);
  } finally {
    loading.value = false;
  }
};

const clearResult = () => {
  result.value = null;
  error.value = null;
};
</script>

<style scoped>
.test-page {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.test-controls {
  margin: 20px 0;
  display: flex;
  gap: 10px;
}

button {
  padding: 10px 20px;
  font-size: 16px;
  cursor: pointer;
  border: 1px solid #ccc;
  border-radius: 4px;
  background: #f0f0f0;
}

button:hover:not(:disabled) {
  background: #e0e0e0;
}

button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.result {
  margin-top: 20px;
  padding: 15px;
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 4px;
}

.error {
  margin-top: 20px;
  padding: 15px;
  background: #f8d7da;
  border: 1px solid #f5c6cb;
  border-radius: 4px;
  color: #721c24;
}

pre {
  white-space: pre-wrap;
  word-wrap: break-word;
}
</style>