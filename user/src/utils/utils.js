import 'element-plus/es/components/message/style/css'
import nprogress from "nprogress"
import { ElMessage, ElMessageBox } from 'element-plus'
// 消息提示
export function showMessage(message = '提示内容', type = 'success', customClass = '') {
    return ElMessage({
        type: type,
        message,
        customClass,
    })
}



// 显示页面加载 Loading
export function showPageLoading() {
    nprogress.start()
}

// 隐藏页面加载 Loading
export function hidePageLoading() {
    nprogress.done()
}

// 给相对路径的文件URL加上 /api 前缀，使其通过 Vite 代理访问后端
export function getFileUrl(url) {
  if (!url) return ''
  // 去掉后端可能包裹在 URL 两侧的方括号
  const cleaned = url.replace(/^\[|\]$/g, '')
  if (cleaned.startsWith('http://') || cleaned.startsWith('https://') || cleaned.startsWith('blob:') || cleaned.startsWith('data:')) {
    return cleaned
  }
  return '/api' + (cleaned.startsWith('/') ? cleaned : '/' + cleaned)
}

// 弹出确认框
export function showModel(content = '提示内容', type = 'warning', title = '') {
    return ElMessageBox.confirm(
        content,
        title,
        {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type,
        }
    )
}

