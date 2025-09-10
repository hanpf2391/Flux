<template>
  <div class="color-palette">
    <!-- 预设颜色 -->
    <div class="preset-colors">
      <div 
        v-for="color in presetColors" 
        :key="color"
        class="preset-color"
        :style="{ backgroundColor: color }"
        @click="selectColor(color)"
        :class="{ active: activeColor === color }"
      />
    </div>
    
    <!-- 自定义颜色选择器 -->
    <div class="custom-color-section">
      <div class="color-picker-grid">
        <div 
          v-for="row in colorGrid" 
          :key="row.name"
          class="color-row"
        >
          <div 
            v-for="color in row.colors" 
            :key="color"
            class="grid-color"
            :style="{ backgroundColor: color }"
            @click="selectColor(color)"
            :class="{ active: activeColor === color }"
          />
        </div>
      </div>
      
      <!-- 渐变色带 -->
      <div class="gradient-slider" ref="gradientSlider" @mousedown="startGradientDrag">
        <div class="gradient-track" :style="gradientStyle">
          <div class="gradient-thumb" :style="thumbStyle"></div>
        </div>
      </div>
      
      <!-- 当前颜色显示 -->
      <div class="current-color-section">
        <div class="current-color-display" :style="{ backgroundColor: activeColor }">
          <span class="color-hex" v-if="activeColor">{{ activeColor }}</span>
        </div>
        <input 
          type="text" 
          v-model="colorInput" 
          @input="handleColorInput"
          class="color-input"
          placeholder="#000000"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';

interface Props {
  modelValue?: string;
  visible?: boolean;
}

interface Emits {
  (e: 'update:modelValue', value: string): void;
  (e: 'change', value: string): void;
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '#FFC0CB',
  visible: true
});

const emit = defineEmits<Emits>();

const activeColor = ref(props.modelValue);
const colorInput = ref(props.modelValue);
const isDragging = ref(false);
const gradientSlider = ref<HTMLDivElement>();
const hue = ref(0);
const saturation = ref(50);
const lightness = ref(50);

// 预设颜色
const presetColors = [
  '#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4', '#FFEAA7',
  '#DDA0DD', '#98D8C8', '#F7DC6F', '#BB8FCE', '#85C1E9',
  '#F8C471', '#82E0AA', '#F1948A', '#85C1E9', '#D7BDE2',
  '#A3E4D7', '#FAD7A0', '#D5A6BD', '#AED6F1', '#A9DFBF'
];

// HSL转HEX
const hslToHex = (h: number, s: number, l: number): string => {
  l /= 100;
  const a = s * Math.min(l, 1 - l) / 100;
  const f = (n: number) => {
    const k = (n + h / 30) % 12;
    const color = l - a * Math.max(Math.min(k - 3, 9 - k, 1), -1);
    return Math.round(255 * color).toString(16).padStart(2, '0');
  };
  return `#${f(0)}${f(8)}${f(4)}`;
};

// 颜色网格
const colorGrid = computed(() => {
  const grid = [];
  const lightnessValues = [20, 30, 40, 50, 60, 70, 80];
  
  for (const lightness of lightnessValues) {
    const rowColors = [];
    for (let i = 0; i < 12; i++) {
      const hue = (i * 30);
      rowColors.push(hslToHex(hue, 70, lightness));
    }
    grid.push({ name: `lightness-${lightness}`, colors: rowColors });
  }
  return grid;
});

// 渐变样式
const gradientStyle = computed(() => ({
  background: `linear-gradient(to right, 
    ${hslToHex(0, 100, 50)}, 
    ${hslToHex(60, 100, 50)}, 
    ${hslToHex(120, 100, 50)}, 
    ${hslToHex(180, 100, 50)}, 
    ${hslToHex(240, 100, 50)}, 
    ${hslToHex(300, 100, 50)}, 
    ${hslToHex(360, 100, 50)})`
}));

const thumbStyle = computed(() => ({
  left: `${hue.value}%`,
  backgroundColor: hslToHex(hue.value, 100, 50)
}));

// 初始化颜色解析
const parseInitialColor = (color: string) => {
  // 如果是HEX格式，转换为HSL
  const hexMatch = color.match(/^#([0-9A-F]{6})$/i);
  if (hexMatch) {
    const hex = hexMatch[1];
    const r = parseInt(hex.substr(0, 2), 16) / 255;
    const g = parseInt(hex.substr(2, 2), 16) / 255;
    const b = parseInt(hex.substr(4, 2), 16) / 255;
    
    const max = Math.max(r, g, b);
    const min = Math.min(r, g, b);
    const l = (max + min) / 2;
    
    if (max === min) {
      hue.value = 0;
    } else {
      const d = max - min;
      let h = 0;
      switch (max) {
        case r: h = ((g - b) / d + (g < b ? 6 : 0)) / 6; break;
        case g: h = ((b - r) / d + 2) / 6; break;
        case b: h = ((r - g) / d + 4) / 6; break;
      }
      hue.value = Math.round(h * 360);
    }
  }
};

// 组件挂载时解析初始颜色
onMounted(() => {
  parseInitialColor(props.modelValue);
});

// 选择颜色
const selectColor = (color: string) => {
  activeColor.value = color;
  colorInput.value = color;
  emit('update:modelValue', color);
  emit('change', color);
};

// 处理颜色输入
const handleColorInput = (event: Event) => {
  const input = event.target as HTMLInputElement;
  const value = input.value;
  
  if (/^#[0-9A-F]{6}$/i.test(value)) {
    activeColor.value = value;
    colorInput.value = value;
    parseInitialColor(value); // 更新hue值
    emit('update:modelValue', value);
    emit('change', value);
  }
};

// 开始拖拽渐变条
const startGradientDrag = (event: MouseEvent) => {
  isDragging.value = true;
  updateGradientPosition(event);
};

// 更新渐变位置
const updateGradientPosition = (event: MouseEvent) => {
  if (!isDragging.value || !gradientSlider.value) return;
  
  const rect = gradientSlider.value.getBoundingClientRect();
  const x = event.clientX - rect.left;
  const percentage = Math.max(0, Math.min(100, (x / rect.width) * 100));
  
  hue.value = percentage;
  
  // 生成新颜色（HEX格式）
  const newColor = hslToHex(hue.value, 70, 50);
  activeColor.value = newColor;
  colorInput.value = newColor;
  emit('update:modelValue', newColor);
  emit('change', newColor);
};

// 监听鼠标事件
onMounted(() => {
  document.addEventListener('mousemove', handleMouseMove);
  document.addEventListener('mouseup', handleMouseUp);
});

onUnmounted(() => {
  document.removeEventListener('mousemove', handleMouseMove);
  document.removeEventListener('mouseup', handleMouseUp);
});

const handleMouseMove = (event: MouseEvent) => {
  updateGradientPosition(event);
};

const handleMouseUp = () => {
  isDragging.value = false;
};

// 监听props变化
watch(() => props.modelValue, (newVal) => {
  if (newVal && newVal !== activeColor.value) {
    activeColor.value = newVal;
    colorInput.value = newVal;
    parseInitialColor(newVal);
  }
});

watch(() => props.visible, (newVal) => {
  if (newVal) {
    // 可见时保持当前颜色
  }
});
</script>

<style scoped>
.color-palette {
  background: white;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  min-width: 280px;
  border: 1px solid #e4e7ed;
}

.preset-colors {
  display: grid;
  grid-template-columns: repeat(10, 1fr);
  gap: 4px;
  margin-bottom: 16px;
}

.preset-color {
  width: 20px;
  height: 20px;
  border-radius: 4px;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.2s ease;
}

.preset-color:hover {
  transform: scale(1.1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.preset-color.active {
  border-color: #409EFF;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.color-picker-grid {
  margin-bottom: 16px;
}

.color-row {
  display: flex;
  gap: 4px;
  margin-bottom: 4px;
}

.grid-color {
  width: 20px;
  height: 20px;
  border-radius: 4px;
  cursor: pointer;
  border: 1px solid #f0f0f0;
  transition: all 0.2s ease;
}

.grid-color:hover {
  transform: scale(1.1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.grid-color.active {
  border-color: #409EFF;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.gradient-slider {
  width: 100%;
  height: 30px;
  border-radius: 15px;
  overflow: hidden;
  cursor: pointer;
  margin-bottom: 16px;
  position: relative;
}

.gradient-track {
  width: 100%;
  height: 100%;
  position: relative;
}

.gradient-thumb {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2px solid white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  cursor: grab;
  transition: all 0.2s ease;
}

.gradient-thumb:hover {
  transform: translateY(-50%) scale(1.2);
}

.current-color-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.current-color-display {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  border: 2px solid #e4e7ed;
  position: relative;
  overflow: hidden;
}

.color-hex {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: white;
  font-size: 10px;
  font-weight: bold;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.8);
  pointer-events: none;
}

.color-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  font-size: 14px;
  font-family: 'Courier New', monospace;
}

.color-input:focus {
  outline: none;
  border-color: #409EFF;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}
</style>