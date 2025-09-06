<template>
  <div class="message-list" ref="listEl">
    <MessageItem
      v-for="message in messages"
      :key="message.id"
      :message="message"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue';
import type { MessageDTO } from '../api/message';
import MessageItem from './MessageItem.vue';

const props = defineProps<{
  messages: MessageDTO[];
}>();

const listEl = ref<HTMLDivElement | null>(null);

const scrollToBottom = async () => {
  await nextTick();
  if (listEl.value) {
    listEl.value.scrollTop = listEl.value.scrollHeight;
  }
};

watch(() => props.messages, scrollToBottom, {
  deep: true,
  flush: 'post' // Ensures the callback runs after DOM updates
});

</script>

<style scoped>
.message-list {
  flex-grow: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 15px; /* Add space between message items */
}
</style>
