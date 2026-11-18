<template>
  <svg class="ai-knowledge-mark" viewBox="0 0 64 64" aria-hidden="true">
    <path class="mark-drop" d="M32 4S11 25.5 11 41c0 11.6 9.4 19 21 19s21-7.4 21-19C53 25.5 32 4 32 4Z" />
    <path class="mark-route" d="m22 33 10-9 10 9-10 10-10-10Z" />
    <path class="mark-route" d="M32 24v25m-5-5 5 5 5-5" />
    <circle class="mark-node" cx="22" cy="33" r="3" />
    <circle class="mark-node mark-node-core" cx="32" cy="24" r="3.5" />
    <circle class="mark-node" cx="42" cy="33" r="3" />
  </svg>
</template>

<style scoped>
.ai-knowledge-mark {
  width: 100%;
  height: 100%;
  display: block;
  overflow: visible;
}

.mark-drop,
.mark-route {
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.mark-drop {
  stroke-width: 2.2;
}

.mark-route {
  stroke-width: 1.7;
  opacity: 0.86;
}

.mark-node {
  fill: var(--ai-mark-node, currentColor);
  stroke: var(--ai-mark-node-stroke, currentColor);
  stroke-width: 1.4;
}

.mark-node-core {
  fill: var(--ai-mark-core, currentColor);
}
</style>
