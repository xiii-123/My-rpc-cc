# yu-rpc - 轻量级 RPC 框架

一个基于 Java 的轻量级 RPC 框架，支持服务注册发现、负载均衡、容错重试等企业级特性。

## 🚀 特性

- **简单易用**：注解驱动的服务发布与引用，Spring Boot 无缝集成
- **服务治理**：支持 Etcd、ZooKeeper 服务注册与发现
- **负载均衡**：轮询、随机、一致性哈希等多种负载均衡策略
- **容错重试**：多种重试和容错策略，保障服务稳定性
- **多序列化**：支持 JDK、JSON、Hessian、Kryo 等多种序列化方式
- **协议扩展**：基于 SPI 机制，支持自定义扩展

## 📋 开发路线图

### 🎯 目标特性
1. **动态配置中心** - 分布式配置管理和热更新
2. **限流功能** - 流量控制和系统保护
3. **服务调用链重构** - 优化架构设计，提升可扩展性
4. **服务缓存优化** - 多级缓存，提升性能
5. **异步调用优化** - 提供高性能异步调用选项

### ✅ 已完成功能

#### 核心功能（100%完成）
- **服务注册发现**：
  - ✅ Etcd、ZooKeeper 注册中心支持
  - ✅ 服务健康检查和心跳机制
  - ✅ 自动服务发现和负载均衡
- **负载均衡**：
  - ✅ 轮询、随机、一致性哈希策略
  - ✅ SPI 机制支持自定义策略
- **容错重试**：
  - ✅ 多种重试策略（无重试、固定间隔等）
  - ✅ 多种容错策略（快速失败、故障转移等）
- **序列化**：
  - ✅ JDK、JSON、Hessian、Kryo 序列化支持
  - ✅ SPI 机制支持自定义序列化器
- **Spring Boot 集成**：
  - ✅ `@RpcService`、`@RpcReference` 注解
  - ✅ `@EnableRpc` 自动配置
  - ✅ 完整的示例应用

### 🚧 开发中

#### 第一阶段：动态配置中心与限流功能（计划中）
- **动态配置中心**：
  - ⏳ 服务配置模型和注册中心
  - ⏳ Etcd 配置中心实现
  - ⏳ 配置热更新和版本管理
- **限流功能**：
  - ⏳ 令牌桶算法实现
  - ⏳ 滑动窗口算法实现
  - ⏳ 全局和方法级限流

#### 第二阶段：服务调用链重构（计划中）
- **调用链框架**：
  - ⏳ 职责链模式实现
  - ⏳ 模块化处理器设计
  - ⏳ 异步调用链深度优化

#### 第三阶段：服务缓存优化（计划中）
- **多级缓存**：
  - ⏳ 本地缓存管理器
  - ⏳ 缓存预热和失效策略
  - ⏳ 分布式缓存支持

#### 第四阶段：异步调用优化（计划中）
- **异步调用支持**：
  - ⏳ 异步API设计和实现
  - ⏳ 连接池优化
  - ⏳ 异步调用性能优化

## 🛠️ 技术栈

- **核心框架**：Java 8+, Vert.x
- **服务注册**：Etcd, ZooKeeper
- **序列化**：JDK, JSON, Hessian, Kryo
- **Spring Boot**：2.6.x
- **网络通信**：TCP, Vert.x Event Loop
- **工具库**：Hutool, Lombok, Caffeine

## 📖 快速开始

### 基础用法

#### 1. 服务提供者

```java
@RpcService(version = "1.0")
public class UserServiceImpl implements UserService {
    @Override
    public User getUser(User user) {
        return user;
    }
}
```

#### 2. 服务消费者

```java
@Service
public class ConsumerService {

    // 服务调用（默认同步）
    @RpcReference
    private UserService userService;

    public void callService() {
        User user = new User("张三", 25);
        User result = userService.getUser(user);
        System.out.println("调用结果: " + result);
    }
}
```

#### 3. 启动应用

```java
@SpringBootApplication
@EnableRpc
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 配置文件

```properties
# RPC 配置
rpc.enabled=true
rpc.registry.address=http://localhost:2379
rpc.registry.type=etcd
rpc.loadBalancer=round_robin
rpc.retryStrategy=fixed_interval
rpc.tolerantStrategy=fail_fast
```

## 🏗️ 项目结构

```
yu-rpc/
├── yu-rpc-core/              # 核心框架
│   ├── src/main/java/
│   │   └── com/yupi/yurpc/
│   │       ├── config/       # 配置管理
│   │       ├── proxy/        # 服务代理
│   │       ├── registry/     # 服务注册
│   │       ├── server/       # 服务端
│   │       ├── spi/          # SPI 扩展
│   │       └── utils/        # 工具类
├── yu-rpc-spring-boot-starter/ # Spring Boot 集成
├── yu-rpc-easy/              # 简化版本
├── example-common/           # 示例公共模块
├── example-provider/         # 服务提供者示例
├── example-consumer/         # 服务消费者示例
└── example-springboot-*/     # Spring Boot 示例
```

## 🔧 配置说明

### 服务发布

```java
@RpcService(
    version = "1.0",           // 服务版本
    group = "default"          // 服务分组
)
public class UserServiceImpl implements UserService {
    // 实现逻辑
}
```

### 服务引用

```java
@RpcReference(
    version = "1.0",           // 服务版本
    group = "default",         // 服务分组
    loadBalancer = "random",   // 负载均衡策略
    retryStrategy = "fixed_interval",  // 重试策略
    tolerantStrategy = "fail_safe",    // 容错策略
    mock = false              // 是否启用模拟调用
)
private UserService userService;
```

## 📊 性能特性

- **连接复用**：Vert.x 连接池，减少连接开销
- **序列化优化**：支持多种序列化方式，可根据性能需求选择
- **负载均衡**：多种负载均衡算法，避免单点压力
- **容错机制**：重试和容错策略，保障服务稳定性

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

MIT License

## 🔮 高级特性

### 异步调用优化（计划中）

> 注意：异步调用功能正在开发中，目前专注于核心功能的稳定性

yu-rpc 未来将提供高性能异步调用功能，包括：

- **异步API设计**：非阻塞调用接口
- **性能优化**：连接池和并发优化
- **向后兼容**：与现有同步API完全兼容

## 🎯 设计理念

- **易用性优先**：保持简单的 API 设计，降低使用门槛
- **性能可选**：提供多种性能优化选项，按需选择
- **渐进增强**：从简单到复杂，支持平滑升级
- **可扩展性**：基于 SPI 机制，支持自定义扩展