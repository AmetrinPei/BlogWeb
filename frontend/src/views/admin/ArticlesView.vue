<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createAdminArticle,
  deleteAdminArticle,
  fetchAdminArticle,
  fetchAdminArticles,
  updateAdminArticle,
} from '@/api/adminArticles'
import { uploadMedia } from '@/api/adminMedia'
import { fetchCategories } from '@/api/categories'
import { fetchTags } from '@/api/tags'
import { formatDateTime } from '@/utils/format'
import { isAdmin } from '@/utils/auth'

const STATUS_OPTIONS = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '已下线', value: 'OFFLINE' },
]

const loading = ref(false)
const saving = ref(false)
const articles = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)

const categories = ref([])
const tags = ref([])

const dialogVisible = ref(false)
const editingId = ref(null)
const isEdit = computed(() => editingId.value != null)
const coverUploading = ref(false)
const contentUploading = ref(false)
const contentTextareaRef = ref()

const formRef = ref()
const form = reactive({
  title: '',
  content: '',
  summary: '',
  coverUrl: '',
  status: 'DRAFT',
  pinned: false,
  recommended: false,
  categoryId: null,
  tagIds: [],
  publishedAt: '',
})

const rules = {
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { max: 200, message: '标题不超过 200 字', trigger: 'blur' },
  ],
  content: [{ required: true, message: '请输入正文', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
}

const UPLOAD_HINT = '支持 jpg/png/gif/webp，最大 8MB'

async function onCoverUpload({ file }) {
  coverUploading.value = true
  try {
    const data = await uploadMedia(file)
    form.coverUrl = data.url
    ElMessage.success('封面已上传')
  } catch (e) {
    ElMessage.error(e.message || '上传失败')
  } finally {
    coverUploading.value = false
  }
}

async function onInsertContentImage({ file }) {
  contentUploading.value = true
  try {
    const data = await uploadMedia(file)
    const snippet = `\n![image](${data.url})\n`
    const el = contentTextareaRef.value?.textarea || contentTextareaRef.value?.$el?.querySelector?.('textarea')
    if (el && typeof el.selectionStart === 'number') {
      const start = el.selectionStart
      const end = el.selectionEnd
      form.content = form.content.slice(0, start) + snippet + form.content.slice(end)
      requestAnimationFrame(() => {
        const pos = start + snippet.length
        el.focus()
        el.setSelectionRange(pos, pos)
      })
    } else {
      form.content = (form.content || '') + snippet
    }
    ElMessage.success('已插入图片')
  } catch (e) {
    ElMessage.error(e.message || '上传失败')
  } finally {
    contentUploading.value = false
  }
}

function statusLabel(status) {
  return STATUS_OPTIONS.find((o) => o.value === status)?.label || status || '—'
}

function resetForm() {
  form.title = ''
  form.content = ''
  form.summary = ''
  form.coverUrl = ''
  form.status = 'DRAFT'
  form.pinned = false
  form.recommended = false
  form.categoryId = null
  form.tagIds = []
  form.publishedAt = ''
  formRef.value?.clearValidate()
}

async function loadOptions() {
  const [cats, tgs] = await Promise.all([fetchCategories(), fetchTags()])
  categories.value = cats ?? []
  tags.value = tgs ?? []
}

async function loadList() {
  loading.value = true
  try {
    const data = await fetchAdminArticles({ page: page.value, size: size.value })
    articles.value = data?.items ?? []
    total.value = data?.total ?? 0
  } catch (e) {
    ElMessage.error(e.message || '加载文章失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

async function openEdit(row) {
  editingId.value = row.id
  resetForm()
  dialogVisible.value = true
  saving.value = true
  try {
    const detail = await fetchAdminArticle(row.id)
    form.title = detail.title || ''
    form.content = detail.content || ''
    form.summary = detail.summary || ''
    form.coverUrl = detail.coverUrl || ''
    form.status = detail.status || 'DRAFT'
    form.pinned = Boolean(detail.pinned)
    form.recommended = Boolean(detail.recommended)
    form.categoryId = detail.category?.id ?? null
    form.tagIds = (detail.tags || []).map((t) => t.id)
    form.publishedAt = detail.publishedAt
      ? String(detail.publishedAt).slice(0, 19)
      : ''
  } catch (e) {
    dialogVisible.value = false
    ElMessage.error(e.message || '加载详情失败')
  } finally {
    saving.value = false
  }
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  const payload = {
    title: form.title.trim(),
    content: form.content,
    summary: form.summary.trim() || undefined,
    coverUrl: form.coverUrl.trim() || undefined,
    status: form.status,
    pinned: form.pinned,
    recommended: form.recommended,
    categoryId: form.categoryId,
    tagIds: form.tagIds || [],
  }
  if (form.publishedAt) {
    payload.publishedAt = form.publishedAt
  }

  saving.value = true
  try {
    if (isEdit.value) {
      await updateAdminArticle(editingId.value, payload)
      ElMessage.success('已更新')
    } else {
      await createAdminArticle(payload)
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
    await ElMessageBox.confirm(`确定删除「${row.title}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }

  try {
    await deleteAdminArticle(row.id)
    ElMessage.success('已删除')
    if (articles.value.length === 1 && page.value > 1) {
      page.value -= 1
    }
    await loadList()
  } catch (e) {
    ElMessage.error(e.message || '删除失败')
  }
}

function onPageChange(p) {
  page.value = p
  loadList()
}

function onSizeChange(s) {
  size.value = s
  page.value = 1
  loadList()
}

function tagNames(row) {
  return (row.tags || []).map((t) => t.name).join('、') || '—'
}

onMounted(async () => {
  try {
    await loadOptions()
  } catch (e) {
    ElMessage.error(e.message || '加载分类/标签失败')
  }
  await loadList()
})
</script>

<template>
  <section class="admin-articles">
    <header class="toolbar">
      <div>
        <h1>文章管理</h1>
        <p class="sub">
          {{
            isAdmin()
              ? '管理员可管理全部文章；支持分类与多标签'
              : '仅显示、编辑、删除你自己发布的文章'
          }}
        </p>
      </div>
      <el-button type="primary" @click="openCreate">新建文章</el-button>
    </header>

    <el-table v-loading="loading" :data="articles" stripe empty-text="暂无文章">
      <el-table-column prop="id" label="ID" width="72" />
      <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          {{ statusLabel(row.status) }}
        </template>
      </el-table-column>
      <el-table-column label="分类" width="100">
        <template #default="{ row }">
          {{ row.category?.name || '—' }}
        </template>
      </el-table-column>
      <el-table-column label="标签" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">
          {{ tagNames(row) }}
        </template>
      </el-table-column>
      <el-table-column label="阅读" width="80" align="center">
        <template #default="{ row }">
          {{ row.viewCount ?? 0 }}
        </template>
      </el-table-column>
      <el-table-column label="发布时间" width="170">
        <template #default="{ row }">
          {{ formatDateTime(row.publishedAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        :current-page="page"
        :page-size="size"
        :page-sizes="[10, 20, 50]"
        @current-change="onPageChange"
        @size-change="onSizeChange"
      />
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑文章' : '新建文章'"
      width="820px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="88px"
        v-loading="saving && isEdit && !form.title"
      >
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option
              v-for="opt in STATUS_OPTIONS"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="封面">
          <div class="cover-row">
            <el-input v-model="form.coverUrl" placeholder="https://... 或上传后自动填入" />
            <el-upload
              :show-file-list="false"
              :http-request="onCoverUpload"
              accept="image/jpeg,image/png,image/gif,image/webp,.jpg,.jpeg,.png,.gif,.webp"
            >
              <el-button :loading="coverUploading">上传封面</el-button>
            </el-upload>
          </div>
          <p class="field-hint">{{ UPLOAD_HINT }}；也可继续手填外链</p>
          <img
            v-if="form.coverUrl"
            class="cover-preview"
            :src="form.coverUrl"
            alt="封面预览"
          />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input
            v-model="form.summary"
            type="textarea"
            :rows="2"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select
            v-model="form.categoryId"
            placeholder="选择分类"
            style="width: 100%"
          >
            <el-option
              v-for="c in categories"
              :key="c.id"
              :label="c.name"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="标签">
          <el-select
            v-model="form.tagIds"
            multiple
            collapse-tags
            collapse-tags-tooltip
            placeholder="可多选"
            style="width: 100%"
          >
            <el-option
              v-for="t in tags"
              :key="t.id"
              :label="t.name"
              :value="t.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="置顶">
          <el-switch v-model="form.pinned" />
        </el-form-item>
        <el-form-item label="精选">
          <el-switch v-model="form.recommended" />
        </el-form-item>
        <el-form-item label="发布时间">
          <el-date-picker
            v-model="form.publishedAt"
            type="datetime"
            placeholder="默认当前时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="正文" prop="content">
          <div class="content-toolbar">
            <el-upload
              :show-file-list="false"
              :http-request="onInsertContentImage"
              accept="image/jpeg,image/png,image/gif,image/webp,.jpg,.jpeg,.png,.gif,.webp"
            >
              <el-button size="small" :loading="contentUploading">插入图片</el-button>
            </el-upload>
            <span class="field-hint">{{ UPLOAD_HINT }}</span>
          </div>
          <el-input
            ref="contentTextareaRef"
            v-model="form.content"
            type="textarea"
            :rows="12"
            placeholder="Markdown 正文"
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
.admin-articles {
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

.cover-row {
  display: flex;
  gap: 8px;
  width: 100%;
  align-items: center;
}

.cover-row .el-input {
  flex: 1;
}

.field-hint {
  margin: 6px 0 0;
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}

.cover-preview {
  display: block;
  margin-top: 8px;
  max-width: 240px;
  max-height: 140px;
  object-fit: cover;
  border-radius: 4px;
  border: 1px solid #e6ebef;
}

.content-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
  width: 100%;
}

.content-toolbar .field-hint {
  margin: 0;
}

.sub {
  margin: 6px 0 0;
  color: #636e72;
  font-size: 0.9rem;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
