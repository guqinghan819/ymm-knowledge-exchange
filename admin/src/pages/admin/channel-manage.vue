<template>
  <div class="channel-management">
    <!-- 操作区域 -->
    <div class="search-and-actions mb-4 p-4 bg-white rounded shadow flex justify-between items-center">
      <div class="text-base font-medium">频道管理</div>
      <el-button type="primary" @click="handleAdd">新增频道</el-button>
    </div>

    <!-- 频道列表 -->
    <div class="channel-list bg-white rounded shadow table-container">
      <el-table
        :data="channelList"
        style="width: 100%"
        v-loading="loading"
        stripe
        border
        class="channel-table"
      >
        <el-table-column prop="id" label="频道ID" width="100" />
        <el-table-column prop="name" label="频道名称" width="150" />
        <el-table-column label="关联话题" min-width="240">
          <template #default="scope">
            <template v-if="scope.row.topics && scope.row.topics.length">
              <el-tag
                v-for="t in scope.row.topics"
                :key="t.id"
                size="small"
                type="info"
                class="mr-1 mb-1"
              >
                {{ t.name }}
              </el-tag>
            </template>
            <span v-else class="text-xs text-gray-400">未绑定话题</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column prop="updateTime" label="更新时间" width="170" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="scope">
            <el-button size="small" type="primary" @click="handleBind(scope.row)">绑定话题</el-button>
            <el-button size="small" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增/编辑频道对话框 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="420px" @close="handleDialogClose">
      <el-form :model="channelForm" :rules="channelFormRules" ref="channelFormRef" label-width="90px">
        <el-form-item label="频道名称" prop="name">
          <el-input v-model="channelForm.name" placeholder="请输入频道名称(最多12个字)" maxlength="12" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitChannelForm" :loading="submitLoading">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 绑定话题对话框 -->
    <el-dialog
      :title="`绑定话题 - ${bindChannel?.name || ''}`"
      v-model="bindVisible"
      width="520px"
      destroy-on-close
    >
      <div class="bind-tip">勾选该频道下需要绑定的话题，取消勾选即为解除绑定</div>
      <div class="bind-topic-wrap">
        <el-checkbox-group v-model="bindTopicIds" class="bind-topic-list">
          <el-checkbox
            v-for="t in allTopics"
            :key="t.id"
            :label="t.id"
            class="bind-topic-item"
          >
            {{ t.name }}
          </el-checkbox>
        </el-checkbox-group>
        <div v-if="!allTopics.length" class="bind-topic-empty">
          暂无话题，请先在「话题管理」中新增
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="bindVisible = false">取消</el-button>
          <el-button type="primary" @click="submitBind" :loading="bindLoading">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getChannelList, createChannel, updateChannel, deleteChannel, bindTopics } from '@/api/admin/channel'
import { getTopicList } from '@/api/admin/topic'

const loading = ref(false)
const channelList = ref([])

// 新增/编辑频道
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitLoading = ref(false)
const channelFormRef = ref()
const isEdit = ref(false)
const channelForm = reactive({ id: null, name: '' })
const channelFormRules = {
  name: [
    { required: true, message: '请输入频道名称', trigger: 'blur' },
    { max: 12, message: '频道名称最多12个字符', trigger: 'blur' }
  ]
}

// 绑定话题
const bindVisible = ref(false)
const bindLoading = ref(false)
const bindChannel = ref(null)
const allTopics = ref([])
const bindTopicIds = ref([])

// 获取频道列表
const getList = async () => {
  loading.value = true
  try {
    const res = await getChannelList()
    if (res.success) {
      channelList.value = res.data || []
    } else {
      ElMessage.error(res.message || '获取频道列表失败')
    }
  } catch (error) {
    ElMessage.error('获取频道列表失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 获取全部话题(用于绑定)
const getAllTopics = async () => {
  try {
    const res = await getTopicList()
    if (res.success) {
      allTopics.value = (res.data || []).map(t => ({ id: t.id, name: t.name }))
    }
  } catch (error) {
    console.error(error)
  }
}

const handleAdd = () => {
  dialogTitle.value = '新增频道'
  isEdit.value = false
  channelForm.id = null
  channelForm.name = ''
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑频道'
  isEdit.value = true
  channelForm.id = row.id
  channelForm.name = row.name
  dialogVisible.value = true
}

const submitChannelForm = () => {
  channelFormRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      const res = isEdit.value
        ? await updateChannel({ id: channelForm.id, name: channelForm.name })
        : await createChannel({ name: channelForm.name })
      if (res.success) {
        ElMessage.success(`${isEdit.value ? '编辑' : '新增'}频道成功`)
        dialogVisible.value = false
        getList()
      } else {
        ElMessage.error(res.message || `${isEdit.value ? '编辑' : '新增'}频道失败`)
      }
    } catch (error) {
      ElMessage.error(`${isEdit.value ? '编辑' : '新增'}频道失败`)
      console.error(error)
    } finally {
      submitLoading.value = false
    }
  })
}

const handleDialogClose = () => {
  channelFormRef.value?.resetFields()
}

const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确定删除频道「${row.name}」吗？删除后其下话题绑定关系将被清除。`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const res = await deleteChannel(row.id)
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

const handleBind = (row) => {
  bindChannel.value = row
  bindTopicIds.value = (row.topics || []).map(t => t.id)
  bindVisible.value = true
}

const submitBind = async () => {
  bindLoading.value = true
  try {
    const res = await bindTopics({ channelId: bindChannel.value.id, topicIds: bindTopicIds.value })
    if (res.success) {
      ElMessage.success('绑定成功')
      bindVisible.value = false
      getList()
    } else {
      ElMessage.error(res.message || '绑定失败')
    }
  } catch (error) {
    ElMessage.error('绑定失败')
    console.error(error)
  } finally {
    bindLoading.value = false
  }
}

onMounted(() => {
  getList()
  getAllTopics()
})
</script>

<style scoped>
.channel-management {
  padding: 20px;
}

.search-and-actions {
  margin-bottom: 20px;
}

.table-container {
  overflow-x: auto;
  max-width: 100%;
}

.channel-table {
  min-width: 1000px;
}

/* 绑定话题弹窗 */
.bind-tip {
  font-size: 12px;
  color: #909399;
  margin-bottom: 10px;
}

.bind-topic-wrap {
  max-height: 320px;
  overflow-y: auto;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 10px 14px;
}

.bind-topic-list .bind-topic-item {
  display: flex;
  align-items: center;
  margin-right: 0;
  margin-bottom: 8px;
}

.bind-topic-list .bind-topic-item:last-child {
  margin-bottom: 0;
}

.bind-topic-empty {
  text-align: center;
  font-size: 12px;
  color: #c0c4cc;
  padding: 20px 0;
}
</style>
