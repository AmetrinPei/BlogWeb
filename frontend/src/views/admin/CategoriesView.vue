<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createAdminCategory,
  deleteAdminCategory,
  fetchAdminCategories,
  updateAdminCategory,
} from '@/api/adminCategories'

const loading = ref(false)
const saving = ref(false)
const categories = ref([])

const dialogVisible = ref(false)
const editingId = ref(null)
const isEdit = computed(() => editingId.value != null)

const formRef = ref()
const form = reactive({
  name: '',
})

const rules = {
  name: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { max: 64, message: '名称不超过 64 字', trigger: 'blur' },
  ],
}

function resetForm() {
  form.name = ''
  formRef.value?.clearValidate()
}

async function loadList() {
  loading.value = true
  try {
    categories.value = (await fetchAdminCategories()) ?? []
  } catch (e) {
    ElMessage.error(e.message || '加载分类失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  form.name = row.name || ''
  dialogVisible.value = true
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  const payload = { name: form.name.trim() }
  saving.value = true
  try {
    if (isEdit.value) {
      await updateAdminCategory(editingId.value, payload)
      ElMessage.success('已更新')
    } else {
      await createAdminCategory(payload)
      ElMessage.success('已创建')
    }
    dialogVisible.value = false
    await loadList()
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function onDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除分类「${row.name}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }

  try {
    await deleteAdminCategory(row.id)
    ElMessage.success('已删除')
    await loadList()
  } catch (e) {
    // 展示后端明确提示，如「该分类下仍有文章，无法删除」
    ElMessage.error(e.message || '删除失败')
  }
}

onMounted(loadList)
</script>

<template>
  <section class="admin-panel">
    <header class="toolbar">
      <div>
        <h1>分类管理</h1>
        <p class="sub">增删改查分类；已被文章引用的分类不可删除</p>
      </div>
      <el-button type="primary" @click="openCreate">新建分类</el-button>
    </header>

    <el-table v-loading="loading" :data="categories" stripe empty-text="暂无分类">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="名称" min-width="200" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑分类' : '新建分类'"
      width="420px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="72px" @submit.prevent>
        <el-form-item label="名称" prop="name">
          <el-input
            v-model="form.name"
            maxlength="64"
            show-word-limit
            placeholder="分类名称"
            @keyup.enter="submitForm"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.admin-panel {
  background: #fff;
  border: 1px solid #e6ebef;
  border-radius: 8px;
  padding: 20px;
}

.toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

h1 {
  margin: 0;
  font-size: 1.35rem;
}

.sub {
  margin: 6px 0 0;
  color: #636e72;
  font-size: 0.9rem;
}
</style>
