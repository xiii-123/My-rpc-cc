<template>
  <div id="app">
    <el-container>
      <el-header class="header">
        <h1>yu-rpc ETCD Management</h1>
        <p>ETCD键值对管理界面 - 动态配置中心</p>
      </el-header>
      <el-main>
        <EtcdManager />
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import EtcdManager from './components/EtcdManager.vue'
</script>

<style>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #2c3e50;
  min-height: 100vh;
}

.header {
  background-color: #409EFF;
  color: white;
  text-align: center;
  padding: 20px;
}

.header h1 {
  margin: 0;
  font-size: 28px;
}

.header p {
  margin: 5px 0 0 0;
  font-size: 14px;
  opacity: 0.8;
}
</style>