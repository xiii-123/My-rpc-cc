<template>
  <div class="etcd-manager">
    <!-- 顶部工具栏 -->
    <el-card class="toolbar">
      <el-row :gutter="20">
        <el-col :span="8">
          <el-input
            v-model="keyPrefix"
            placeholder="键前缀 (如: /rpc)"
            @keyup.enter="loadKeys"
          >
            <template #prepend>前缀</template>
          </el-input>
        </el-col>
        <el-col :span="4">
          <el-button type="primary" @click="loadKeys" :loading="loading">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
        </el-col>
        <el-col :span="4">
          <el-button @click="refreshKeys">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </el-col>
        <el-col :span="4">
          <el-button type="success" @click="showCreateDialog">
            <el-icon><Plus /></el-icon>
            新增
          </el-button>
        </el-col>
        <el-col :span="4">
          <el-button type="danger" @click="deleteBatchKeys" :disabled="selectedKeys.length === 0">
            <el-icon><Delete /></el-icon>
            批量删除
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 统计信息 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-number">{{ stats.totalKeys }}</div>
          <div class="stat-label">总键数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-number strategy">{{ stats.strategyKeys }}</div>
          <div class="stat-label">策略配置</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-number metrics">{{ stats.metricsKeys }}</div>
          <div class="stat-label">监控指标</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-number registry">{{ stats.registryKeys }}</div>
          <div class="stat-label">服务注册</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 键值列表 -->
    <el-card class="key-value-list">
      <template #header>
        <div class="list-header">
          <span>ETCD键值对列表</span>
          <el-tag v-if="keyValuePairs.length > 0" type="info">
            共 {{ keyValuePairs.length }} 条记录
          </el-tag>
        </div>
      </template>

      <el-table
        :data="keyValuePairs"
        style="width: 100%"
        v-loading="loading"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="key" label="键" min-width="300">
          <template #default="scope">
            <el-tag type="info" class="key-tag">{{ scope.row.key }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="value" label="值" min-width="200">
          <template #default="scope">
            <el-input
              v-if="scope.row.editing"
              v-model="scope.row.tempValue"
              size="small"
              @blur="finishEdit(scope.row)"
              @keyup.enter="finishEdit(scope.row)"
            />
            <span v-else class="value-text">{{ scope.row.value }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="120">
          <template #default="scope">
            <el-tag :type="getTypeColor(scope.row.type)">{{ scope.row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="80" />
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button size="small" @click="editKeyValue(scope.row)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button size="small" type="danger" @click="deleteKeyValue(scope.row)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form :model="formData" label-width="80px">
        <el-form-item label="键">
          <el-input
            v-model="formData.key"
            :disabled="editMode"
            placeholder="请输入键名"
          />
        </el-form-item>
        <el-form-item label="值">
          <el-input
            v-model="formData.value"
            type="textarea"
            :rows="4"
            placeholder="请输入值"
          />
        </el-form-item>
        <el-form-item v-if="!editMode" label="键类型">
          <el-select v-model="formData.type" placeholder="选择类型">
            <el-option label="负载均衡配置" value="loadbalance" />
            <el-option label="重试策略配置" value="retry" />
            <el-option label="容错策略配置" value="tolerant" />
            <el-option label="节点权重配置" value="weight" />
            <el-option label="监控指标" value="metrics" />
            <el-option label="其他配置" value="other" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveKeyValue" :loading="saving">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Delete, Edit } from '@element-plus/icons-vue'
import axios from 'axios'

// 响应式数据
const keyPrefix = ref('/rpc')
const keyValuePairs = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editMode = ref(false)
const selectedKeys = ref([])

// 表单数据
const formData = reactive({
  key: '',
  value: '',
  type: 'other'
})

// 统计数据
const stats = reactive({
  totalKeys: 0,
  strategyKeys: 0,
  metricsKeys: 0,
  registryKeys: 0
})

const dialogTitle = ref('新增键值对')

// 方法
const loadKeys = async () => {
  if (!keyPrefix.value.trim()) {
    ElMessage.warning('请输入键前缀')
    return
  }

  loading.value = true
  try {
    const response = await axios.get(`/api/etcd/keys?prefix=${encodeURIComponent(keyPrefix.value)}`)
    keyValuePairs.value = response.data.map(item => ({
      ...item,
      type: detectType(item.key),
      editing: false,
      tempValue: item.value
    }))
    updateStats()
    ElMessage.success(`成功加载 ${keyValuePairs.value.length} 条记录`)
  } catch (error) {
    console.error('加载键失败:', error)
    ElMessage.error('加载键值对失败: ' + (error.response?.data?.message || error.message))
  } finally {
    loading.value = false
  }
}

const refreshKeys = () => {
  loadKeys()
}

const detectType = (key) => {
  if (key.includes('/strategy/loadbalance')) return '负载均衡'
  if (key.includes('/strategy/retry')) return '重试策略'
  if (key.includes('/strategy/tolerant')) return '容错策略'
  if (key.includes('/strategy/weight')) return '节点权重'
  if (key.includes('/metrics/')) return '监控指标'
  if (key.includes('/rpc/') && !key.includes('/strategy/') && !key.includes('/metrics/')) return '服务注册'
  return '其他配置'
}

const getTypeColor = (type) => {
  const colors = {
    '负载均衡': 'primary',
    '重试策略': 'success',
    '容错策略': 'warning',
    '节点权重': 'info',
    '监控指标': 'danger',
    '服务注册': '',
    '其他配置': 'info'
  }
  return colors[type] || 'info'
}

const showCreateDialog = () => {
  editMode.value = false
  dialogTitle.value = '新增键值对'
  formData.key = ''
  formData.value = ''
  formData.type = 'other'
  dialogVisible.value = true
}

const editKeyValue = (row) => {
  editMode.value = true
  dialogTitle.value = '编辑键值对'
  formData.key = row.key
  formData.value = row.value
  formData.type = row.type
  dialogVisible.value = true
}

const saveKeyValue = async () => {
  if (!formData.key.trim()) {
    ElMessage.warning('请输入键名')
    return
  }
  if (!formData.value.trim()) {
    ElMessage.warning('请输入值')
    return
  }

  saving.value = true
  try {
    if (editMode.value) {
      await axios.put('/api/etcd/key', formData)
      ElMessage.success('更新成功')
    } else {
      await axios.post('/api/etcd/key', formData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadKeys()
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败: ' + (error.response?.data?.message || error.message))
  } finally {
    saving.value = false
  }
}

const deleteKeyValue = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除键 "${row.key}" 吗？`, '确认删除', {
      type: 'warning'
    })

    await axios.delete(`/api/etcd/key?key=${encodeURIComponent(row.key)}`)
    ElMessage.success('删除成功')
    loadKeys()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败: ' + (error.response?.data?.message || error.message))
    }
  }
}

const deleteBatchKeys = async () => {
  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${selectedKeys.value.length} 个键吗？`, '确认批量删除', {
      type: 'warning'
    })

    loading.value = true
    for (const key of selectedKeys.value) {
      await axios.delete(`/api/etcd/key?key=${encodeURIComponent(key)}`)
    }
    ElMessage.success(`成功删除 ${selectedKeys.value.length} 个键`)
    selectedKeys.value = []
    loadKeys()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除失败:', error)
      ElMessage.error('批量删除失败: ' + (error.response?.data?.message || error.message))
    }
  } finally {
    loading.value = false
  }
}

const handleSelectionChange = (selection) => {
  selectedKeys.value = selection.map(item => item.key)
}

const finishEdit = (row) => {
  row.editing = false
  if (row.tempValue !== row.value) {
    saveInlineEdit(row)
  }
}

const saveInlineEdit = async (row) => {
  try {
    await axios.put('/api/etcd/key', {
      key: row.key,
      value: row.tempValue
    })
    row.value = row.tempValue
    ElMessage.success('编辑成功')
  } catch (error) {
    console.error('内联编辑失败:', error)
    ElMessage.error('编辑失败: ' + (error.response?.data?.message || error.message))
    row.tempValue = row.value
  }
}

const updateStats = () => {
  stats.totalKeys = keyValuePairs.value.length
  stats.strategyKeys = keyValuePairs.value.filter(item => item.type.includes('策略') || item.type === '节点权重').length
  stats.metricsKeys = keyValuePairs.value.filter(item => item.type === '监控指标').length
  stats.registryKeys = keyValuePairs.value.filter(item => item.type === '服务注册').length
}

// 生命周期
onMounted(() => {
  loadKeys()
})
</script>

<style scoped>
.etcd-manager {
  padding: 20px;
}

.toolbar {
  margin-bottom: 20px;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
  padding: 20px;
}

.stat-number {
  font-size: 32px;
  font-weight: bold;
  color: #409EFF;
}

.stat-number.strategy {
  color: #67C23A;
}

.stat-number.metrics {
  color: #E6A23C;
}

.stat-number.registry {
  color: #F56C6C;
}

.stat-label {
  margin-top: 8px;
  color: #909399;
  font-size: 14px;
}

.key-value-list {
  min-height: 400px;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.key-tag {
  font-family: 'Courier New', monospace;
  word-break: break-all;
}

.value-text {
  word-break: break-all;
  max-width: 200px;
  display: inline-block;
}
</style>