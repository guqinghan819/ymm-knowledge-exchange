<script setup>
defineProps({
  name: { type: String, required: true },
  size: { type: [Number, String], default: '1em' },
})

const paths = {
  home: ['m3 10 9-7 9 7', 'M5 9v11h5v-6h4v6h5V9'],
  grid: ['M3 3h7v7H3zM14 3h7v7h-7zM3 14h7v7H3zM14 14h7v7h-7z'],
  plus: ['M12 5v14M5 12h14'],
  close: ['m6 6 12 12M18 6 6 18'],
  chevron: ['m9 5 7 7-7 7'],
  arrow: ['M4 12h16m-6-6 6 6-6 6'],
  up: ['M12 20V4m-6 6 6-6 6 6'],
  down: ['M12 4v16m-6-6 6 6 6-6'],
  logout: ['M9 4H4v16h5M10 12h11m-5-5 5 5-5 5'],
  image: ['M4 3h16a1 1 0 0 1 1 1v16a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1Z', 'm3 16 5-5 5 5 3-3 5 5', 'M15.5 7h.01'],
  video: ['M4 5h11a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1Z', 'm16 9 5-3v12l-5-3'],
  play: ['m9 5 11 7-11 7V5Z'],
  note: ['M5 3h10l4 4v14H5V3Z', 'M14 3v5h5M9 12h6M9 16h6'],
  edit: ['m14 5 5 5M4 20l5-1L21 7a2.1 2.1 0 0 0-4-4L5 15l-1 5Z'],
  users: ['M16 21v-2a5 5 0 0 0-5-5H8a5 5 0 0 0-5 5v2', 'M9.5 3a4 4 0 1 0 0 8 4 4 0 0 0 0-8Z', 'M17 4a4 4 0 0 1 0 7M21 21v-2a5 5 0 0 0-3-4.6'],
  user: ['M20 21v-2a7 7 0 0 0-14 0v2', 'M13 3a4 4 0 1 0 0 8 4 4 0 0 0 0-8Z'],
  chart: ['M4 3v17h17M8 15v-4M13 15V7M18 15v-6'],
  heart: ['M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1.1-1.1a5.5 5.5 0 0 0-7.8 7.8L12 21l8.8-8.6a5.5 5.5 0 0 0 0-7.8Z'],
  bookmark: ['M6 3h12v18l-6-4-6 4V3Z'],
  star: ['m12 3 2.8 5.7 6.2.9-4.5 4.4 1.1 6.2-5.6-3-5.6 3 1.1-6.2L3 9.6l6.2-.9L12 3Z'],
  search: ['M10.5 3a7.5 7.5 0 1 0 0 15 7.5 7.5 0 0 0 0-15Z', 'm16 16 5 5'],
  upload: ['M12 16V3m-5 5 5-5 5 5M4 15v5h16v-5'],
  cloud: ['M7 17H6a5 5 0 0 1-.7-10A7 7 0 0 1 19 8a4.5 4.5 0 0 1-.5 9H17', 'M12 21V11m-3 3 3-3 3 3'],
  refresh: ['M20 7a9 9 0 0 0-15-1L3 9m0-5v5h5M4 17a9 9 0 0 0 15 1l2-3m0 5v-5h-5'],
  shield: ['m12 3 8 3v6c0 4.5-8 9-8 9s-8-4.5-8-9V6l8-3Z', 'm8 12 3 3 5-6'],
  bulb: ['M9 18h6M10 21h4M8 14a6 6 0 1 1 8 0l-1 2H9l-1-2Z'],
  book: ['M12 5v16M3 3c4 0 7 0 9 2 2-2 5-2 9-2v16c-4 0-7 0-9 2-2-2-5-2-9-2V3Z'],
  spark: ['m12 3 2.5 6.5L21 12l-6.5 2.5L12 21l-2.5-6.5L3 12l6.5-2.5L12 3Z'],
  check: ['m5 12 4 4L19 6'],
  male: ['M10 9a6 6 0 1 0 0 12 6 6 0 0 0 0-12Z', 'm14 10 7-7m-6 0h6v6'],
  female: ['M12 2a6 6 0 1 0 0 12 6 6 0 0 0 0-12Z', 'M12 14v8m-4-4h8'],
}
</script>

<template>
  <svg class="app-icon" :width="size" :height="size" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false">
    <path v-for="(path, index) in (paths[name] || paths.note)" :key="index" :d="path" />
  </svg>
</template>

<style scoped>
.app-icon { display: inline-block; flex-shrink: 0; vertical-align: -0.15em; }
</style>
