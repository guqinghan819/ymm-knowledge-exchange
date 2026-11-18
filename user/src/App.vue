<script setup>
import { RouterView } from 'vue-router'
</script>

<template>
  <RouterView />
</template>

<style>
:root {
  --ymm-primary: #2859a6;
  --ymm-primary-strong: #1f4788;
  --ymm-primary-soft: #eaf1fb;
  --ymm-ink: #17233b;
  --ymm-text: #354158;
  --ymm-muted: #7b879b;
  --ymm-line: #dce4f0;
  --ymm-line-soft: #e9eef5;
  --ymm-paper: #ffffff;
  --ymm-canvas: #f4f7fb;
  --ymm-danger: #d94f5c;
  --ymm-success: #2f8164;
  --ymm-radius-sm: 8px;
  --ymm-radius-md: 12px;
  --ymm-radius-lg: 18px;
  --ymm-shadow-float: 0 18px 50px rgba(24, 46, 82, 0.16);
  --ymm-shadow-soft: 0 8px 24px rgba(24, 46, 82, 0.08);
  --ymm-font-sans: "PingFang SC", "Microsoft YaHei", "Noto Sans CJK SC", sans-serif;
  --ymm-font-serif: "Songti SC", "STSong", "Noto Serif CJK SC", serif;

  --el-color-primary: var(--ymm-primary);
  --el-color-primary-light-3: #5d7fbc;
  --el-color-primary-light-5: #8fa7d0;
  --el-color-primary-light-7: #bdcae2;
  --el-color-primary-light-8: #d7e0ee;
  --el-color-primary-light-9: var(--ymm-primary-soft);
  --el-color-primary-dark-2: var(--ymm-primary-strong);
  --el-border-radius-base: var(--ymm-radius-sm);
  --el-border-color: var(--ymm-line);
  --el-text-color-primary: var(--ymm-ink);
  --el-text-color-regular: var(--ymm-text);
  --el-bg-color-page: var(--ymm-canvas);
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  min-height: 100%;
  width: 100%;
  background: var(--ymm-canvas);
  color: var(--ymm-text);
  font-family: var(--ymm-font-sans);
  font-size: 15px;
  line-height: 1.6;
  text-rendering: optimizeLegibility;
  -webkit-font-smoothing: antialiased;
}

#app {
  min-height: 100vh;
  width: 100%;
}

button,
input,
textarea,
select {
  font: inherit;
}

button,
a {
  -webkit-tap-highlight-color: transparent;
}

button:focus-visible,
a:focus-visible,
input:focus-visible,
textarea:focus-visible,
select:focus-visible {
  outline: 3px solid rgba(40, 89, 166, 0.22);
  outline-offset: 2px;
}

::selection {
  color: #fff;
  background: var(--ymm-primary);
}

::-webkit-scrollbar {
  width: 10px;
  height: 10px;
}

::-webkit-scrollbar-thumb {
  border: 3px solid transparent;
  border-radius: 999px;
  background: #bcc8d9;
  background-clip: padding-box;
}

::-webkit-scrollbar-track {
  background: transparent;
}

/* Shared dialog and popover motion. Vue keeps leaving nodes mounted until these finish. */
.dialog-motion-enter-active,
.dialog-motion-leave-active {
  transition: opacity 220ms ease, background-color 220ms ease;
}

.dialog-motion-enter-active .dialog-panel {
  transition:
    opacity 240ms ease,
    transform 420ms cubic-bezier(0.16, 1, 0.3, 1),
    border-radius 420ms cubic-bezier(0.16, 1, 0.3, 1);
}

.dialog-motion-leave-active .dialog-panel {
  transition:
    opacity 210ms ease,
    transform 300ms cubic-bezier(0.4, 0, 0.2, 1),
    border-radius 300ms cubic-bezier(0.4, 0, 0.2, 1);
}

.dialog-motion-enter-from,
.dialog-motion-leave-to {
  opacity: 0;
}

.dialog-motion-enter-from .dialog-panel {
  opacity: 0;
  border-radius: var(--dialog-start-radius, 18px);
  transform:
    translate3d(var(--dialog-shift-x, 0px), var(--dialog-shift-y, 12px), 0)
    scale(var(--dialog-start-scale, 0.94));
}

.dialog-motion-leave-to .dialog-panel {
  opacity: 0;
  border-radius: var(--dialog-start-radius, 18px);
  transform:
    translate3d(var(--dialog-shift-x, 0px), var(--dialog-shift-y, 12px), 0)
    scale(var(--dialog-start-scale, 0.94));
}

.dialog-panel {
  transform-origin: center center;
  backface-visibility: hidden;
  will-change: transform, opacity;
}

.popover-motion-enter-active {
  transition: opacity 160ms ease, transform 220ms cubic-bezier(0.22, 1, 0.36, 1);
}

.popover-motion-leave-active {
  transition: opacity 120ms ease, transform 140ms ease-in;
}

.popover-motion-enter-from,
.popover-motion-leave-to {
  opacity: 0;
  transform: translate3d(0, 6px, 0) scale(0.98);
}

@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    scroll-behavior: auto !important;
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}
</style>
