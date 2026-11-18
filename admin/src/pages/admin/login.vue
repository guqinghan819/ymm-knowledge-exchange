<template>
    <div class="admin-login grid grid-cols-2 h-screen">
        <div class="login-brand-panel col-span-2 order-2 p-10 md:col-span-1 md:order-1">
    <!-- 指定为 flex 布局，并设置为屏幕垂直水平居中，高度为 100% -->
    <div class="login-brand-content flex justify-center items-center h-full flex-col">
        <p class="brand-kicker">知识交流社区 · 管理端</p>
        <h2>让每一份知识，<br />都被妥善管理。</h2>
        <p class="brand-description">管理内容、用户与权限，让真实经验有序流动，让每一次分享都值得信赖。</p>
        <div class="platform-mark" role="img" aria-label="雨滴、开放书页与知识连接节点组成的雨绵绵平台标志">
            <svg viewBox="0 0 320 320" aria-hidden="true">
                <path class="mark-drop" d="M160 24S72 116 72 187c0 53 39 91 88 91s88-38 88-91C248 116 160 24 160 24Z" />
                <path class="mark-flow" d="M111 132c14-18 31-26 49-26s35 8 49 26" />
                <path class="mark-link" d="M116 132l44 31 44-31M160 106v57" />
                <circle class="mark-node" cx="116" cy="132" r="7" />
                <circle class="mark-node mark-node-main" cx="160" cy="106" r="8" />
                <circle class="mark-node" cx="204" cy="132" r="7" />
                <path class="mark-page mark-page-left" d="M93 166c26-9 48-5 67 12v63c-19-15-41-20-67-11Z" />
                <path class="mark-page mark-page-right" d="M227 166c-26-9-48-5-67 12v63c19-15 41-20 67-11Z" />
                <path class="mark-spine" d="M160 178v63" />
                <path class="mark-page-line" d="M108 185c15-2 27 1 38 8M108 203c15-2 27 1 38 8M212 185c-15-2-27 1-38 8M212 203c-15-2-27 1-38 8" />
            </svg>
            <div class="mark-caption"><span>知识汇聚</span><i></i><span>可信流动</span></div>
        </div>
    </div>
</div>
        <div class="login-form-panel col-span-2 order-1 md:col-span-1 md:order-2">
            <!-- flex-col 用于指定子元素垂直排列 -->
            <div class="login-form-content flex justify-center items-center h-full flex-col">
                <div class="login-heading">
                    <p>雨绵绵管理台</p>
                    <h1>欢迎回来</h1>
                    <span>登录后继续管理社区内容</span>
                </div>
                <!-- 登录方式切换选项卡 -->
                <div class="login-switch-wrap w-5/6 md:w-2/5 mb-4 relative">
                    <div class="login-switch flex p-1 mb-6 relative overflow-hidden">
                        <!-- 滑块 -->
                        <div class="login-switch-indicator absolute top-1 bottom-1 transition-all duration-300 ease-in-out"
                             :class="activeTab === 'password' ? 'left-1 w-[calc(50%-4px)]' : 'left-[calc(50%+3px)] w-[calc(50%-4px)]'">
                        </div>
                        <button 
                            class="flex-1 py-3 px-4 rounded-lg transition-all duration-300 text-sm font-medium z-10 relative"
                            :class="activeTab === 'password' ? 'is-active font-semibold' : ''"
                            @click="activeTab = 'password'"
                        >
                            <i class="fas fa-lock mr-2"></i>密码登录
                        </button>
                        <button 
                            class="flex-1 py-3 px-4 rounded-lg transition-all duration-300 text-sm font-medium z-10 relative"
                            :class="activeTab === 'sms' ? 'is-active font-semibold' : ''"
                            @click="activeTab = 'sms'"
                        >
                            <i class="fas fa-comment-dots mr-2"></i>短信登录
                        </button>
                    </div>
                </div>
                
                <!-- 登录表单 -->
                <transition name="slide-fade" mode="out-in">
                    <!-- 密码登录表单 -->
                    <el-form 
                        v-if="activeTab === 'password'" 
                        class="login-form w-5/6 md:w-2/5"
                        :model="passwordForm" 
                        :rules="passwordRules"
                        key="password-form"
                    >
                        <el-form-item prop="phone">
                            <!-- 输入框组件 -->
                            <el-input 
                                size="large" 
                                placeholder="请输入手机号" 
                                :prefix-icon="Phone" 
                                clearable 
                                v-model="passwordForm.phone"
                            />
                        </el-form-item>
                        <el-form-item prop="password">
                            <!-- 密码框组件 -->
                            <el-input 
                                size="large" 
                                type="password" 
                                placeholder="请输入密码" 
                                :prefix-icon="Lock" 
                                clearable 
                                v-model="passwordForm.password"
                            />
                        </el-form-item>
                        <el-form-item>
                            <!-- 登录按钮，宽度设置为 100% -->
                            <el-button class="w-full" size="large" :loading="loading"  type="primary" @click="handlePasswordLogin">登录</el-button>
                        </el-form-item>
                    </el-form>
                    
                    <!-- 短信登录表单 -->
                    <el-form 
                        v-else 
                        class="login-form w-5/6 md:w-2/5"
                        :model="smsForm" 
                        :rules="smsRules"
                        key="sms-form"
                    >
                        <el-form-item prop="phone">
                            <!-- 手机号输入框 -->
                            <el-input 
                                size="large" 
                                placeholder="请输入手机号" 
                                :prefix-icon="Phone" 
                                clearable 
                                v-model="smsForm.phone"
                            />
                        </el-form-item>
                        <el-form-item prop="smsCode">
                            <!-- 短信验证码输入框 -->
                            <div class="flex gap-2">
                                <el-input 
                                    size="large" 
                                    placeholder="请输入短信验证码" 
                                    :prefix-icon="Message" 
                                    clearable 
                                    v-model="smsForm.smsCode"
                                />
                                <el-button 
                                    size="large" 
                                    :disabled="smsCodeDisabled" 
                                    @click="sendSmsCode"
                                >
                                    {{ smsCodeButtonText }}
                                </el-button>
                            </div>
                        </el-form-item>
                        <el-form-item>
                            <!-- 登录按钮，宽度设置为 100% -->
                            <el-button class="w-full" size="large" :loading="loading" type="primary" @click="handleSmsLogin">登录</el-button>
                        </el-form-item>
                    </el-form>
                </transition>
            </div>
        </div>
    </div>
</template>

<script setup>
// 引入 Element Plus 图标
import { Lock, Phone, Message } from '@element-plus/icons-vue'
import { ref, reactive } from 'vue'
import { onUnmounted } from 'vue'
import { getVerificationCode, login} from '@/api/auth/auth'
import { showMessage } from '@/utils/utils'
import { setToken } from '@/utils/auth'
import { useUserStore } from '@/stores/user'
import router from '@/router'

const userStore = useUserStore()
// 登录方式切换
const activeTab = ref('password')
// 登录按钮加载
const loading = ref(false)
const code = ref('')
const phone = ref('')
// 密码登录表单数据
const passwordForm = reactive({
    phone: '',
    password: ''
})

// 短信登录表单数据
const smsForm = reactive({
    phone: '',
    smsCode: ''
})

// 表单验证规则
const passwordRules = {
    phone: [
        { required: true, message: '请输入手机号', trigger: 'blur' },
        { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
    ],
    password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, message: '密码长度至少6位', trigger: 'blur' }
    ]
}

const smsRules = {
    phone: [
        { required: true, message: '请输入手机号', trigger: 'blur' },
        { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
    ],
    smsCode: [
        { required: true, message: '请输入短信验证码', trigger: 'blur' }
    ]
}

// 短信验证码倒计时相关
const smsCodeDisabled = ref(false)
const smsCodeButtonText = ref('获取验证码')
let timer = null
const countdown = ref(60)
const verificationType = 1
// 发送短信验证码
const sendSmsCode = () => {
    // 表单验证
    if (!smsForm.phone) {
    showMessage('请输入手机号', 'error')
        return
    }
    
    if (!/^1[3-9]\d{9}$/.test(smsForm.phone)) { 
        showMessage('请输入正确的手机号', 'error')
        return
    }
    
    // 发送短信验证码
    //ElMessage.info('正在发送验证码...')
    getVerificationCode(smsForm.phone, verificationType).then(res => {
        if (!res.success) {
            return
        }
        showMessage('验证码已发送')
        // 开始倒计时
        smsCodeDisabled.value = true
        countdown.value = 60 // 1分钟
        smsCodeButtonText.value = `${countdown.value}秒后重新获取`
        
        if (timer) {
            clearInterval(timer)
        }
        
        timer = setInterval(() => {
            countdown.value--
            if (countdown.value > 0) {
                smsCodeButtonText.value = `${countdown.value}秒后重新获取`
            } else {
                clearInterval(timer)
                timer = null
                smsCodeDisabled.value = false
                smsCodeButtonText.value = '获取验证码'
                countdown.value = 60 // 重置倒计时
            }
        }, 1000)
    })
}

// 处理密码登录
const handlePasswordLogin = () => {
    // 这里应该调用实际的登录接口
    if (!passwordForm.phone || !passwordForm.password) {
       showMessage('请填写手机号和密码', 'warning')
        return
    }
    
    if (!/^1[3-9]\d{9}$/.test(passwordForm.phone)) {
        showMessage('请输入正确的手机号', 'warning')
        return
    }
    
    // 调用登录接口
   // 开始加载
        loading.value = true
    
    // 调用登录接口
    login({phone: passwordForm.phone, password: passwordForm.password, loginType: 2}).then(res => {
    if (!res.success) {
      showMessage('密码错误', 'error')
      return
    }    
    //保存token到cookie
    setToken(res.data)
    userStore.clearUserInfo()
    
    // 跳转到管理后台首页
    router.push('/admin')
     showMessage('登录成功')
  }).finally(() => {
            // 结束加载
            loading.value = false
        })

        


}

// 处理短信登录
const handleSmsLogin = () => {
    // 这里应该调用实际的登录接口
    if (!smsForm.phone || !smsForm.smsCode) {
        showMessage('请填写手机号和验证码', 'warning')
        return
    }
    
    if (!/^1[3-9]\d{9}$/.test(smsForm.phone)) {
       showMessage('请输入正确的手机号', 'warning')
        return
    }

     // 开始加载
        loading.value = true
    
    // 调用登录接口
    login({phone: smsForm.phone, code: smsForm.smsCode, loginType: 1}).then(res => {
    if (!res.success) {
      showMessage('验证码错误', 'error')
      return
    }    
    showMessage('登录成功')
    //保存token到cookie
    setToken(res.data)
    //获取登陆用户
    userStore.clearUserInfo()
    
    // 跳转到管理后台首页
    router.push('/admin')
  }).finally(() => {
            // 结束加载
            loading.value = false
        })
   
}

// 组件卸载时清除定时器
onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})
    
    
    

</script>

<style scoped>
.admin-login {
  min-height: 100vh;
  overflow: hidden;
  color: var(--ymm-text);
  background: var(--ymm-canvas);
}

.login-brand-panel {
  position: relative;
  overflow: hidden;
  color: #fff;
  background:
    linear-gradient(rgba(255, 255, 255, 0.045) 1px, transparent 1px),
    #17233b;
  background-size: 100% 38px;
  animation: brand-panel-in 760ms cubic-bezier(0.16, 1, 0.3, 1) both;
  will-change: transform, opacity;
}

.login-brand-panel::after {
  content: '';
  position: absolute;
  right: -18%;
  bottom: -26%;
  width: 62%;
  aspect-ratio: 1;
  border: 1px solid rgba(160, 190, 232, 0.24);
  border-radius: 50%;
  box-shadow: 0 0 0 70px rgba(160, 190, 232, 0.04), 0 0 0 140px rgba(160, 190, 232, 0.025);
}

.login-brand-content {
  position: relative;
  z-index: 1;
  align-items: flex-start;
  width: min(540px, 90%);
  margin: 0 auto;
}

.brand-kicker {
  margin: 0 0 22px;
  color: #9fb6d8;
  font-size: 13px;
  font-weight: 700;
}

.login-brand-content h2 {
  margin: 0;
  font-family: var(--ymm-font-serif);
  font-size: clamp(40px, 4.5vw, 68px);
  font-weight: 700;
  line-height: 1.28;
  letter-spacing: -0.035em;
}

.brand-description {
  max-width: 470px;
  margin: 25px 0 0;
  color: #b8c7db;
  font-size: 14px;
  line-height: 1.9;
}

.platform-mark {
  width: min(310px, 62%);
  margin: 30px auto 0;
  color: #fff;
}

.platform-mark svg {
  width: 100%;
  height: auto;
  display: block;
  overflow: visible;
  filter: drop-shadow(0 18px 30px rgba(5, 15, 34, 0.25));
}

.mark-drop,
.mark-flow,
.mark-link,
.mark-spine,
.mark-page-line {
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.mark-drop {
  stroke: #8fb4eb;
  stroke-width: 2;
  stroke-dasharray: 760;
  animation: mark-draw 1.05s 380ms cubic-bezier(0.16, 1, 0.3, 1) both;
}

.mark-flow {
  stroke: rgba(255, 255, 255, 0.28);
  stroke-width: 1.5;
  stroke-dasharray: 150;
  animation: mark-draw-short 720ms 720ms ease-out both;
}

.mark-link {
  stroke: #9ebfe9;
  stroke-width: 1.5;
  stroke-dasharray: 150;
  animation: mark-draw-short 720ms 650ms ease-out both;
}

.mark-node {
  fill: #17233b;
  stroke: #a9c7ee;
  stroke-width: 2;
  opacity: 0;
  transform-box: fill-box;
  transform-origin: center;
  animation: mark-node-in 460ms 900ms cubic-bezier(0.16, 1, 0.3, 1) both;
}

.mark-node-main {
  fill: #2859a6;
  stroke: #fff;
}

.mark-page {
  fill: rgba(255, 255, 255, 0.96);
  stroke: #fff;
  stroke-width: 1.5;
  opacity: 0;
  transform-box: fill-box;
  transform-origin: bottom center;
  animation: mark-page-in 650ms 560ms cubic-bezier(0.16, 1, 0.3, 1) both;
}

.mark-page-left {
  transform-origin: right bottom;
}

.mark-page-right {
  transform-origin: left bottom;
}

.mark-spine {
  stroke: #8aa9d3;
  stroke-width: 1.4;
  stroke-dasharray: 70;
  animation: mark-draw-short 520ms 850ms ease-out both;
}

.mark-page-line {
  stroke: #91a9c8;
  stroke-width: 1.4;
  stroke-dasharray: 150;
  animation: mark-draw-short 620ms 980ms ease-out both;
}

.mark-caption {
  margin-top: -8px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #9fb6d8;
  font-size: 11px;
  font-weight: 600;
}

.mark-caption i {
  width: 34px;
  height: 1px;
  background: rgba(159, 182, 216, 0.45);
}

.login-form-panel {
  background:
    linear-gradient(rgba(40, 89, 166, 0.035) 1px, transparent 1px),
    #fff;
  background-size: 100% 38px;
  animation: form-panel-in 780ms 70ms cubic-bezier(0.16, 1, 0.3, 1) both;
  will-change: transform, opacity;
}

.login-form-content {
  width: 100%;
}

.login-heading,
.login-switch-wrap,
.login-form {
  width: min(420px, calc(100% - 48px)) !important;
}

.login-heading {
  margin-bottom: 26px;
}

.login-heading p {
  margin: 0 0 8px;
  color: var(--ymm-primary);
  font-size: 13px;
  font-weight: 700;
}

.login-heading h1 {
  margin: 0;
  color: var(--ymm-ink);
  font-family: var(--ymm-font-serif);
  font-size: 38px;
  font-weight: 700;
}

.login-heading span {
  display: block;
  margin-top: 8px;
  color: var(--ymm-muted);
  font-size: 13px;
}

.login-switch {
  border: 1px solid var(--ymm-line);
  border-radius: 10px;
  background: #f4f7fb;
}

.login-switch button {
  color: var(--ymm-muted);
}

.login-switch button.is-active {
  color: var(--ymm-primary);
}

.login-switch-indicator {
  border: 1px solid #d8e2f0;
  border-radius: 7px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(24, 46, 82, 0.08);
}

.login-form :deep(.el-input__wrapper) {
  min-height: 48px;
}

.login-form :deep(.el-button--large) {
  min-height: 48px;
  border-radius: 9px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 滑动切换动画 */
.slide-fade-enter-active {
  transition: opacity 280ms ease, transform 420ms cubic-bezier(0.16, 1, 0.3, 1);
}

.slide-fade-leave-active {
  transition: opacity 180ms ease, transform 240ms ease;
}

.slide-fade-enter-from {
  transform: translateX(20px);
  opacity: 0;
}

.slide-fade-leave-to {
  transform: translateX(-20px);
  opacity: 0;
}

@keyframes brand-panel-in {
  from {
    opacity: 0;
    transform: translate3d(-10vw, 0, 0);
  }
  to {
    opacity: 1;
    transform: translate3d(0, 0, 0);
  }
}

@keyframes form-panel-in {
  from {
    opacity: 0;
    transform: translate3d(10vw, 0, 0);
  }
  to {
    opacity: 1;
    transform: translate3d(0, 0, 0);
  }
}

@keyframes mark-draw {
  from { stroke-dashoffset: 760; }
  to { stroke-dashoffset: 0; }
}

@keyframes mark-draw-short {
  from { stroke-dashoffset: 150; }
  to { stroke-dashoffset: 0; }
}

@keyframes mark-page-in {
  from {
    opacity: 0;
    transform: scaleX(0.72) translateY(10px);
  }
  to {
    opacity: 1;
    transform: scaleX(1) translateY(0);
  }
}

@keyframes mark-node-in {
  from {
    opacity: 0;
    transform: scale(0.4);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

@media (max-width: 767px) {
  .admin-login {
    display: flex;
    flex-direction: column;
    min-height: 100dvh;
  }

  .login-brand-panel {
    order: 1;
    min-height: 220px;
    padding: 30px 24px;
  }

  .login-brand-content {
    justify-content: center;
  }

  .login-brand-content h2 {
    font-size: 34px;
  }

  .brand-kicker {
    margin-bottom: 12px;
  }

  .brand-description,
  .platform-mark {
    display: none;
  }

  .login-form-panel {
    order: 2;
    min-height: calc(100dvh - 220px);
  }

  .login-form-content {
    padding: 42px 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  .login-brand-panel,
  .login-form-panel,
  .mark-drop,
  .mark-flow,
  .mark-link,
  .mark-spine,
  .mark-node,
  .mark-page,
  .mark-page-line {
    animation: none;
  }

  .mark-node,
  .mark-page {
    opacity: 1;
  }
}
</style>
