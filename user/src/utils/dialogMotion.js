import { ref } from 'vue'

const DEFAULT_STYLE = Object.freeze({
  '--dialog-shift-x': '0px',
  '--dialog-shift-y': '12px',
  '--dialog-start-scale': '0.94',
  '--dialog-start-radius': '18px',
})

export const useDialogMotionOrigin = () => {
  const dialogMotionStyle = ref({ ...DEFAULT_STYLE })

  const captureDialogOrigin = (event) => {
    const trigger = event?.currentTarget
    if (!(trigger instanceof Element)) {
      dialogMotionStyle.value = { ...DEFAULT_STYLE }
      return
    }

    const rect = trigger.getBoundingClientRect()
    const triggerX = rect.left + rect.width / 2
    const triggerY = rect.top + rect.height / 2
    const viewportX = window.innerWidth / 2
    const viewportY = window.innerHeight / 2
    const startScale = Math.min(0.28, Math.max(0.1, rect.width / Math.max(window.innerWidth, 1)))

    dialogMotionStyle.value = {
      '--dialog-shift-x': `${Math.round(triggerX - viewportX)}px`,
      '--dialog-shift-y': `${Math.round(triggerY - viewportY)}px`,
      '--dialog-start-scale': startScale.toFixed(3),
      '--dialog-start-radius': `${Math.round(Math.max(rect.width, rect.height) / 2)}px`,
    }
  }

  const resetDialogOrigin = () => {
    dialogMotionStyle.value = { ...DEFAULT_STYLE }
  }

  return {
    dialogMotionStyle,
    captureDialogOrigin,
    resetDialogOrigin,
  }
}
