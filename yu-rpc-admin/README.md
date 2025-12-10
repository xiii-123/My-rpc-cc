# yu-rpc ETCD 配置中心管理界面

基于ETCD键值对的yu-rpc动态配置中心，支持节点级策略配置和监控指标管理。

## 功能特性

- 🔧 **动态策略配置**：支持负载均衡、重试、容错策略的动态配置
- 📊 **监控指标管理**：实时查看服务调用量、成功率、平均耗时等指标
- 🎯 **节点级配置**：每个服务节点都可以独立配置策略
- 🌐 **Web管理界面**：直观的键值对管理界面
- 🔄 **实时更新**：配置变更后自动生效

## 项目结构

```
yu-rpc-admin/
├── src/                         # 后端管理API
│   ├── main/java/
│   │   └── com/yupi/yurpc/admin/
│   │       ├── controller/       # ETCD管理控制器
│   │       ├── dto/             # 数据传输对象
│   │       └── YurpcAdminApplication.java  # 启动类
│   └── main/resources/
│       └── application.properties
├── frontend/                     # 前端管理界面
│   ├── src/
│   │   ├── components/          # Vue组件
│   │   │   └── EtcdManager.vue  # 主要管理组件
│   │   ├── App.vue             # 主应用组件
│   │   └── main.js             # 入口文件
│   ├── index.html
│   ├── package.json
│   └── vite.config.js
├── pom.xml                       # Maven配置文件
└── README.md                    # 文档
```

## 快速开始

### 1. 启动ETCD服务

```bash
# 使用Docker启动ETCD
docker run -d --name etcd-server \
    --publish 2379:2379 \
    --publish 2380:2380 \
    --env ALLOW_NONE_AUTHENTICATION=yes \
    --env ETCD_ADVERTISE_CLIENT_URLS=http://0.0.0.0:2379 \
    quay.io/coreos/etcd:v3.5.0
```

### 2. 启动后端API

```bash
cd yu-rpc-admin
mvn spring-boot:run
```

后端API将在 `http://localhost:8080` 启动

### 3. 启动前端界面

```bash
cd yu-rpc-admin/frontend
npm install
npm run dev
```

前端界面将在 `http://localhost:3000` 启动

## ETCD键值结构

### 服务注册
```
/rpc/UserService:1.0/localhost:8080  -> ServiceMetaInfo JSON
```

### 策略配置
```
/rpc/strategy/UserService:1.0/localhost:8080/loadbalance     -> "round_robin"
/rpc/strategy/UserService:1.0/localhost:8080/retry           -> "fixed_interval"
/rpc/strategy/UserService:1.0/localhost:8080/tolerant        -> "fail_fast"
/rpc/strategy/UserService:1.0/localhost:8080/weight          -> "5"
```

### 监控指标
```
/rpc/metrics/UserService:1.0/localhost:8080/calls            -> "1000"
/rpc/metrics/UserService:1.0/localhost:8080/success          -> "950"
/rpc/metrics/UserService:1.0/localhost:8080/failure          -> "50"
/rpc/metrics/UserService:1.0/localhost:8080/avg_time         -> "150"
```

## 使用方式

### 1. 配置负载均衡策略

1. 打开前端管理界面：http://localhost:3000
2. 在键前缀输入框中输入：`/rpc/strategy`
3. 点击"查询"按钮
4. 找到目标服务的负载均衡配置键
5. 点击"编辑"，修改值：
   - `round_robin`：轮询
   - `random`：随机
   - `consistent_hash`：一致性哈希

### 2. 设置节点权重

1. 创建新键：`/rpc/strategy/UserService:1.0/localhost:8080/weight`
2. 设置值为：`10`（高性能节点）
3. 另一个节点设置：`5`（普通性能节点）
4. 配合加权轮询策略使用

### 3. 查看监控指标

1. 在键前缀输入框中输入：`/rpc/metrics`
2. 点击"查询"查看所有监控数据
3. 监控指标会实时更新，包括：
   - calls：总调用次数
   - success：成功次数
   - failure：失败次数
   - avg_time：平均耗时

## API接口

### 键值对管理

```http
# 查询键值对
GET /api/etcd/keys?prefix=/rpc

# 获取单个键值对
GET /api/etcd/key?key=/rpc/strategy/UserService:1.0/localhost:8080/loadbalance

# 创建/更新键值对
POST /api/etcd/key
PUT /api/etcd/key
Content-Type: application/json
{
  "key": "/rpc/strategy/UserService:1.0/localhost:8080/loadbalance",
  "value": "random"
}

# 删除键值对
DELETE /api/etcd/key?key=/rpc/strategy/UserService:1.0/localhost:8080/loadbalance
```

### 策略配置

```http
# 发布节点策略
POST /api/etcd/strategy/UserService/1.0/localhost/8080?strategyType=loadbalance&value=random

# 获取节点策略
GET /api/etcd/strategy/UserService/1.0/localhost/8080?strategyType=loadbalance
```

## 工作原理

### 动态策略获取

1. **客户端请求**：ServiceProxy在每次RPC调用前，从ETCD获取最新策略配置
2. **优先级机制**：ETCD配置 > 全局配置 > 默认值
3. **实时生效**：配置更新后，下一次RPC调用立即使用新策略

### 监控指标收集

1. **自动记录**：每次RPC调用后自动记录指标到ETCD
2. **增量更新**：调用次数、成功/失败次数使用增量更新
3. **异步处理**：监控记录失败不影响主业务流程

## 注意事项

1. **ETCD连接**：确保ETCD服务正常运行且可访问
2. **键命名**：遵循预定义的键命名规范
3. **配置格式**：策略值必须是有效的枚举值
4. **性能影响**：每次RPC调用都会查询ETCD，建议在生产环境中增加本地缓存

## 扩展功能

- [ ] 配置变更监听和实时推送
- [ ] 配置版本管理和回滚
- [ ] 更丰富的监控图表
- [ ] 配置模板功能
- [ ] 批量操作优化

## 故障排查

### 1. 无法连接ETCD
- 检查ETCD服务是否启动
- 确认连接地址和端口正确

### 2. 配置不生效
- 检查键名是否正确
- 确认策略值是否有效
- 查看客户端日志中的策略获取信息

### 3. 监控数据不更新
- 确认客户端使用了更新后的代码
- 检查ETCD写入权限
- 查看是否有错误日志

## 技术栈

- **后端**：Spring Boot, yu-rpc-core
- **前端**：Vue.js 3, Element Plus, Vite
- **存储**：ETCD v3
- **通信**：RESTful API