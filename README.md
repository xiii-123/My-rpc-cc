# yu-rpc - 轻量级 RPC 框架

一个基于 Java 的轻量级 RPC 框架，支持服务注册发现、负载均衡、容错重试、异步调用等企业级特性。

## 🚀 特性

- **简单易用**：注解驱动的服务发布与引用，Spring Boot 无缝集成
- **服务治理**：支持 Etcd、ZooKeeper 服务注册与发现
- **负载均衡**：轮询、随机、一致性哈希等多种负载均衡策略
- **容错重试**：多种重试和容错策略，保障服务稳定性
- **异步调用**：支持高性能异步调用，提升并发处理能力
- **多序列化**：支持 JDK、JSON、Hessian、Kryo 等多种序列化方式
- **协议扩展**：基于 SPI 机制，支持自定义扩展

## 📋 开发路线图

### 🎯 目标特性
1. **异步调用支持** - 提供高性能异步调用选项
2. **动态配置中心** - 分布式配置管理和热更新
3. **限流功能** - 流量控制和系统保护
4. **服务调用链重构** - 优化架构设计，提升可扩展性
5. **服务缓存优化** - 多级缓存，提升性能

### ✅ 已完成功能

#### 第一阶段：异步调用支持（90%完成）
- **异步调用API设计**：
  - ✅ `AsyncResult<T>` - 异步结果封装类
  - ✅ `AsyncServiceProxy` - 异步服务代理实现
  - ✅ `AsyncServiceProxyFactory` - 异步代理工厂
- **注解集成**：
  - ✅ `@RpcReference(async = true)` - 启用异步调用
  - ✅ `@RpcReference(timeout = 5000)` - 设置超时时间
- **配置扩展**：
  - ✅ `RpcConfig.enableAsync` - 全局异步开关
  - ✅ `RpcConfig.asyncTimeout` - 异步调用超时配置
  - ✅ `RpcConfig.asyncThreadPoolConfig` - 异步线程池配置
- **网络层优化**：
  - ✅ `VertxTcpClient.doRequestAsync()` - 真正异步请求方法
  - ✅ 连接复用和连接池优化
- **完整示例**：
  - ✅ Spring Boot 异步调用示例
  - ✅ 异步 vs 同步性能对比示例
  - ✅ HTTP 测试接口

### 🚧 开发中

#### 第二阶段：动态配置中心与限流功能（计划中）
- **动态配置中心**：
  - ⏳ 服务配置模型和注册中心
  - ⏳ Etcd 配置中心实现
  - ⏳ 配置热更新和版本管理
- **限流功能**：
  - ⏳ 令牌桶算法实现
  - ⏳ 滑动窗口算法实现
  - ⏳ 全局和方法级限流

#### 第三阶段：服务调用链重构（计划中）
- **调用链框架**：
  - ⏳ 职责链模式实现
  - ⏳ 模块化处理器设计
  - ⏳ 异步调用链深度优化

#### 第四阶段：服务缓存优化（计划中）
- **多级缓存**：
  - ⏳ 本地缓存管理器
  - ⏳ 缓存预热和失效策略
  - ⏳ 分布式缓存支持

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

    // 同步调用（默认）
    @RpcReference
    private UserService userService;

    // 异步调用
    @RpcReference(async = true, timeout = 5000)
    private UserService asyncUserService;

    public void syncCall() {
        User result = userService.getUser(new User("张三", 25));
        System.out.println("同步结果: " + result);
    }

    public void asyncCall() {
        AsyncResult<User> asyncResult = (AsyncResult<User>)
            asyncUserService.getUser(new User("李四", 30));

        asyncResult.whenComplete((result, throwable) -> {
            if (throwable == null) {
                System.out.println("异步结果: " + result);
            } else {
                System.err.println("异步调用失败: " + throwable.getMessage());
            }
        });
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
rpc.enableAsync=true
rpc.asyncTimeout=30000
```

## 🏗️ 项目结构

```
yu-rpc/
├── yu-rpc-core/              # 核心框架
│   ├── src/main/java/
│   │   └── com/yupi/yurpc/
│   │       ├── async/        # 异步调用模块
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
    mock = false,              // 是否启用模拟调用
    async = false,             // 是否异步调用
    timeout = 30000            // 超时时间
)
private UserService userService;
```

## 📊 性能特性

- **异步调用**：支持真正的非阻塞调用，提升并发性能
- **连接复用**：Vert.x 连接池，减少连接开销
- **序列化优化**：支持多种序列化方式，可根据性能需求选择
- **负载均衡**：多种负载均衡算法，避免单点压力

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

MIT License

## 🎯 设计理念

- **易用性优先**：保持简单的 API 设计，降低使用门槛
- **性能可选**：提供多种性能优化选项，按需选择
- **渐进增强**：从简单到复杂，支持平滑升级
- **可扩展性**：基于 SPI 机制，支持自定义扩展