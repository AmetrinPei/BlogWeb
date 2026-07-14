<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchSite, updateSite } from '@/api/site'

const loading = ref(false)
const saving = ref(false)
const formRef = ref()

const form = reactive({
  siteName: '',
  tagline: '',
  aboutText: '',
  socialLinks: [{ name: '', url: '' }],
})

const rules = {
  siteName: [{ required: true, message: '请输入站点名称', trigger: 'blur' }],
}

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

async function loadSite() {
  loading.value = true
  try {
    const data = await fetchSite()
    form.siteName = data?.siteName || ''
    form.tagline = data?.tagline || ''
    form.aboutText = data?.aboutText || ''
    const links = data?.socialLinks || []
    form.socialLinks = links.length
      ? links.map((l) => ({ name: l.name || '', url: l.url || '' }))
      : [{ name: '', url: '' }]
  } catch (e) {
    ElMessage.error(e.message || '加载站点配置失败')
  } finally {
    loading.value = false
  }
}

async function onSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  const payload = {
    siteName: form.siteName.trim(),
    tagline: form.tagline.trim(),
    aboutText: form.aboutText,
    socialLinks: form.socialLinks
      .filter((l) => l.name?.trim() || l.url?.trim())
      .map((l) => ({
        name: l.name.trim(),
        url: l.url.trim(),
      })),
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
        <p class="sub">配置访客端站点名称、标语、关于文案与社交链接</p>
      </div>
    </header>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="站点名称" prop="siteName">
        <el-input v-model="form.siteName" maxlength="100" show-word-limit />
      </el-form-item>

      <el-form-item label="标语">
        <el-input v-model="form.tagline" maxlength="200" show-word-limit />
      </el-form-item>

      <el-form-item label="关于文案">
        <el-input v-model="form.aboutText" type="textarea" :rows="5" />
      </el-form-item>

      <el-form-item label="社交链接">
        <div class="social-list">
          <div
            v-for="(link, index) in form.socialLinks"
            :key="index"
            class="social-row"
          >
            <el-input v-model="link.name" placeholder="名称" />
            <el-input v-model="link.url" placeholder="URL" />
            <el-button type="danger" link @click="removeSocialLink(index)">删除</el-button>
          </div>
          <el-button type="primary" link @click="addSocialLink">+ 添加链接</el-button>
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
  padding: 20px;
}

.toolbar {
  margin-bottom: 20px;
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

.social-list {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.social-row {
  display: grid;
  grid-template-columns: 1fr 2fr auto;
  gap: 10px;
  align-items: center;
}

@media (max-width: 640px) {
  .social-row {
    grid-template-columns: 1fr;
  }
}
</style>
