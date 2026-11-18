<script setup lang="ts">
defineProps<{ name: string }>()
</script>

<template>
  <svg class="ai-mode-icon" viewBox="0 0 24 24" aria-hidden="true">
    <template v-if="name === 'webSearch'">
      <circle cx="10.5" cy="10.5" r="7.5" />
      <path d="M3.5 10.5h14M10.5 3c2.2 2.1 3.2 4.6 3.2 7.5s-1 5.4-3.2 7.5c-2.2-2.1-3.2-4.6-3.2-7.5S8.3 5.1 10.5 3Zm5.5 13 5 5" />
    </template>
    <template v-else-if="name === 'noteRag'">
      <path d="M4 5.5c3.2-.8 5.9-.2 8 1.8v12c-2.1-2-4.8-2.6-8-1.8v-12Zm16 0c-3.2-.8-5.9-.2-8 1.8v12c2.1-2 4.8-2.6 8-1.8v-12Z" />
      <path d="M7 9h2.5M15 9h2" />
    </template>
    <template v-else-if="name === 'ppt'">
      <path d="M4 4h16v12H4zM8 20h8M12 16v4" />
      <path d="m8 12 2.5-3 2 2 3.5-4" />
    </template>
    <template v-else-if="name === 'imageWriting'">
      <rect x="3" y="4" width="14" height="14" rx="2" />
      <circle cx="7.5" cy="8.5" r="1.5" />
      <path d="m5 16 4-4 2.5 2.5 2-2 2 2M16 19l4.5-4.5 1 1L17 20l-2 .5z" />
    </template>
    <template v-else>
      <path d="M5 3h10l4 4v14H5zM15 3v5h4M8 12h6M8 16h4" />
      <path d="m18.5 10 .7 1.5 1.5.7-1.5.7-.7 1.5-.7-1.5-1.5-.7 1.5-.7z" />
    </template>
  </svg>
</template>

<style scoped>
.ai-mode-icon {
  width: 17px;
  height: 17px;
  display: block;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.65;
  stroke-linecap: round;
  stroke-linejoin: round;
}
</style>
