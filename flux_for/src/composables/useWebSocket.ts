import { onMounted, onUnmounted, shallowRef } from 'vue';
import { useFluxStore } from '../store/fluxStore';
import { useLockStore } from '../store/lockStore';
import type { MessageNodeDTO } from '../types';

// --- WebSocket Singleton Instance ---
// This ensures that there is only one WebSocket connection per application instance.
let socket: WebSocket | null = null;
const isConnected = shallowRef(false);

// --- Message Types ---
interface InboundMessage {
  type: 'CELL_UPDATED' | 'CELL_DELETED' | 'USER_IS_EDITING' | 'USER_STOPPED_EDITING' | 'ONLINE_COUNT_UPDATED' | 'SYSTEM_STATS_UPDATED';
  payload: any;
}

interface OutboundMessage {
  type: 'USER_IS_EDITING' | 'USER_STOPPED_EDITING';
  payload: any;
}

/**
 * Composable for managing the WebSocket connection, sending messages, and handling incoming events.
 */
export function useWebSocket() {
  const fluxStore = useFluxStore();
  const lockStore = useLockStore();

  const connect = () => {
    // Avoid creating duplicate connections
    if (socket && socket.readyState === WebSocket.OPEN) {
      isConnected.value = true;
      return;
    }
    if (socket && socket.readyState === WebSocket.CONNECTING) {
      return;
    }

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = window.location.host;
    const socketURL = `${protocol}//${host}/ws/flux`;

    socket = new WebSocket(socketURL);

    socket.onopen = () => {
      isConnected.value = true;
    };

    socket.onmessage = (event) => {
      try {
        const message: InboundMessage = JSON.parse(event.data);
        const payload = message.payload;

        switch (message.type) {
          case 'CELL_UPDATED':
            fluxStore.updateCell(payload as MessageNodeDTO);
            break;
          case 'CELL_DELETED':
            fluxStore.deleteCell(payload.rowIndex, payload.colIndex);
            break;
          case 'USER_IS_EDITING':
            lockStore.addLock(payload.rowIndex, payload.colIndex);
            break;
          case 'USER_STOPPED_EDITING':
            lockStore.removeLock(payload.rowIndex, payload.colIndex);
            break;
          case 'ONLINE_COUNT_UPDATED':
            fluxStore.setOnlineUsers(payload as number);
            break;
          case 'SYSTEM_STATS_UPDATED':
            fluxStore.updateSystemStats(payload);
            break;
          default:
            console.warn('Received unknown WebSocket message type:', message.type);
        }
      } catch (error) {
        console.error('Failed to parse WebSocket message:', error);
      }
    };

    socket.onclose = (event) => {
      isConnected.value = false;
      if (event.wasClean) {
        console.log(`WebSocket connection closed cleanly, code=${event.code}`);
      } else {
        console.warn('WebSocket connection died. Attempting to reconnect in 5 seconds...');
        setTimeout(connect, 5000);
      }
    };

    socket.onerror = (error) => {
      console.error('WebSocket error:', error);
      socket?.close();
    };
  };

  const sendMessage = (message: OutboundMessage) => {
    if (socket && isConnected.value) {
      socket.send(JSON.stringify(message));
    } else {
      console.error('Cannot send message: WebSocket is not connected.');
    }
  };

  // --- Lifecycle Hooks ---
  // The connection is managed here and will persist for the life of the app.
  onMounted(() => {
    if (!socket) { // Only call connect on the first component mount
      connect();
    }
  });

  // The disconnect logic can be handled by the browser or when the app is fully closed.
  // onUnmounted is not used here to prevent disconnection when navigating between views.

  return { isConnected, sendMessage };
}
