<template>
  <div class="topic-management">
    <!-- 操作区域 -->
    <div class="search-and-actions mb-4 p-4 bg-white rounded shadow flex justify-between items-center">
      <div class="text-base font-medium">话题管理</div>
      <el-button type="primary" @click="handleAdd">新增话题</el-button>
    </div>

    <!-- 话题列表 -->
    <div class="topic-list bg-white rounded shadow table-container">
      <el-table
        :data="topicList"
        style="width: 100%"
        v-loading="loading"
        stripe
        border
        class="topic-table"
      >
        <el-table-column prop="id" label="话题ID" width="100" />
        <el-table-column prop="name" label="话题名称" width="160" />
        <el-table-column label="所属频道" min-width="240">
          <template #default="scope">
            <template v-if="scope.row.channels && scope.row.channels.length">
              <el-tag
                v-for="c in scope.row.channels"
                :key="c.id"
                size="small"
                type="success"
                class="mr-1 mb-1"
              >
                {{ c.name }}
              </el-tag>
            </template>
            <span v-else class="text-xs text-gray-400">未归属任何频道</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column prop="updateTime" label="更新时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <el-button size="small" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增/编辑话题对话框 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="420px" @close="handleDialogClose">
      <el-form :model="topicForm" :rules="topicFormRules" ref="topicFormRef" label-width="90px">
        <el-form-item label="话题名称" prop="name">
          <el-input v-model="topicForm.name" placeholder="请输入话题名称(最多12个字)" maxlength="12" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitTopicForm" :loading="submitLoading">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTopicList, createTopic, updateTopic, deleteTopic } from '@/api/admin/topic'

const loading = ref(false)
const topicList = ref([])

// 新增/编辑话题
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitLoading = ref(false)
const topicFormRef = ref()
const isEdit = ref(false)
const topicForm = reactive({ id: null, name: '' })
const topicFormRules = {
  name: [
    { required: true, message: '请输入话题名称', trigger: 'blur' },
    { max: 12, message: '话题名称最多12个字符', trigger: 'blur' }
  ]
}

// 获取话题列表
const getList = async () => {
  loading.value = true
  try {
    const res = await getTopicList()
    if (res.success) {
      topicList.value = res.data || []
    } else {
      ElMessage.error(res.message || '获取话题列表失败')
    }
  } catch (error) {
    ElMessage.error('获取话题列表失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  dialogTitle.value = '新增话题'
  isEdit.value = false
  topicForm.id = null
  topicForm.name = ''
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑话题'
  isEdit.value = true
  topicForm.id = row.id
  topicForm.name = row.name
  dialogVisible.value = true
}

const submitTopicForm = () => {
  topicFormRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      const res = isEdit.value
        ? await updateTopic({ id: topicForm.id, name: topicForm.name })
        : await createTopic({ name: topicForm.name })
      if (res.success) {
        ElMessage.success(`${isEdit.value ? '编辑' : '新增'}话题成功`)
        dialogVisible.value = false
        getList()
      } else {
        ElMessage.error(res.message || `${isEdit.value ? '编辑' : '新增'}话题失败`)
      }
    } catch (error) {
      ElMessage.error(`${isEdit.value ? '编辑' : '新增'}话题失败`)
      console.error(error)
    } finally {
      submitLoading.value = false
    }
  })
}

const handleDialogClose = () => {
  topicFormRef.value?.resetFields()
}

const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确定删除话题「${row.name}」吗？删除后其频道绑定关系将被清除。`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const res = await deleteTopic(row.id)
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
.topic-management {
  padding: 20px;
}

.search-and-actions {
  margin-bottom: 20px;
}

.table-container {
  overflow-x: auto;
  max-width: 100%;
}

.topic-table {
  min-width: 900px;
}
</style>
