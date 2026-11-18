<template>
  <el-dialog :model-value="modelValue && allowed" :title="`分配角色 · ${user?.nickname || user?.ymmId || ''}`" width="520px" :close-on-click-modal="false" @update:model-value="$emit('update:modelValue', $event)">
    <div v-loading="loading">
      <el-checkbox-group v-model="selected"><div v-for="role in roles" :key="role.id" class="role-choice"><el-checkbox :label="String(role.id)">{{ role.roleName }}（{{ role.roleKey }}）</el-checkbox></div></el-checkbox-group>
      <el-empty v-if="!loading && !roles.length" description="暂无启用角色" />
    </div>
    <template #footer><el-button :disabled="saving" @click="$emit('update:modelValue', false)">取消</el-button><el-button type="primary" :loading="saving" :disabled="loading || !ready" @click="save">保存角色</el-button></template>
  </el-dialog>
</template>
<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getRoleList, getUserRoles, assignUserRoles } from '@/api/admin/role'
const props = defineProps({ modelValue: Boolean, user: Object })
import { useUserStore } from '@/stores/user'
import { canAssignRoles, canManageUserRow } from '@/utils/authorization'
const store = useUserStore()
const allowed = computed(() => canAssignRoles(store.userInfo) && canManageUserRow(props.user?.id, store.userInfo))
const emit = defineEmits(['update:modelValue'])
const roles = ref([]), selected = ref([]), loading = ref(false), saving = ref(false), ready = ref(false)
let version = 0
watch(() => [props.modelValue && allowed.value, props.user?.id], async ([open, id]) => {
  const current = ++version
  if (!open || !id) { ready.value = false; if (props.modelValue) emit('update:modelValue', false); return }
  ready.value = false; loading.value = true; roles.value = []; selected.value = []
  try {
    const [all, assigned] = await Promise.all([getRoleList({ status: 0 }), getUserRoles(id)])
    if (current !== version) return
    if (!all.success || !assigned.success) throw new Error(all.message || assigned.message)
    roles.value = all.data || []; selected.value = (assigned.data || []).map(String); ready.value = true
  } catch (e) { ElMessage.error(e.message || '加载角色失败') } finally { if (current === version) loading.value = false }
})
async function save() {
  if (saving.value || !ready.value || !allowed.value) return
  saving.value = true
  try { const res = await assignUserRoles(props.user.id, selected.value); if (!res.success) throw new Error(res.message); ElMessage.success('用户角色已更新'); emit('update:modelValue', false) } catch (e) { ElMessage.error(e.message || '保存失败') } finally { saving.value = false }
}
</script>
<style scoped>.role-choice { margin: 8px 0; }</style>
