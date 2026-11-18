<template>
  <div class="user-management">
    <!-- 搜索和操作区域 -->
    <div class="search-and-actions mb-4 p-4 bg-white rounded shadow">
      <el-row :gutter="20" class="items-center">
        <!-- 搜索条件 -->
        <el-col :span="18">
          <el-form :model="searchForm" label-width="80px" class="flex flex-wrap" @submit.prevent="handleSearch">
            <el-form-item label="用户名">
              <el-input v-model="searchForm.nickname" placeholder="请输入用户名" clearable />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="searchForm.phone" placeholder="请输入手机号" clearable />
            </el-form-item>
            <el-form-item label="YMM ID">
              <el-input v-model="searchForm.ymmId" placeholder="请输入YMM ID" clearable />
            </el-form-item>
          </el-form>
        </el-col>
        
        <!-- 操作按钮 -->
        <el-col :span="6" class="flex justify-end">
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleAdd">新增用户</el-button>
        </el-col>
      </el-row>
    </div>

    <!-- 用户列表 -->
    <div class="user-list bg-white rounded shadow table-container">
      <el-table 
        :data="userList" 
        style="width: 100%" 
        v-loading="loading"
        stripe
        border
        class="user-table"
      >
        <el-table-column prop="id" label="编号" width="80" />
        <el-table-column prop="ymmId" label="YMM ID" width="120" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="avatar" label="头像" width="80">
          <template #default="scope">
            <el-image 
              :src="scope.row.avatar" 
              class="avatar-preview"
              :preview-src-list="[scope.row.avatar]"
              preview-teleported
              fit="cover"
              v-if="scope.row.avatar"
            >
              <template #error>
                <div class="image-slot">
                  <i class="el-icon-picture-outline"></i>
                </div>
              </template>
            </el-image>
            <span v-else>无头像</span>
          </template>
        </el-table-column>
        <el-table-column prop="sex" label="性别" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.sex === 0 ? 'primary' : scope.row.sex === 1 ? 'success' : 'info'">
              {{ scope.row.sex === 0 ? '男' : scope.row.sex === 1 ? '女' : '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="birthday" label="生日" width="120" />
        <el-table-column prop="phone" label="手机号" width="120" />
        <el-table-column prop="backgroundImg" label="背景图" width="100">
          <template #default="scope">
            <el-image 
              :src="scope.row.backgroundImg" 
              class="background-preview"
              :preview-src-list="[scope.row.backgroundImg]"
              preview-teleported
              fit="cover"
              v-if="scope.row.backgroundImg"
            >
              <template #error>
                <div class="image-slot">
                  <i class="el-icon-picture-outline"></i>
                </div>
              </template>
            </el-image>
            <span v-else>无背景图</span>
          </template>
        </el-table-column>
        <el-table-column prop="introduce" label="简介" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === 0 ? 'success' : 'danger'">
              {{ scope.row.status === 0 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="scope">
            {{ formatDate(scope.row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="350" fixed="right">
          <template #default="scope">
            <template v-if="canManageUserRow(scope.row.id, authorizationStore.userInfo)">
              <el-button 
                size="small" 
                :type="scope.row.status === 0 ? 'danger' : 'success'"
                @click="handleChangeStatus(scope.row)"
              >
                {{ scope.row.status === 0 ? '禁用' : '启用' }}
              </el-button>
              <el-button size="small" @click="handleEdit(scope.row)">编辑</el-button>
              <el-button v-if="canAssignRoles" size="small" type="primary" @click="assignedUser = scope.row; rolesVisible = true">分配角色</el-button>
              <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container p-4 flex justify-end">
        <el-pagination
          v-model:current-page="pagination.currentPage"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <UserRolesDialog v-model="rolesVisible" :user="assignedUser" />
    <!-- 用户编辑/新增对话框 -->
    <el-dialog 
      :title="dialogTitle" 
      v-model="dialogVisible" 
      width="600px"
      @close="handleDialogClose"
    >
      <el-form 
        :model="userForm" 
        :rules="userFormRules" 
        ref="userFormRef" 
        label-width="100px"
      >
        <el-row :gutter="20">
          <el-col :span="12">
           
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="userForm.nickname" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="userForm.phone" />
            </el-form-item>
            <el-form-item label="性别" prop="sex">
              <el-select v-model="userForm.sex" placeholder="请选择性别" style="width: 100%">
                <el-option label="男" :value="0"></el-option>
                <el-option label="女" :value="1"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="生日" prop="birthday">
              <el-date-picker 
                v-model="userForm.birthday" 
                type="date" 
                placeholder="请选择生日" 
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="头像" prop="avatar">
              <el-upload
                class="avatar-uploader"
                :show-file-list="false"
                :auto-upload="false"
                :on-change="handleAvatarChange"
              >
                <img v-if="userForm.avatar" :src="userForm.avatar" class="avatar" />
                <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
              </el-upload>
            </el-form-item>
            <el-form-item label="背景图" prop="backgroundImg">
              <el-upload
                class="background-uploader"
                :show-file-list="false"
                :auto-upload="false"
                :on-change="handleBackgroundChange"
              >
                <img v-if="userForm.backgroundImg" :src="userForm.backgroundImg" class="background" />
                <el-icon v-else class="background-uploader-icon"><Plus /></el-icon>
              </el-upload>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="简介" prop="introduce">
          <el-input 
            v-model="userForm.introduce" 
            type="textarea" 
            :rows="3" 
            placeholder="请输入简介"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="userForm.status">
            <el-radio :label="0">启用</el-radio>
            <el-radio :label="1">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!userForm.id">
          <el-input v-model="userForm.password" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitUserForm" :loading="submitLoading">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import UserRolesDialog from '@/components/UserRolesDialog.vue'
import { useUserStore } from '@/stores/user'
const authorizationStore = useUserStore()
const canAssignRoles = computed(() => mayAssignRoles(authorizationStore.userInfo))
import { canAssignRoles as mayAssignRoles, canManageUserRow } from '@/utils/authorization'
const assignedUser = ref(null)
const rolesVisible = ref(false)
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getUserList, addUser, updateUser, deleteUser } from '@/api/admin/user'

// 搜索表单
const searchForm = reactive({
  nickname: '',
  phone: '',
  ymmId: ''
})

// 分页参数
const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

// 用户列表
const userList = ref([])
const loading = ref(false)

// 对话框相关
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitLoading = ref(false)
const userFormRef = ref()
const isEdit = ref(false)

// 用户表单
const userForm = reactive({
  id: null,
  ymmId: '',
  nickname: '',
  avatar: '',
  avatarFile: null,
  birthday: '',
  introduce: '',
  sex: 0,
  backgroundImg: '',
  backgroundImgFile: null,
  phone: '',
  status: 0,
  password: ''
})

// 表单验证规则
const userFormRules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 24, message: '昵称长度在 2 到 24 个字符', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  sex: [
    { required: true, message: '请选择性别', trigger: 'change' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, message: '密码长度至少8位', trigger: 'blur' }
  ]
}

// 处理状态变更
const handleChangeStatus = (row) => {
  const newStatus = row.status === 0 ? 1 : 0;
  const statusText = newStatus === 0 ? '启用' : '禁用';
  
  ElMessageBox.confirm(
    `确定要${statusText}用户 "${row.nickname}" 吗？`,
    '状态变更确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const formData = new FormData()
      formData.append('id', row.id)
      formData.append('status', newStatus)
      const res = await updateUser(formData)
      if (res.success) {
        ElMessage.success(`${statusText}用户成功`)
        // 更新本地数据
        row.status = newStatus
      } else {
        ElMessage.error(res.message || `${statusText}用户失败`)
      }
    } catch (error) {
      ElMessage.error(`${statusText}用户失败`)
      console.error(error)
    }
  }).catch(() => {
    ElMessage.info(`已取消${statusText}`)
  })
}

// 获取用户列表
const getUserListData = async () => {
  loading.value = true
  try {
    const params = {
      currentPage: pagination.currentPage,
      pageSize: pagination.pageSize,
      nickname: searchForm.nickname,
      phone: searchForm.phone,
      ymmId: searchForm.ymmId
    }
    
    const res = await getUserList(params)
    
    if (res.success) {
      // 根据后端接口文档，数据在 res.data 中
      userList.value = res.data.records || []
      pagination.total = res.data.total || 0
    } else {
      ElMessage.error(res.message || '获取用户列表失败')
    }
  } catch (error) {
    ElMessage.error('获取用户列表失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 处理搜索
const handleSearch = () => {
  pagination.currentPage = 1
  getUserListData()
}

// 处理重置
const handleReset = () => {
  searchForm.nickname = ''
  searchForm.phone = ''
  searchForm.ymmId = ''
  pagination.currentPage = 1
  getUserListData()
}

// 处理新增用户
const handleAdd = () => {
  dialogTitle.value = '新增用户'
  isEdit.value = false
  resetUserForm()
  dialogVisible.value = true
}

// 处理编辑用户
const handleEdit = (row) => {
  dialogTitle.value = '编辑用户'
  isEdit.value = true
  Object.assign(userForm, row)
  userForm.sex = [0, 1].includes(row.sex) ? row.sex : undefined
  // 编辑时不传密码字段
  delete userForm.password
  dialogVisible.value = true
}

// 处理删除用户
const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确定要删除用户 "${row.nickname}" 吗？`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const res = await deleteUser(row.ymmId)
      if (res.success) {
        ElMessage.success('删除成功')
        getUserListData() // 重新加载列表
      } else {
        ElMessage.error(res.message || '删除失败')
      }
    } catch (error) {
      ElMessage.error('删除失败')
      console.error(error)
    }
  }).catch(() => {
    ElMessage.info('已取消删除')
  })
}

// 提交用户表单
const submitUserForm = () => {
  userFormRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        let res
        if (isEdit.value) {
          // 编辑用户
          const formData = new FormData()
          formData.append('id', userForm.id)
          formData.append('nickname', userForm.nickname)
          if (userForm.password) {
            formData.append('newPassword', userForm.password)
          }
          if (userForm.birthday) {
            formData.append('birthday', userForm.birthday)
          }
          if (userForm.introduce) {
            formData.append('introduce', userForm.introduce)
          }
          formData.append('sex', userForm.sex)
          formData.append('phone', userForm.phone)
          formData.append('status', userForm.status)
          
          if (userForm.avatarFile) {
            formData.append('avatar', userForm.avatarFile)
          }
          if (userForm.backgroundImgFile) {
            formData.append('backgroundImg', userForm.backgroundImgFile)
          }
          
          res = await updateUser(formData)
        } else {
          // 新增用户
          res = await addUser(userForm)
        }
        
        if (res.success) {
          ElMessage.success(`${isEdit.value ? '编辑' : '新增'}用户成功`)
          dialogVisible.value = false
          getUserListData() // 重新加载列表
        } else {
          ElMessage.error(res.message || `${isEdit.value ? '编辑' : '新增'}用户失败`)
        }
      } catch (error) {
        ElMessage.error(`${isEdit.value ? '编辑' : '新增'}用户失败`)
        console.error(error)
      } finally {
        submitLoading.value = false
      }
    }
  })
}

// 重置用户表单
const resetUserForm = () => {
  Object.keys(userForm).forEach(key => {
    userForm[key] = key === 'sex' ? 0 : key === 'status' ? 0 : ''
  })
  userForm.avatarFile = null
  userForm.backgroundImgFile = null
}

// 处理对话框关闭
const handleDialogClose = () => {
  userFormRef.value.resetFields()
}

// 处理分页大小变化
const handleSizeChange = (val) => {
  pagination.pageSize = val
  getUserListData()
}

// 处理当前页变化
const handleCurrentChange = (val) => {
  pagination.currentPage = val
  getUserListData()
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  }).replace(/\//g, '-')
}

// 头像上传相关方法
const handleAvatarChange = (uploadFile) => {
  const rawFile = uploadFile.raw
  if (rawFile.type !== 'image/jpeg' && rawFile.type !== 'image/png') {
    ElMessage.error('头像图片必须是 JPG 或 PNG 格式!')
    return
  } else if (rawFile.size / 1024 / 1024 > 2) {
    ElMessage.error('头像图片大小不能超过 2MB!')
    return
  }
  userForm.avatar = URL.createObjectURL(rawFile)
  userForm.avatarFile = rawFile
}

// 背景图上传相关方法
const handleBackgroundChange = (uploadFile) => {
  const rawFile = uploadFile.raw
  if (rawFile.type !== 'image/jpeg' && rawFile.type !== 'image/png') {
    ElMessage.error('背景图片必须是 JPG 或 PNG 格式!')
    return
  } else if (rawFile.size / 1024 / 1024 > 5) {
    ElMessage.error('背景图片大小不能超过 5MB!')
    return
  }
  userForm.backgroundImg = URL.createObjectURL(rawFile)
  userForm.backgroundImgFile = rawFile
}

// 组件挂载时获取用户列表
onMounted(() => {
  getUserListData()
})
</script>

<style scoped>
.user-management {
  padding: 20px;
}

.search-and-actions {
  margin-bottom: 20px;
}

.search-and-actions .el-form-item {
  margin-right: 20px;
  margin-bottom: 0;
}

.pagination-container {
  background-color: #fff;
}

.table-container {
  overflow-x: auto;
  max-width: 100%;
}

.user-table {
  min-width: 1200px;
}

.avatar-preview {
  width: 60px;
  height: 60px;
  border-radius: 50%;
}

.background-preview {
  width: 100px;
  height: 60px;
  border-radius: 4px;
}

.image-slot {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  background: #f5f7fa;
  color: #909399;
}

.avatar-uploader .avatar {
  width: 100px;
  height: 100px;
  display: block;
  border-radius: 50%;
}

.background-uploader .background {
  width: 150px;
  height: 100px;
  display: block;
  border-radius: 4px;
}

.avatar-uploader .el-upload {
  border: 1px dashed #d9d9d9;
  border-radius: 50%;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
  width: 100px;
  height: 100px;
}

.background-uploader .el-upload {
  border: 1px dashed #d9d9d9;
  border-radius: 4px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
  width: 150px;
  height: 100px;
}

.avatar-uploader .el-upload:hover {
  border-color: var(--el-color-primary);
}

.background-uploader .el-upload:hover {
  border-color: var(--el-color-primary);
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 100px;
  height: 100px;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
}

.background-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 150px;
  height: 100px;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
