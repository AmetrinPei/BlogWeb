<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createSensitiveWord,
  deleteSensitiveWord,
  fetchSensitiveWords,
} from '@/api/adminSensitiveWords'

const loading = ref(false)
const saving = ref(false)
const words = ref([])

const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({
  word: '',
})

const rules = {
  word: [
    { required: true, message: '请输入敏感词', trigger: 'blur' },
    { max: 64, message: '不超过 64 字', trigger: 'blur' },
  ],
}

async function loadList() {
  loading.value = true
  try {
    words.value = (await fetchSensitiveWords()) ?? []
  } catch (e) {
    ElMessage.error(e.message || '加载敏感词失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.word = ''
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    await createSensitiveWord({ word: form.word.trim() })
    ElMessage.success('已添加')
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
    await ElMessageBox.confirm(`确定删除敏感词「${row.word}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }

  try {
    await deleteSensitiveWord(row.id)
    ElMessage.success('已删除')
    await loadList()
  } catch (e) {
    ElMessage.error(e.message || '删除失败')
  }
}

onMounted(loadList)
</script>

<template>
  <section class="admin-panel">
    <header class="toolbar">
      <div>
        <h1>敏感词</h1>
        <p class="sub">命中词表的评论进入待审，不会直接出现在前台</p>
      </div>
      <el-button type="primary" @click="openCreate">添加敏感词</el-button>
    </header>

    <el-table v-loading="loading" :data="words" stripe empty-text="暂无敏感词">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="word" label="词条" min-width="200" />
      <el-table-column prop="createdAt" label="创建时间" width="180" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="添加敏感词" width="420px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="72px" @submit.prevent>
        <el-form-item label="词条" prop="word">
          <el-input
            v-model="form.word"
            maxlength="64"
            show-word-limit
            placeholder="将按小写子串匹配"
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
