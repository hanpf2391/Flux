
import { ref, reactive, onMounted, onUnmounted, shallowRef, type Ref } from 'vue';

// --- Constants and Types ---

export interface Viewport {
  x: number;
  y: number;
  zoom: number;
}

type InteractionState = 'IDLE' | 'PANNING';

const EDGE_SCROLL_AREA_PX = 50;
const MIN_ZOOM = 0.1;
const MAX_ZOOM = 4.0;
const BASE_PAN_SPEED = 7.5; // Halved from previous 15, as requested

// --- Composable ---

export function useCanvas(canvasRef: Ref<HTMLElement | null>) {
  const viewport = reactive<Viewport>({ x: 0, y: 0, zoom: 1 });
  const isPanningMode = shallowRef(false); // Is Ctrl/Meta key held down?
  const interactionState = shallowRef<InteractionState>('IDLE');
  const animationFrameId = ref<number | null>(null);

  // --- State for Interaction Logic ---
  const lastPointerPosition = { x: 0, y: 0 };
  const currentMousePosition = { x: 0, y: 0 };

  // --- Event Handlers ---

  const onPointerDown = (event: PointerEvent) => {
    // Ignore touch events on mobile devices - let touch handlers handle them
    if (event.pointerType === 'touch') return;
    
    // Only pan with left-click when in panning mode
    if (event.button !== 0 || !isPanningMode.value) return;

    // Prevent default browser actions like text selection and stop the event from bubbling to the grid cells
    event.preventDefault();
    event.stopPropagation();

    lastPointerPosition.x = event.clientX;
    lastPointerPosition.y = event.clientY;

    interactionState.value = 'PANNING';
    if (canvasRef.value) {
      canvasRef.value.style.cursor = 'grabbing'; // Change to grabbing cursor
      canvasRef.value.setPointerCapture(event.pointerId);
    }
  };

  const onPointerMove = (event: PointerEvent) => {
    // Ignore touch events on mobile devices - let touch handlers handle them
    if (event.pointerType === 'touch') return;
    
    currentMousePosition.x = event.clientX;
    currentMousePosition.y = event.clientY;

    if (interactionState.value !== 'PANNING') return;

    // Pan the viewport based on mouse movement
    const dx = event.clientX - lastPointerPosition.x;
    const dy = event.clientY - lastPointerPosition.y;

    viewport.x += dx;
    viewport.y += dy;

    lastPointerPosition.x = event.clientX;
    lastPointerPosition.y = event.clientY;
  };

  const onPointerUp = (event: PointerEvent) => {
    // Ignore touch events on mobile devices - let touch handlers handle them
    if (event.pointerType === 'touch') return;
    
    if (interactionState.value !== 'PANNING') return;

    interactionState.value = 'IDLE';
    if (canvasRef.value) {
      // If still in panning mode (Ctrl is held), set cursor to 'grab', otherwise 'default'
      canvasRef.value.style.cursor = isPanningMode.value ? 'grab' : 'default';
      canvasRef.value.releasePointerCapture(event.pointerId);
    }
  };

  const onWheel = (event: WheelEvent) => {
    event.preventDefault();
    const rect = canvasRef.value?.getBoundingClientRect();
    if (!rect) return;

    const mouseX = event.clientX - rect.left;
    const mouseY = event.clientY - rect.top;

    // --- Zoom logic: zoom towards the mouse cursor ---
    const worldXBefore = (mouseX - viewport.x) / viewport.zoom;
    const worldYBefore = (mouseY - viewport.y) / viewport.zoom;

    const scaleAmount = event.deltaY < 0 ? 1.1 : 1 / 1.1;
    const newZoom = viewport.zoom * scaleAmount;
    viewport.zoom = Math.max(MIN_ZOOM, Math.min(newZoom, MAX_ZOOM));

    // Adjust viewport to keep the point under the cursor stationary
    viewport.x = mouseX - worldXBefore * viewport.zoom;
    viewport.y = mouseY - worldYBefore * viewport.zoom;
  };

  // --- Keyboard Handlers for Panning Mode ---

  const onKeyDown = (event: KeyboardEvent) => {
    // FR-1.1: Enter panning mode with Ctrl or Meta key
    if ((event.ctrlKey || event.metaKey) && !isPanningMode.value) {
      isPanningMode.value = true;
      if (canvasRef.value && interactionState.value !== 'PANNING') {
        canvasRef.value.style.cursor = 'grab'; // FR-1.2: Set grab cursor
      }
    }
  };

  const onKeyUp = (event: KeyboardEvent) => {
    // Check if the released key is Ctrl or Meta
    if (!event.ctrlKey && !event.metaKey && isPanningMode.value) {
      isPanningMode.value = false;
      if (canvasRef.value && interactionState.value !== 'PANNING') {
        canvasRef.value.style.cursor = 'default'; // FR-1.2: Revert to default cursor
      }
    }
  };

  // --- Edge Scrolling Logic ---

  const edgeScrollLoop = () => {
    // FR-1.4: Edge scroll only active in panning mode and when not actively dragging
    if (!canvasRef.value || !isPanningMode.value || interactionState.value === 'PANNING') {
      animationFrameId.value = requestAnimationFrame(edgeScrollLoop);
      return;
    }

    const rect = canvasRef.value.getBoundingClientRect();
    let dx = 0;
    let dy = 0;
    // FR-3.2: Panning speed is sensitive to zoom level
    const speed = BASE_PAN_SPEED / viewport.zoom;

    // Check horizontal edges
    if (currentMousePosition.x < rect.left + EDGE_SCROLL_AREA_PX) {
      const speedFactor = (rect.left + EDGE_SCROLL_AREA_PX - currentMousePosition.x) / EDGE_SCROLL_AREA_PX;
      dx = speed * speedFactor;
    } else if (currentMousePosition.x > rect.right - EDGE_SCROLL_AREA_PX) {
      const speedFactor = (currentMousePosition.x - (rect.right - EDGE_SCROLL_AREA_PX)) / EDGE_SCROLL_AREA_PX;
      dx = -speed * speedFactor;
    }

    // Check vertical edges
    if (currentMousePosition.y < rect.top + EDGE_SCROLL_AREA_PX) {
      const speedFactor = (rect.top + EDGE_SCROLL_AREA_PX - currentMousePosition.y) / EDGE_SCROLL_AREA_PX;
      dy = speed * speedFactor;
    } else if (currentMousePosition.y > rect.bottom - EDGE_SCROLL_AREA_PX) {
      const speedFactor = (currentMousePosition.y - (rect.bottom - EDGE_SCROLL_AREA_PX)) / EDGE_SCROLL_AREA_PX;
      dy = -speed * speedFactor;
    }

    if (dx !== 0 || dy !== 0) {
      // Apply the calculated pan. The speed is already adjusted for zoom.
      viewport.x += dx;
      viewport.y += dy;
    }

    animationFrameId.value = requestAnimationFrame(edgeScrollLoop);
  };

  // --- Lifecycle Hooks ---

  onMounted(() => {
    const canvas = canvasRef.value;
    if (!canvas) return;

    // Center the initial view
    viewport.x = canvas.clientWidth / 2;
    viewport.y = canvas.clientHeight / 2;

    // Add all event listeners
    canvas.addEventListener('pointerdown', onPointerDown);
    canvas.addEventListener('pointermove', onPointerMove);
    canvas.addEventListener('pointerup', onPointerUp);
    canvas.addEventListener('wheel', onWheel, { passive: false }); // `passive: false` to allow preventDefault
    window.addEventListener('keydown', onKeyDown);
    window.addEventListener('keyup', onKeyUp);

    // Start the edge scroll loop
    animationFrameId.value = requestAnimationFrame(edgeScrollLoop);
  });

  onUnmounted(() => {
    const canvas = canvasRef.value;
    if (canvas) {
      canvas.removeEventListener('pointerdown', onPointerDown);
      canvas.removeEventListener('pointermove', onPointerMove);
      canvas.removeEventListener('pointerup', onPointerUp);
      canvas.removeEventListener('wheel', onWheel);
    }
    window.removeEventListener('keydown', onKeyDown);
    window.removeEventListener('keyup', onKeyUp);

    if (animationFrameId.value) {
      cancelAnimationFrame(animationFrameId.value);
    }
  });

  return { viewport, isPanningMode };
}
