<template>
  <el-dialog :model-value="modelValue && allowed" :title="`分配权限 · ${role?.roleName || ''}`" width="560px" :close-on-click-modal="false" @update:model-value="$emit('update:modelValue', $event)">
    <p class="hint">各权限独立勾选。灰色项已禁用，已绑定项可以保留或撤销。</p>
    <div v-loading="loading">
      <el-tree ref="treeRef" :data="tree" node-key="id" show-checkbox check-strictly default-expand-all :props="{ label: 'label', children: 'children', disabled: 'disabled' }" />
      <el-empty v-if="!loading && !tree.length" description="暂无可分配权限" />
    </div>
    <template #footer><el-button :disabled="saving" @click="$emit('update:modelValue', false)">取消</el-button><el-button type="primary" :loading="saving" :disabled="loading || !ready" @click="save">保存授权</el-button></template>
  </el-dialog>
</template>
<script setup>
import { computed, ref, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { getPermissionList } from '@/api/admin/permission'
import { getRolePermissions, assignRolePermissions } from '@/api/admin/role'
import { permissionTree } from '@/utils/authorization'
const props = defineProps({ modelValue: Boolean, role: Object })
import { useUserStore } from '@/stores/user'
import { canAssignPermissions } from '@/utils/authorization'
const store = useUserStore()
const allowed = computed(() => canAssignPermissions(store.userInfo))
const emit = defineEmits(['update:modelValue'])
const tree = ref([]), treeRef = ref(), loading = ref(false), saving = ref(false), ready = ref(false)
let version = 0
watch(() => [props.modelValue && allowed.value, props.role?.id], async ([open, id]) => {
  const current = ++version
  if (!open || !id) { ready.value = false; if (props.modelValue) emit('update:modelValue', false); return }
  tree.value = []; ready.value = false; loading.value = true
  try {
    const [permissions, assigned] = await Promise.all([getPermissionList(), getRolePermissions(id)])
    if (current !== version) return
    if (!permissions.success || !assigned.success) throw new Error(permissions.message || assigned.message)
    const ids = (assigned.data || []).map(String)
    tree.value = permissionTree((permissions.data || []).map(p => ({ ...p, label: `${p.name}${p.status === 1 ? '（已禁用）' : ''}`, disabled: p.status === 1 && !ids.includes(String(p.id)) })))
    await nextTick(); if (current !== version || !allowed.value) return; treeRef.value?.setCheckedKeys(ids); ready.value = true
  } catch (e) { ElMessage.error(e.message || '加载授权失败') } finally { if (current === version) loading.value = false }
})
async function save() {
  if (saving.value || !ready.value || !allowed.value) return
  saving.value = true
  try {
    const res = await assignRolePermissions(props.role.id, treeRef.value.getCheckedKeys())
    if (!res.success) throw new Error(res.message)
    ElMessage.success('角色权限已更新'); emit('update:modelValue', false)
  } catch (e) { ElMessage.error(e.message || '授权失败') } finally { saving.value = false }
}
</script>
<style scoped>.hint { color: #64748b; font-size: 13px; margin-bottom: 16px; }</style>
