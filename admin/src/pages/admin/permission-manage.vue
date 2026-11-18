<template>
  <div v-if="canManagePermissions" class="permission-management">
    <div class="toolbar">
      <div><h2>权限管理</h2><p>配置权限及层级，再到角色管理中分配。层级仅用于整理，各项权限独立生效。</p></div>
      <el-button type="primary" @click="openForm()">新增权限</el-button>
    </div>
    <el-table :data="tree" v-loading="loading" row-key="id" default-expand-all border>
      <el-table-column prop="name" label="权限名称" min-width="170" />
      <el-table-column label="类型" width="85"><template #default="{ row }">{{ types[row.type] }}</template></el-table-column>
      <el-table-column prop="permissionKey" label="权限标识" min-width="200" show-overflow-tooltip />
      <el-table-column prop="menuUrl" label="菜单路径" min-width="170" show-overflow-tooltip />
      <el-table-column prop="sort" label="排序" width="70" />
      <el-table-column label="状态" width="80"><template #default="{ row }"><el-tag :type="row.status === 0 ? 'success' : 'info'">{{ row.status === 0 ? '启用' : '禁用' }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="310" fixed="right"><template #default="{ row }">
        <el-button link type="primary" :disabled="row.type === 3" @click="openForm(null, row.id)">添加子项</el-button>
        <el-button link type="primary" @click="openForm(row)">编辑</el-button>
        <el-button link type="warning" :disabled="busy" @click="toggle(row)">{{ row.status === 0 ? '禁用' : '启用' }}</el-button>
        <el-button link type="danger" :disabled="busy" @click="remove(row)">删除</el-button>
      </template></el-table-column>
      <template #empty>暂无权限，可通过“新增权限”添加</template>
    </el-table>
    <el-dialog v-model="visible" :title="form.id ? '编辑权限' : '新增权限'" width="580px" :close-on-click-modal="false" @closed="formRef?.clearValidate()">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="95px" @submit.prevent="save">
        <el-form-item label="权限名称" prop="name"><el-input v-model.trim="form.name" maxlength="16" show-word-limit /></el-form-item>
        <el-form-item label="类型" prop="type"><el-radio-group v-model="form.type"><el-radio :label="1">目录</el-radio><el-radio :label="2">菜单</el-radio><el-radio :label="3">按钮</el-radio></el-radio-group></el-form-item>
        <el-form-item label="上级权限"><el-tree-select v-model="form.parentId" :data="parents" node-key="id" :props="{ label: 'name', children: 'children', disabled: 'disabled' }" check-strictly default-expand-all style="width: 100%" /></el-form-item>
        <el-form-item label="权限标识" prop="permissionKey"><el-input v-model.trim="form.permissionKey" :disabled="builtIn" maxlength="64" placeholder="如 admin:note:manage" /><small v-if="builtIn">内置标识用于接口鉴权，不可修改。</small></el-form-item>
        <el-form-item label="菜单路径" prop="menuUrl"><el-input v-model.trim="form.menuUrl" maxlength="32" placeholder="如 /admin/content/article" /></el-form-item>
        <el-form-item label="图标" prop="menuIcon"><el-input v-model.trim="form.menuIcon" maxlength="255" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" :max="2147483647" :precision="0" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio :label="0">启用</el-radio><el-radio :label="1">禁用</el-radio></el-radio-group></el-form-item>
        <p class="hint">自定义权限用于授权配置，不会自动生成页面或接口。</p>
      </el-form>
      <template #footer><el-button :disabled="busy" @click="visible = false">取消</el-button><el-button type="primary" :loading="busy" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPermissionList, createPermission, updatePermission, deletePermission } from '@/api/admin/permission'
import { builtinPermissionKeys, permissionTree } from '@/utils/authorization'
import { useUserStore } from '@/stores/user'
import { isAdmin } from '@/utils/authorization'
const authorizationStore = useUserStore()
const canManagePermissions = computed(() => isAdmin(authorizationStore.userInfo) && (authorizationStore.userInfo.permissionKeys || []).includes('admin:permission:manage'))
const types = { 1: '目录', 2: '菜单', 3: '按钮' }
const list = ref([]), loading = ref(false), busy = ref(false), visible = ref(false), formRef = ref()
const empty = () => ({ id: null, parentId: '0', name: '', type: 2, permissionKey: '', menuUrl: '', menuIcon: '', sort: 0, status: 0 })
const form = reactive(empty())
const originalKey = ref('')
const builtIn = computed(() => Boolean(form.id && builtinPermissionKeys.includes(originalKey.value)))
const tree = computed(() => permissionTree(list.value))
const parents = computed(() => {
  const blocked = new Set(form.id ? [String(form.id)] : [])
  let changed = true
  while (changed) { changed = false; list.value.forEach(p => { if (blocked.has(String(p.parentId)) && !blocked.has(String(p.id))) { blocked.add(String(p.id)); changed = true } }) }
  const items = list.value.map(p => ({ ...p, disabled: p.type === 3 || blocked.has(String(p.id)) }))
  return [{ id: '0', name: '顶级权限', children: permissionTree(items) }]
})
const rules = {
  name: [{ required: true, message: '请输入权限名称', trigger: 'blur' }, { max: 16, message: '最多16个字', trigger: 'blur' }],
  permissionKey: [{ required: true, message: '请输入权限标识', trigger: 'blur' }, { pattern: /^[a-z][a-z0-9]*(?::[a-z][a-z0-9]*)+$/, message: '使用小写字母、数字及冒号分隔，如 admin:note:manage', trigger: 'blur' }],
}
function payload(row) { return Object.fromEntries(Object.keys(empty()).map(key => [key, row[key] ?? empty()[key]])) }
async function load() { loading.value = true; try { const res = await getPermissionList(); if (!res.success) throw new Error(res.message); list.value = res.data || [] } catch (e) { ElMessage.error(e.message || '获取权限失败') } finally { loading.value = false } }
function openForm(row, parentId = '0') { Object.assign(form, row ? payload(row) : { ...empty(), parentId: String(parentId) }); form.parentId = String(form.parentId); originalKey.value = row?.permissionKey || ''; visible.value = true }
async function save() {
  if (!canManagePermissions.value) return
  if (busy.value || !await formRef.value.validate().catch(() => false)) return
  busy.value = true
  try { const res = await (form.id ? updatePermission(payload(form)) : createPermission(payload(form))); if (!res.success) throw new Error(res.message); visible.value = false; ElMessage.success('保存成功'); await load() } catch (e) { ElMessage.error(e.message || '保存失败') } finally { busy.value = false }
}
async function toggle(row) {
  if (!canManagePermissions.value) return
  try { await ElMessageBox.confirm(`确定${row.status === 0 ? '禁用' : '启用'}权限“${row.name}”？变更会影响已授权角色。`, '确认操作', { type: 'warning' }) } catch { return }
  busy.value = true
  try { const res = await updatePermission({ ...payload(row), status: row.status === 0 ? 1 : 0 }); if (!res.success) throw new Error(res.message); await load(); ElMessage.success('状态已更新') } catch (e) { ElMessage.error(e.message || '更新失败') } finally { busy.value = false }
}
async function remove(row) {
  if (!canManagePermissions.value) return
  try { await ElMessageBox.confirm(`确定删除权限“${row.name}”？存在子项或角色引用时不能删除。`, '删除权限', { type: 'warning' }) } catch { return }
  busy.value = true
  try { const res = await deletePermission(row.id); if (!res.success) throw new Error(res.message); await load(); ElMessage.success('已删除') } catch (e) { ElMessage.error(e.message || '删除失败') } finally { busy.value = false }
}
onMounted(load)
</script>
<style scoped>
.permission-management { padding: 20px; }
.toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
h2 { margin: 0 0 6px; font-size: 18px; } p, .hint, small { color: #64748b; font-size: 13px; }
</style>
