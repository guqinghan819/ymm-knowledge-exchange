import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useMenuStore = defineStore('menu', () => {
  // 左边栏菜单默认宽度
  const menuWidth = ref("250px")
  
  // 计算属性判断是否折叠
  const isCollapse = computed(() => menuWidth.value === '64px')
  
  // 展开或伸缩左边栏菜单
  function handleMenuWidth() {
      menuWidth.value = menuWidth.value == '250px' ? '64px' : '250px'
  }
  
  return { menuWidth, isCollapse, handleMenuWidth }
}, 
{
  // 开启持久化
  persist: true,
})