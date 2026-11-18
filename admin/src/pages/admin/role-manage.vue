<template>
  <div class="role-management">
    <!-- 搜索和操作区域 -->
    <div class="search-and-actions mb-4 p-4 bg-white rounded shadow">
      <el-row :gutter="20" class="items-center">
        <el-col :span="18">
          <el-form :model="searchForm" label-width="80px" class="flex flex-wrap" @submit.prevent="handleSearch">
            <el-form-item label="角色名称">
              <el-input v-model="searchForm.roleName" placeholder="请输入角色名称" clearable style="width: 180px" />
            </el-form-item>
            <el-form-item label="角色标识">
              <el-input v-model="searchForm.roleKey" placeholder="请输入角色标识" clearable style="width: 180px" />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 130px">
                <el-option label="启用" :value="0" />
                <el-option label="禁用" :value="1" />
              </el-select>
            </el-form-item>
          </el-form>
        </el-col>

        <el-col :span="6" class="flex justify-end">
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleAdd">新增角色</el-button>
        </el-col>
      </el-row>
    </div>

    <!-- 角色列表 -->
    <div class="role-list bg-white rounded shadow table-container">
      <el-table
        :data="roleList"
        style="width: 100%"
        v-loading="loading"
        stripe
        border
        class="role-table"
      >
        <el-table-column prop="id" label="角色ID" width="80" />
        <el-table-column prop="roleName" label="角色名称" width="160" />
        <el-table-column prop="roleKey" label="角色标识" width="150" />
        <el-table-column label="状态" width="90">
          <template #default="scope">
            <el-tag :type="scope.row.status === 0 ? 'success' : 'danger'">
              {{ scope.row.status === 0 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="70" />
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="350" fixed="right">
          <template #default="scope">
            <el-button
              size="small"
              :type="scope.row.status === 0 ? 'danger' : 'success'"
              @click="handleChangeStatus(scope.row)"
            >
              {{ scope.row.status === 0 ? '禁用' : '启用' }}
            </el-button>
            <el-button size="small" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button v-if="canAssignPermissions" size="small" type="primary" :disabled="scope.row.status !== 0" @click="assignedRole = scope.row; permissionsVisible = true">分配权限</el-button>
            <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <RolePermissionsDialog v-model="permissionsVisible" :role="assignedRole" />
    <!-- 新增/编辑角色对话框 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="480px" @close="handleDialogClose">
      <el-form :model="roleForm" :rules="roleFormRules" ref="roleFormRef" label-width="90px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="roleForm.roleName" placeholder="请输入角色名称(最多32个字)" maxlength="32" />
        </el-form-item>
        <el-form-item label="角色标识" prop="roleKey">
          <el-input v-model="roleForm.roleKey" placeholder="如 admin / common_user" maxlength="32" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="roleForm.status">
            <el-radio :label="0">启用</el-radio>
            <el-radio :label="1">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="roleForm.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="roleForm.remark" type="textarea" :rows="3" placeholder="请输入备注" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitRoleForm" :loading="submitLoading">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { useUserStore } from '@/stores/user'
import { canAssignPermissions as mayAssignPermissions } from '@/utils/authorization'
const authorizationStore = useUserStore()
const canAssignPermissions = computed(() => mayAssignPermissions(authorizationStore.userInfo))
import { computed, ref, reactive, onMounted } from 'vue'
import RolePermissionsDialog from '@/components/RolePermissionsDialog.vue'
const permissionsVisible = ref(false)
const assignedRole = ref(null)
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRoleList, createRole, updateRole, deleteRole } from '@/api/admin/role'

// 搜索表单
const searchForm = reactive({
  roleName: '',
  roleKey: '',
  status: null
})

// 角色列表
const roleList = ref([])
const loading = ref(false)

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitLoading = ref(false)
const roleFormRef = ref()
const isEdit = ref(false)

const roleForm = reactive({
  id: null,
  roleName: '',
  roleKey: '',
  status: 0,
  sort: 0,
  remark: ''
})

const roleFormRules = {
  roleName: [
    { required: true, message: '请输入角色名称', trigger: 'blur' },
    { max: 32, message: '角色名称最多32个字符', trigger: 'blur' }
  ],
  roleKey: [
    { required: true, message: '请输入角色标识', trigger: 'blur' },
    { max: 32, message: '角色标识最多32个字符', trigger: 'blur' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
}

// 获取角色列表
const getList = async () => {
  loading.value = true
  try {
    const params = {
      roleName: searchForm.roleName || null,
      roleKey: searchForm.roleKey || null,
      status: (searchForm.status === null || searchForm.status === '' || searchForm.status === undefined)
        ? null
        : searchForm.status
    }
    const res = await getRoleList(params)
    if (res.success) {
      roleList.value = res.data || []
    } else {
      ElMessage.error(res.message || '获取角色列表失败')
    }
  } catch (error) {
    ElMessage.error('获取角色列表失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  getList()
}

const handleReset = () => {
  searchForm.roleName = ''
  searchForm.roleKey = ''
  searchForm.status = null
  getList()
}

// 状态切换
const handleChangeStatus = (row) => {
  const newStatus = row.status === 0 ? 1 : 0
  const statusText = newStatus === 0 ? '启用' : '禁用'

  ElMessageBox.confirm(
    `确定要${statusText}角色 "${row.roleName}" 吗？`,
    '状态变更确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const res = await updateRole({ id: row.id, status: newStatus })
      if (res.success) {
        ElMessage.success(`${statusText}成功`)
        row.status = newStatus
      } else {
        ElMessage.error(res.message || `${statusText}失败`)
      }
    } catch (error) {
      ElMessage.error(`${statusText}失败`)
      console.error(error)
    }
  }).catch(() => {
    ElMessage.info(`已取消${statusText}`)
  })
}

// 新增
const handleAdd = () => {
  dialogTitle.value = '新增角色'
  isEdit.value = false
  Object.assign(roleForm, { id: null, roleName: '', roleKey: '', status: 0, sort: 0, remark: '' })
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  dialogTitle.value = '编辑角色'
  isEdit.value = true
  Object.assign(roleForm, {
    id: row.id,
    roleName: row.roleName,
    roleKey: row.roleKey,
    status: row.status,
    sort: row.sort ?? 0,
    remark: row.remark || ''
  })
  dialogVisible.value = true
}

// 提交
const submitRoleForm = () => {
  roleFormRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      const res = isEdit.value ? await updateRole(roleForm) : await createRole(roleForm)
      if (res.success) {
        ElMessage.success(`${isEdit.value ? '编辑' : '新增'}角色成功`)
        dialogVisible.value = false
        getList()
      } else {
        ElMessage.error(res.message || `${isEdit.value ? '编辑' : '新增'}角色失败`)
      }
    } catch (error) {
      ElMessage.error(`${isEdit.value ? '编辑' : '新增'}角色失败`)
      console.error(error)
    } finally {
      submitLoading.value = false
    }
  })
}

const handleDialogClose = () => {
  roleFormRef.value?.resetFields()
}

// 删除
const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确定删除角色 "${row.roleName}" 吗？`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const res = await deleteRole(row.id)
      if (res.success) {
        ElMessage.success('删除成功')
        getList()
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

onMounted(() => {
  getList()
})
</script>

<style scoped>
.role-management {
  padding: 20px;
}

.search-and-actions {
  margin-bottom: 20px;
}

.search-and-actions .el-form-item {
  margin-right: 20px;
  margin-bottom: 0;
}

.table-container {
  overflow-x: auto;
  max-width: 100%;
}

.role-table {
  min-width: 1000px;
}
</style>
