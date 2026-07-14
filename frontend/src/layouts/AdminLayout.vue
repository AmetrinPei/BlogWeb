<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { clearAuth, getUser, isAdmin } from '@/utils/auth'

const router = useRouter()
const user = computed(() => getUser())

function logout() {
  clearAuth()
  router.push({ name: 'admin-login' })
}
</script>

<template>
  <div class="admin-layout">
    <header class="admin-bar">
      <RouterLink class="brand" to="/admin/articles">管理后台</RouterLink>
      <nav class="links">
        <RouterLink to="/admin/articles">文章</RouterLink>
        <template v-if="isAdmin()">
          <RouterLink to="/admin/categories">分类</RouterLink>
          <RouterLink to="/admin/tags">标签</RouterLink>
          <RouterLink to="/admin/site">站点</RouterLink>
        </template>
        <span v-if="user?.username" class="username">{{ user.username }}</span>
        <RouterLink to="/">回访客站</RouterLink>
        <button class="logout" type="button" @click="logout">退出</button>
      </nav>
    </header>
    <main class="admin-main">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.admin-layout {
  min-height: 100vh;
  background: #f5f7f8;
  color: #2d3436;
  font-family: system-ui, -apple-system, 'Segoe UI', sans-serif;
}

.admin-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 20px;
  background: #ffffff;
  border-bottom: 1px solid #e6ebef;
}

.brand {
  font-weight: 700;
  text-decoration: none;
  color: #2d3436;
}

.brand::before {
  content: '';
  display: inline-block;
  width: 8px;
  height: 8px;
  margin-right: 8px;
  border-radius: 50%;
  background: #6fcf97;
  vertical-align: middle;
}

.links {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 14px;
}

.links a {
  text-decoration: none;
  color: #636e72;
  font-size: 0.92rem;
}

.links a.router-link-active {
  color: #2d3436;
  font-weight: 600;
}

.username {
  color: #636e72;
  font-size: 0.88rem;
  padding: 0 4px;
}

.logout {
  min-height: 30px;
  padding: 0 12px;
  border: 1px solid #dfe6e9;
  border-radius: 6px;
  background: #fff;
  color: #636e72;
  font-size: 0.88rem;
  cursor: pointer;
}

.logout:hover {
  color: #2d3436;
  border-color: #b2bec3;
}

.admin-main {
  max-width: 1100px;
  margin: 0 auto;
  padding: 24px 16px 40px;
}

@media (max-width: 640px) {
  .admin-bar {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
