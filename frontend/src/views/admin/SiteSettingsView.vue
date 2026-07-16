<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchSite, updateSite } from '@/api/site'
import { uploadMedia } from '@/api/adminMedia'

const loading = ref(false)
const saving = ref(false)
const uploading = ref('')
const formRef = ref()

const form = reactive({
  siteName: '',
  tagline: '',
  aboutTitle: '',
  aboutDisplayName: '',
  aboutText: '',
  aboutHighlights: [''],
  socialLinks: [{ name: '', url: '' }],
  friendLinks: [{ name: '', url: '', description: '' }],
  defaultTheme: 'light',
  backgroundMode: 'theme',
  backgroundColor: '#E8F6EE',
  backgroundGradient: 'mint-wash',
  backgroundImageUrl: '',
  aboutAvatarUrl: '',
  homeHeroUrl: '',
})

const rules = {
  siteName: [{ required: true, message: '请输入站点名称', trigger: 'blur' }],
}

const showColor = computed(() => form.backgroundMode === 'color')
const showGradient = computed(() => form.backgroundMode === 'gradient')
const showImage = computed(() => form.backgroundMode === 'image')

function addSocialLink() {
  form.socialLinks.push({ name: '', url: '' })
}

function removeSocialLink(index) {
  if (form.socialLinks.length <= 1) {
    form.socialLinks[0] = { name: '', url: '' }
    return
  }
  form.socialLinks.splice(index, 1)
}

function addHighlight() {
  if (form.aboutHighlights.length >= 10) {
    ElMessage.warning('亮点最多 10 条')
    return
  }
  form.aboutHighlights.push('')
}

function removeHighlight(index) {
  if (form.aboutHighlights.length <= 1) {
    form.aboutHighlights[0] = ''
    return
  }
  form.aboutHighlights.splice(index, 1)
}

function addFriendLink() {
  if (form.friendLinks.length >= 50) {
    ElMessage.warning('友链最多 50 条')
    return
  }
  form.friendLinks.push({ name: '', url: '', description: '' })
}

function removeFriendLink(index) {
  if (form.friendLinks.length <= 1) {
    form.friendLinks[0] = { name: '', url: '', description: '' }
    return
  }
  form.friendLinks.splice(index, 1)
}

function moveFriendLink(index, delta) {
  const next = index + delta
  if (next < 0 || next >= form.friendLinks.length) return
  const list = form.friendLinks
  const tmp = list[index]
  list[index] = list[next]
  list[next] = tmp
}

async function loadSite() {
  loading.value = true
  try {
    const data = await fetchSite()
    form.siteName = data?.siteName || ''
    form.tagline = data?.tagline || ''
    form.aboutTitle = data?.aboutTitle || ''
    form.aboutDisplayName = data?.aboutDisplayName || ''
    form.aboutText = data?.aboutText || ''
    const highlights = data?.aboutHighlights || []
    form.aboutHighlights = highlights.length ? [...highlights] : ['']
    const links = data?.socialLinks || []
    form.socialLinks = links.length
      ? links.map((l) => ({ name: l.name || '', url: l.url || '' }))
      : [{ name: '', url: '' }]
    const friends = data?.friendLinks || []
    form.friendLinks = friends.length
      ? friends.map((l) => ({
          name: l.name || '',
          url: l.url || '',
          description: l.description || '',
        }))
      : [{ name: '', url: '', description: '' }]
    form.defaultTheme = data?.defaultTheme || 'light'
    form.backgroundMode = data?.backgroundMode || 'theme'
    form.backgroundColor = data?.backgroundColor || '#E8F6EE'
    form.backgroundGradient = data?.backgroundGradient || 'mint-wash'
    form.backgroundImageUrl = data?.backgroundImageUrl || ''
    form.aboutAvatarUrl = data?.aboutAvatarUrl || ''
    form.homeHeroUrl = data?.homeHeroUrl || ''
  } catch (e) {
    ElMessage.error(e.message || '加载站点配置失败')
  } finally {
    loading.value = false
  }
}

async function onUpload(field, file) {
  if (!file) return
  uploading.value = field
  try {
    const data = await uploadMedia(file)
    form[field] = data.url
    ElMessage.success('上传成功')
  } catch (e) {
    ElMessage.error(e.message || '上传失败')
  } finally {
    uploading.value = ''
  }
}

async function onSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  const payload = {
    siteName: form.siteName.trim(),
    tagline: form.tagline.trim(),
    aboutTitle: form.aboutTitle.trim(),
    aboutDisplayName: form.aboutDisplayName.trim(),
    aboutText: form.aboutText,
    aboutHighlights: form.aboutHighlights
      .map((h) => h.trim())
      .filter(Boolean),
    socialLinks: form.socialLinks
      .filter((l) => l.name?.trim() || l.url?.trim())
      .map((l) => ({
        name: l.name.trim(),
        url: l.url.trim(),
      })),
    friendLinks: form.friendLinks
      .filter((l) => l.name?.trim() || l.url?.trim() || l.description?.trim())
      .map((l, index) => ({
        name: l.name.trim(),
        url: l.url.trim(),
        description: (l.description || '').trim(),
        sortOrder: index,
      })),
    defaultTheme: form.defaultTheme,
    backgroundMode: form.backgroundMode,
    backgroundColor: form.backgroundColor || '',
    backgroundGradient: form.backgroundGradient || '',
    backgroundImageUrl: form.backgroundImageUrl.trim(),
    aboutAvatarUrl: form.aboutAvatarUrl.trim(),
    homeHeroUrl: form.homeHeroUrl.trim(),
  }

  saving.value = true
  try {
    await updateSite(payload)
    ElMessage.success('站点配置已保存')
    await loadSite()
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(loadSite)
</script>

<template>
  <section class="admin-site" v-loading="loading">
    <header class="toolbar">
      <div>
        <h1>站点设置</h1>
        <p class="sub">
          配置访客端站点名称、标语、关于页、社交链接、友情链接与主题视觉（友情链接与社交链接相互独立）
        </p>
      </div>
    </header>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
      <el-form-item label="站点名称" prop="siteName">
        <el-input v-model="form.siteName" maxlength="100" show-word-limit />
      </el-form-item>

      <el-form-item label="标语">
        <el-input v-model="form.tagline" maxlength="200" show-word-limit />
      </el-form-item>

      <el-divider content-position="left">关于页</el-divider>

      <el-form-item label="关于标题">
        <el-input v-model="form.aboutTitle" maxlength="100" show-word-limit placeholder="空则前台显示「关于我」" />
      </el-form-item>

      <el-form-item label="展示名">
        <el-input
          v-model="form.aboutDisplayName"
          maxlength="50"
          show-word-limit
          placeholder="空则使用前端默认作者名"
        />
      </el-form-item>

      <el-form-item label="关于文案">
        <el-input v-model="form.aboutText" type="textarea" :rows="5" maxlength="5000" show-word-limit />
      </el-form-item>

      <el-form-item label="亮点列表">
        <div class="social-list">
          <div v-for="(_, index) in form.aboutHighlights" :key="'h-' + index" class="social-row">
            <el-input v-model="form.aboutHighlights[index]" maxlength="100" placeholder="一条亮点" />
            <el-button type="danger" link @click="removeHighlight(index)">删除</el-button>
          </div>
          <el-button type="primary" link @click="addHighlight">+ 添加亮点</el-button>
        </div>
      </el-form-item>

      <el-divider content-position="left">社交链接</el-divider>

      <el-form-item label="社交链接">
        <div class="social-list">
          <div v-for="(link, index) in form.socialLinks" :key="'s-' + index" class="social-row">
            <el-input v-model="link.name" placeholder="名称" />
            <el-input v-model="link.url" placeholder="URL" />
            <el-button type="danger" link @click="removeSocialLink(index)">删除</el-button>
          </div>
          <el-button type="primary" link @click="addSocialLink">+ 添加链接</el-button>
        </div>
      </el-form-item>

      <el-divider content-position="left">友情链接</el-divider>

      <el-form-item label="友情链接">
        <div class="friend-list">
          <div v-for="(link, index) in form.friendLinks" :key="'f-' + index" class="friend-row">
            <el-input v-model="link.name" placeholder="名称" />
            <el-input v-model="link.url" placeholder="https://…" />
            <el-input v-model="link.description" placeholder="简介（可选）" />
            <el-button link :disabled="index === 0" @click="moveFriendLink(index, -1)">上移</el-button>
            <el-button
              link
              :disabled="index === form.friendLinks.length - 1"
              @click="moveFriendLink(index, 1)"
            >
              下移
            </el-button>
            <el-button type="danger" link @click="removeFriendLink(index)">删除</el-button>
          </div>
          <el-button type="primary" link @click="addFriendLink">+ 添加友链</el-button>
        </div>
      </el-form-item>

      <el-divider content-position="left">主题与视觉</el-divider>

      <el-form-item label="默认主题">
        <el-select v-model="form.defaultTheme" style="width: 200px">
          <el-option label="亮色 light" value="light" />
          <el-option label="暗色 dark" value="dark" />
        </el-select>
        <span class="hint">访客无本地偏好时使用</span>
      </el-form-item>

      <el-form-item label="背景模式">
        <el-radio-group v-model="form.backgroundMode">
          <el-radio-button label="theme">theme 氛围</el-radio-button>
          <el-radio-button label="color">color</el-radio-button>
          <el-radio-button label="gradient">gradient</el-radio-button>
          <el-radio-button label="image">image</el-radio-button>
        </el-radio-group>
      </el-form-item>

      <el-form-item v-if="showColor" label="背景色">
        <el-color-picker v-model="form.backgroundColor" color-format="hex" />
        <el-input v-model="form.backgroundColor" style="width: 140px; margin-left: 12px" maxlength="7" />
      </el-form-item>

      <el-form-item v-if="showGradient" label="渐变预设">
        <el-select v-model="form.backgroundGradient" style="width: 220px">
          <el-option label="mint-wash" value="mint-wash" />
          <el-option label="lilac-mist" value="lilac-mist" />
          <el-option label="peach-glow" value="peach-glow" />
        </el-select>
      </el-form-item>

      <el-form-item v-if="showImage" label="背景图 URL">
        <div class="url-row">
          <el-input v-model="form.backgroundImageUrl" placeholder="/uploads/… 或 https://…" />
          <el-upload
            :show-file-list="false"
            accept="image/jpeg,image/png,image/gif,image/webp"
            :http-request="({ file }) => onUpload('backgroundImageUrl', file)"
          >
            <el-button :loading="uploading === 'backgroundImageUrl'">上传</el-button>
          </el-upload>
        </div>
      </el-form-item>

      <el-form-item label="关于页头像">
        <div class="url-row">
          <el-input v-model="form.aboutAvatarUrl" placeholder="空则使用默认头像" />
          <el-upload
            :show-file-list="false"
            accept="image/jpeg,image/png,image/gif,image/webp"
            :http-request="({ file }) => onUpload('aboutAvatarUrl', file)"
          >
            <el-button :loading="uploading === 'aboutAvatarUrl'">上传</el-button>
          </el-upload>
        </div>
      </el-form-item>

      <el-form-item label="首页贴图">
        <div class="url-row">
          <el-input v-model="form.homeHeroUrl" placeholder="空则使用默认分层插画" />
          <el-upload
            :show-file-list="false"
            accept="image/jpeg,image/png,image/gif,image/webp"
            :http-request="({ file }) => onUpload('homeHeroUrl', file)"
          >
            <el-button :loading="uploading === 'homeHeroUrl'">上传</el-button>
          </el-upload>
        </div>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="saving" @click="onSubmit">保存</el-button>
      </el-form-item>
    </el-form>
  </section>
</template>

<style scoped>
.admin-site {
  background: #fff;
  border: 1px solid #e6ebef;
  border-radius: 8px;
  padding: 24px;
}

.toolbar {
  margin-bottom: 20px;
}

.toolbar h1 {
  margin: 0;
  font-size: 1.35rem;
}

.sub {
  margin: 6px 0 0;
  color: #909399;
  font-size: 0.9rem;
}

.social-list,
.friend-list {
  width: 100%;
}

.social-row,
.friend-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;
  align-items: center;
}

.friend-row .el-input {
  flex: 1;
  min-width: 120px;
}

.url-row {
  display: flex;
  gap: 8px;
  width: 100%;
  align-items: center;
}

.hint {
  margin-left: 12px;
  color: #909399;
  font-size: 0.85rem;
}
</style>
