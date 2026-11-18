<template>
	<!-- 通过 flex 指定水平布局 -->
    <div class="admin-header">
        <!-- 左边栏收缩、展开 -->
        <div class="header-leading">
          <button class="header-action menu-toggle" type="button" :aria-label="menuStore.isCollapse ? '展开菜单' : '收起菜单'" @click="toggleMenu">
            <el-icon>
                <Fold v-if="!menuStore.isCollapse" />
                <Expand v-else />
            </el-icon>
          </button>
          <div class="header-title">
            <strong>知识管理台</strong>
            <span>内容、用户与权限治理</span>
          </div>
        </div>

        <!-- 右边容器 -->
        <div class="flex items-center">

            <!-- 点击刷新页面 -->
            <el-tooltip class="box-item" effect="dark" content="刷新" placement="bottom">
                <button class="header-action header-utility" type="button" aria-label="刷新页面"
                    @click="handleRefresh">
                    <el-icon>
                        <Refresh />
                    </el-icon>
                </button>
            </el-tooltip>


        <el-tooltip class="box-item" effect="dark" :content="isFullscreen ? '退出全屏' : '全屏'" placement="bottom">
    <!-- 全屏展示 -->
        <button class="header-action header-utility" type="button" :aria-label="isFullscreen ? '退出全屏' : '进入全屏'" @click="toggle">
        <el-icon>
            <FullScreen v-if="!isFullscreen"/>
            <Aim v-else/>
        </el-icon>
        </button>
        </el-tooltip>

            <!-- 登录用户头像 -->
            <el-dropdown>
                <span class="admin-profile">
                    <!-- 头像 Avatar -->
                    <el-avatar class="mr-2" :size="25" :src="userStore.userInfo.avatar" />
                    <span class="admin-profile-name">{{ userStore.userInfo.nickname || '管理员' }}</span>
                    <el-icon class="el-icon--right ml-1">
                        <arrow-down />
                    </el-icon>
                </span>
                <template #dropdown>
                    <el-dropdown-menu>
                        <el-dropdown-item @click="openPasswordDialog">修改密码</el-dropdown-item>
                        <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
                    </el-dropdown-menu>
                </template>
            </el-dropdown>
        </div>
    </div>
    
    <!-- 修改密码对话框 -->
    <el-dialog v-model="passwordDialogVisible" title="修改密码" width="500px" :before-close="handlePasswordDialogClose">
        <el-steps :active="passwordStep" finish-status="success" align-center>
            <el-step title="验证手机号"></el-step>
            <el-step title="设置新密码"></el-step>
        </el-steps>
        
        <!-- 步骤1: 验证手机号 -->
        <div v-if="passwordStep === 1" class="mt-6">
            <el-form :model="phoneForm" :rules="phoneRules" ref="phoneFormRef" label-width="100px">
                <el-form-item label="手机号" prop="phone">
                    <el-input v-model="phoneForm.phone" placeholder="请输入手机号" />
                </el-form-item>
                <el-form-item label="验证码" prop="code">
                    <div class="flex gap-2">
                        <el-input v-model="phoneForm.code" placeholder="请输入验证码" />
                        <el-button 
                            @click="sendVerificationCode" 
                            :disabled="codeDisabled"
                            :loading="codeLoading">
                            {{ codeButtonText }}
                        </el-button>
                    </div>
                </el-form-item>
            </el-form>
        </div>
        
        <!-- 步骤2: 设置新密码 -->
        <div v-else class="mt-6">
            <el-form :model="newPasswordForm" :rules="newPasswordRules" ref="newPasswordFormRef" label-width="100px">
                <el-form-item label="新密码" prop="password">
                    <el-input v-model="newPasswordForm.password" type="password" show-password placeholder="请输入新密码" />
                </el-form-item>
                <el-form-item label="确认密码" prop="confirmPassword">
                    <el-input v-model="newPasswordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
                </el-form-item>
            </el-form>
        </div>
        
        <template #footer>
            <span class="dialog-footer">
                <el-button @click="closePasswordDialog">取消</el-button>
                <el-button v-if="passwordStep === 1" type="primary" @click="nextStep" :loading="passwordSubmitLoading">下一步</el-button>
                <el-button v-else type="primary" @click="submitNewPassword" :loading="passwordSubmitLoading">确认修改</el-button>
            </span>
        </template>
    </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Fold, Expand, FullScreen, ArrowDown, Refresh } from '@element-plus/icons-vue'
import { useMenuStore } from '@/stores/menu';
import { useFullscreen } from '@vueuse/core';
import { useUserStore } from '@/stores/user'
import { logout } from '@/api/auth/auth'
import { getToken, removeToken } from '@/utils/auth'
import { useRouter } from 'vue-router'
import { getVerificationCode, updatePassword } from '@/api/auth/auth'
import { showMessage } from '@/utils/utils'
import 'element-plus/es/components/message-box/style/css'                 
// 引入了用户 Store
const userStore = useUserStore()
const menuStore = useMenuStore();
const router = useRouter();

onMounted(() => {
    // 首次读取资料若遇到短暂网络错误，顶部栏挂载后再尝试一次。
    if (getToken() && !userStore.userInfo.id) {
        userStore.setUserInfo().catch(error => {
            console.error('重试获取登录用户信息失败:', error)
        })
    }
})

// 修改密码对话框相关
const passwordDialogVisible = ref(false)
const passwordSubmitLoading = ref(false)
const passwordStep = ref(1) // 1: 验证手机号, 2: 设置新密码
const phoneFormRef = ref()
const newPasswordFormRef = ref()
const codeLoading = ref(false)
const codeDisabled = ref(false)
const codeButtonText = ref('发送验证码')
let timer = null
const countdown = ref(60)

// 手机号表单数据
const phoneForm = reactive({
    phone: '',
    code: ''
})

// 新密码表单数据
const newPasswordForm = reactive({
    password: '',
    confirmPassword: ''
})

// 手机号表单验证规则
const phoneRules = {
    phone: [
        { required: true, message: '请输入手机号', trigger: 'blur' },
        { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
    ],
    code: [
        { required: true, message: '请输入验证码', trigger: 'blur' }
    ]
}

// 新密码表单验证规则
const newPasswordRules = {
    password: [
        { required: true, message: '请输入新密码', trigger: 'blur' },
        { min: 6, message: '密码长度至少6位', trigger: 'blur' }
    ],
    confirmPassword: [
        { required: true, message: '请再次输入新密码', trigger: 'blur' },
        { validator: validateConfirmPassword, trigger: 'blur' }
    ]
}

// 确认密码验证函数
function validateConfirmPassword(rule, value, callback) {
    if (value === '') {
        callback(new Error('请再次输入新密码'))
    } else if (value !== newPasswordForm.password) {
        callback(new Error('两次输入的密码不一致'))
    } else {
        callback()
    }
}

// 发送验证码
const sendVerificationCode = () => {
    // 验证手机号
    if (!phoneForm.phone) {
        ElMessage.warning('请输入手机号')
        return
    }
    
    if (!/^1[3-9]\d{9}$/.test(phoneForm.phone)) {
        ElMessage.warning('请输入正确的手机号')
        return
    }
    
    codeLoading.value = true
    
    // 调用发送验证码API，类型2表示修改密码
    getVerificationCode(phoneForm.phone, 2).then(res => {
        codeLoading.value = false
        
        if (res.success) {
            showMessage('验证码已发送，请注意查收', 'success')
            
            // 启动倒计时
            codeDisabled.value = true
            countdown.value = 60
            codeButtonText.value = `${countdown.value}秒后重新发送`
            
            if (timer) {
                clearInterval(timer)
            }
            
            timer = setInterval(() => {
                countdown.value--
                if (countdown.value > 0) {
                    codeButtonText.value = `${countdown.value}秒后重新发送`
                } else {
                    clearInterval(timer)
                    timer = null
                    codeDisabled.value = false
                    codeButtonText.value = '发送验证码'
                }
            }, 1000)
        } else {
            showMessage(res.message || '验证码发送失败', 'error')
        }
    }).catch(err => {
        codeLoading.value = false
        showMessage('验证码发送失败，请稍后重试', 'error')
        console.error('发送验证码失败:', err)
    })
}

// 下一步
const nextStep = () => {
    phoneFormRef.value.validate((valid) => {
        if (valid) {
            // 验证通过，进入下一步
            passwordStep.value = 2
        }
    })
}

// 提交新密码
const submitNewPassword = () => {
    newPasswordFormRef.value.validate((valid) => {
        if (valid) {
            passwordSubmitLoading.value = true
            
            // 调用更新密码API
            updatePassword(phoneForm.phone, phoneForm.code, newPasswordForm.password).then(res => {
                if (res.success) {
                    ElMessage.success('密码修改成功')
                    closePasswordDialog()
                } else {
                    ElMessage.error(res.message || '密码修改失败')
                }
            }).catch(err => {
                ElMessage.error('密码修改失败')
            }).finally(() => {
                passwordSubmitLoading.value = false
            })
        }
    })
}

// 打开修改密码对话框
const openPasswordDialog = () => {
    passwordDialogVisible.value = true
    passwordStep.value = 1
}

// 关闭修改密码对话框
const closePasswordDialog = () => {
    passwordDialogVisible.value = false
    // 重置表单
    if (phoneFormRef.value) phoneFormRef.value.resetFields()
    if (newPasswordFormRef.value) newPasswordFormRef.value.resetFields()
    phoneForm.phone = ''
    phoneForm.code = ''
    newPasswordForm.password = ''
    newPasswordForm.confirmPassword = ''
    passwordStep.value = 1
    
    // 清理定时器
    if (timer) {
        clearInterval(timer)
        timer = null
    }
    codeDisabled.value = false
    codeButtonText.value = '发送验证码'
}

// 处理对话框关闭前的操作
const handlePasswordDialogClose = (done) => {
    ElMessageBox.confirm('确认关闭修改密码窗口吗？', '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning',
        })
        .then(() => {
            done()
            // 重置表单
            setTimeout(() => {
                if (phoneFormRef.value) phoneFormRef.value.resetFields()
                if (newPasswordFormRef.value) newPasswordFormRef.value.resetFields()
                phoneForm.phone = ''
                phoneForm.code = ''
                newPasswordForm.password = ''
                newPasswordForm.confirmPassword = ''
                passwordStep.value = 1
                
                // 清理定时器
                if (timer) {
                    clearInterval(timer)
                    timer = null
                }
                codeDisabled.value = false
                codeButtonText.value = '发送验证码'
            }, 300)
        })
        .catch(() => {
            // 用户取消关闭操作
        })
}

// 组件卸载时清理定时器
onUnmounted(() => {
    if (timer) {
        clearInterval(timer)
        timer = null
    }
})

const toggleMenu = () => {
    menuStore.handleMenuWidth();
}

const { isFullscreen, toggle } = useFullscreen();
// 刷新页面
const handleRefresh = () => location.reload()

// 登出功能
const handleLogout = async () => {
    try {
        await ElMessageBox.confirm('您确定要退出当前账号吗？', '退出登录确认', {
            confirmButtonText: '确认退出',
            cancelButtonText: '取消',
            type: 'warning',
            center: true
        })
    } catch {
        // 用户取消退出
        return
    }

    try {
        // 服务端退出失败不应阻止本地会话结束。
        await logout()
    } catch (error) {
        console.warn('服务端退出接口调用失败，继续清理本地会话:', error)
    } finally {
        removeToken()
        userStore.clearUserInfo()
        await router.replace('/admin/login')
        ElMessage.success('已退出登录')
    }
}

</script>

<style scoped>
.admin-header {
    height: 64px;
    padding: 0 18px 0 10px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    border-bottom: 1px solid var(--ymm-line);
    background: rgba(255, 255, 255, 0.94);
    backdrop-filter: blur(16px);
}

.header-leading,
.admin-header > div:last-child {
    display: flex;
    align-items: center;
}

.header-leading {
    gap: 13px;
}

.header-action {
    width: 38px;
    height: 38px;
    padding: 0;
    display: inline-grid;
    place-items: center;
    border: 0;
    border-radius: 9px;
    color: #5f6d82;
    background: transparent;
    cursor: pointer;
    font-size: 17px;
    transition: color 0.18s ease, background 0.18s ease;
}

.header-action:hover {
    color: var(--ymm-primary);
    background: var(--ymm-primary-soft);
}

.header-action:focus-visible,
.admin-profile:focus-visible {
    outline: 3px solid rgba(40, 89, 166, 0.2);
    outline-offset: 2px;
}

.header-title {
    display: flex;
    align-items: baseline;
    gap: 10px;
}

.header-title strong {
    color: var(--ymm-ink);
    font-family: var(--ymm-font-serif);
    font-size: 17px;
    font-weight: 700;
}

.header-title span {
    color: var(--ymm-muted);
    font-size: 11px;
}

.admin-profile {
    min-height: 38px;
    margin-left: 7px;
    padding: 4px 9px 4px 6px;
    display: flex;
    align-items: center;
    border: 1px solid transparent;
    border-radius: 9px;
    color: var(--ymm-text);
    cursor: pointer;
    font-size: 13px;
    transition: border-color 0.18s ease, background 0.18s ease;
}

.admin-profile:hover {
    border-color: var(--ymm-line);
    background: #f8fafd;
}

.admin-profile-name {
    display: inline-block;
    max-width: 120px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

@media (max-width: 720px) {
    .header-title span {
        display: none;
    }

    .header-utility {
        display: none;
    }

    .admin-profile-name {
        max-width: 72px;
    }
}
</style>
