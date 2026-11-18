<template>
  <div class="h-[60px] flex items-center justify-center fixed-footer" :class="{ 'collapsed': menuStore.isCollapse }">
    <p>雨绵绵知识管理台 · 内容因整理而清晰</p>
  </div>
</template>

<script setup>
import { useMenuStore } from '@/stores/menu';
const menuStore = useMenuStore();
</script>

<style scoped>
.fixed-footer {
  position: fixed;
  bottom: 0;
  right: 0;
  left: 250px;
  z-index: 1000;
  transition: left 0.3s;
  overflow: hidden; /* 防止页脚出现滚动条 */
  border-top: 1px solid var(--ymm-line);
  color: var(--ymm-muted);
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(14px);
  font-size: 12px;
}

.fixed-footer.collapsed {
  left: 64px;
}
</style>
