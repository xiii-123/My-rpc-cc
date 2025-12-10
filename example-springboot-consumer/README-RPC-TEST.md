# RPC测试指南

现在有三种方式可以直接测试RPC功能，无需启动HTTP服务：

## 方法1: QuickTest - 最简单的测试

```bash
cd D:\Work-Files\my-rpc-cc\yu-rpc\example-springboot-consumer
mvn exec:java -Dexec.mainClass="com.yupi.examplespringbootconsumer.QuickTest"
```

**功能**: 执行一次基本的RPC调用，测试基本功能是否正常

## 方法2: ServiceExample - 完整测试

```bash
cd D:\Work-Files\my-rpc-cc\yu-rpc\example-springboot-consumer
mvn exec:java -Dexec.mainClass="com.yupi.examplespringbootconsumer.ServiceExample"
```

**功能**: 运行完整的服务调用示例，包括：
- 基础调用测试
- 多用户调用测试
- 重复调用测试
- 不同参数测试

## 方法3: RpcTestRunner - 独立测试框架

```bash
cd D:\Work-Files\my-rpc-cc\yu-rpc\example-springboot-consumer
mvn exec:java -Dexec.mainClass="com.yupi.examplespringbootconsumer.RpcTestRunner"
```

**功能**: 完整的RPC测试套件，包含详细的性能统计

## 编码问题解决方案

如果出现中文乱码，请使用以下方法：

### 方法1: 使用批处理文件（推荐）
```bash
# 运行快速测试
run-test.bat

# 运行完整测试
run-service-test.bat
```

### 方法2: 手动设置编码后运行
```cmd
# Windows CMD
chcp 65001
set JAVA_OPTS=-Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8
mvn exec:java -Dexec.mainClass="com.yupi.examplespringbootconsumer.QuickTest"
```

### 方法3: IDEA中设置
1. File → Settings → Editor → File Encodings
2. 所有编码设置为 UTF-8
3. VM Options 添加：`-Dfile.encoding=UTF-8`

## 运行前准备

确保以下服务正在运行：
1. **Etcd注册中心** (默认端口2379)
2. **服务提供者** (启动ProviderExample)

## 输出示例

```
=== 快速RPC测试开始 ===
1. 初始化RPC框架...
2. 获取UserService代理...
3. 执行RPC调用...
调用结果: User{name='测试用户', age=25}
调用耗时: 15ms
=== 快速RPC测试完成 ===
```

## 端口说明

- `8989` - Spring Boot Consumer Web端口(如果使用HTTP测试)
- `8999` - RPC服务提供者端口
- `2379` - Etcd注册中心端口

现在你可以直接运行这些测试类来验证RPC功能，无需担心端口冲突问题！