<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchAdminComments, updateCommentStatus } from '@/api/adminComments'

const loading = ref(false)
const actingId = ref(null)
const items = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const statusFilter = ref('PENDING')

async function loadList() {
  loading.value = true
  try {
    const data = await fetchAdminComments({
      status: statusFilter.value,
      page: page.value,
      size: size.value,
    })
    items.value = data?.items ?? []
    total.value = data?.total ?? 0
  } catch (e) {
    ElMessage.error(e.message || '加载评论失败')
  } finally {
    loading.value = false
  }
}

async function setStatus(row, status) {
  actingId.value = row.id
  try {
    await updateCommentStatus(row.id, status)
    ElMessage.success(status === 'APPROVED' ? '已通过' : '已拒绝')
    await loadList()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    actingId.value = null
  }
}

function onFilterChange() {
  page.value = 1
  loadList()
}

onMounted(loadList)
</script>

<template>
  <section class="admin-panel">
    <header class="toolbar">
      <div>
        <h1>评论审核</h1>
        <p class="sub">审核待审评论；通过后对访客可见，拒绝后保持隐藏</p>
      </div>
      <el-select v-model="statusFilter" style="width: 140px" @change="onFilterChange">
        <el-option label="待审" value="PENDING" />
        <el-option label="已通过" value="APPROVED" />
        <el-option label="已拒绝" value="REJECTED" />
      </el-select>
    </header>

    <el-table v-loading="loading" :data="items" stripe empty-text="暂无评论">
      <el-table-column prop="id" label="ID" width="72" />
      <el-table-column prop="articleId" label="文章" width="88" />
      <el-table-column prop="username" label="作者" width="120" />
      <el-table-column prop="content" label="内容" min-width="240" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="100" />
      <el-table-column prop="createdAt" label="时间" width="180" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            :loading="actingId === row.id"
            :disabled="row.status === 'APPROVED'"
            @click="setStatus(row, 'APPROVED')"
          >
            通过
          </el-button>
          <el-button
            link
            type="danger"
            :loading="actingId === row.id"
            :disabled="row.status === 'REJECTED'"
            @click="setStatus(row, 'REJECTED')"
          >
            拒绝
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        layout="total, prev, pager, next"
        :total="total"
        @current-change="loadList"
      />
    </div>
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

.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
