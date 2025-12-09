package com.yupi.example.consumer;

import com.yupi.example.common.model.User;
import com.yupi.example.common.service.UserService;
import com.yupi.yurpc.RpcApplication;
import com.yupi.yurpc.async.AsyncResult;
import com.yupi.yurpc.async.AsyncServiceProxyFactory;
import com.yupi.yurpc.config.RpcConfig;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 异步调用示例
 *
 * 展示yu-rpc框架的异步调用功能
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @learn <a href="https://codefather.cn">程序员鱼皮的编程宝典</a>
 * @from <a href="https://yupi.icu">编程导航学习圈</a>
 */
public class AsyncExample {

    /**
     * 同步服务接口（对比用）
     */
    public interface SyncUserService {
        User getUser(User user);
    }

    /**
     * 异步服务接口
     */
    public interface AsyncUserService {
        // 异步方法：返回AsyncResult
        AsyncResult<User> getUserAsync(User user);

        // 同步方法：返回具体结果
        User getUser(User user);
    }

    public static void main(String[] args) {
        // 初始化RPC框架
        RpcConfig config = new RpcConfig();
        config.setEnableAsync(true);  // 启用异步调用
        RpcApplication.init(config);

        // 创建异步服务代理
        AsyncUserService userService = AsyncServiceProxyFactory.getProxy(AsyncUserService.class);

        System.out.println("=== yu-rpc 异步调用示例 ===\n");

        // 示例1：异步调用基础用法
        asyncCallBasicExample(userService);

        // 示例2：异步调用回调处理
        asyncCallWithCallbackExample(userService);

        // 示例3：批量异步调用
        batchAsyncCallExample(userService);

        // 示例4：异步调用链式处理
        asyncCallChainExample(userService);

        // 示例5：异步调用异常处理
        asyncCallExceptionExample(userService);

        // 示例6：异步调用超时控制
        asyncCallTimeoutExample(userService);

        // 示例7：同步与异步混合调用
        syncAndAsyncMixedExample(userService);
    }

    /**
     * 示例1：异步调用基础用法
     */
    private static void asyncCallBasicExample(AsyncUserService userService) {
        System.out.println("1. 异步调用基础用法：");

        User requestUser = new User("张三", 25);

        // 发起异步调用
        AsyncResult<User> asyncResult = userService.getUserAsync(requestUser);

        System.out.println("   异步调用已发起，请求ID: " + asyncResult.getRequestId());
        System.out.println("   可以在等待结果时做其他事情...");

        try {
            // 获取结果（会阻塞直到结果返回）
            User result = asyncResult.get();
            System.out.println("   异步调用结果: " + result);
        } catch (Exception e) {
            System.err.println("   异步调用失败: " + e.getMessage());
        }

        System.out.println();
    }

    /**
     * 示例2：异步调用回调处理
     */
    private static void asyncCallWithCallbackExample(AsyncUserService userService) {
        System.out.println("2. 异步调用回调处理：");

        User requestUser = new User("李四", 30);

        AsyncResult<User> asyncResult = userService.getUserAsync(requestUser);

        // 注册完成回调
        asyncResult.whenComplete((result, throwable) -> {
            if (throwable != null) {
                System.err.println("   回调处理 - 调用失败: " + throwable.getMessage());
            } else {
                System.out.println("   回调处理 - 调用成功: " + result);
            }
        });

        System.out.println("   回调已注册，继续执行其他操作...");

        try {
            Thread.sleep(1000); // 等待回调执行
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println();
    }

    /**
     * 示例3：批量异步调用
     */
    private static void batchAsyncCallExample(AsyncUserService userService) {
        System.out.println("3. 批量异步调用：");

        List<User> users = Arrays.asList(
            new User("王五", 28),
            new User("赵六", 32),
            new User("钱七", 27)
        );

        long startTime = System.currentTimeMillis();

        // 并发发起多个异步调用
        List<AsyncResult<User>> asyncResults = users.stream()
            .map(user -> userService.getUserAsync(user))
            .collect(Collectors.toList());

        System.out.println("   已发起 " + asyncResults.size() + " 个异步调用");

        // 等待所有调用完成
        List<User> results = asyncResults.stream()
            .map(async -> {
                try {
                    return async.get();
                } catch (Exception e) {
                    System.err.println("   调用失败: " + e.getMessage());
                    return null;
                }
            })
            .collect(Collectors.toList());

        long endTime = System.currentTimeMillis();

        System.out.println("   批量调用完成，耗时: " + (endTime - startTime) + "ms");
        System.out.println("   结果数量: " + results.size());
        results.forEach(System.out::println);

        System.out.println();
    }

    /**
     * 示例4：异步调用链式处理
     */
    private static void asyncCallChainExample(AsyncUserService userService) {
        System.out.println("4. 异步调用链式处理：");

        User requestUser = new User("孙八", 35);

        userService.getUserAsync(requestUser)
            .thenApply(user -> {
                // 转换结果：提取用户名并转为大写
                System.out.println("   第一步：获取用户: " + user);
                return user.getName().toUpperCase();
            })
            .thenAccept(name -> {
                // 消费结果：打印大写的用户名
                System.out.println("   第二步：处理后的用户名: " + name);
            })
            .exceptionally(e -> {
                // 异常处理
                System.err.println("   链式调用出错: " + e.getMessage());
                return null;
            });

        try {
            Thread.sleep(1000); // 等待链式调用完成
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println();
    }

    /**
     * 示例5：异步调用异常处理
     */
    private static void asyncCallExceptionExample(AsyncUserService userService) {
        System.out.println("5. 异步调用异常处理：");

        // 传入可能引起异常的参数（比如null）
        AsyncResult<User> asyncResult = userService.getUserAsync(null);

        // 注册异常处理回调
        asyncResult.exceptionally(e -> {
            System.err.println("   捕获到异常: " + e.getMessage());
            // 返回默认值
            return new User("默认用户", 0);
        });

        try {
            User result = asyncResult.get();
            System.out.println("   异常处理后的结果: " + result);
        } catch (Exception e) {
            System.err.println("   获取结果时出错: " + e.getMessage());
        }

        System.out.println();
    }

    /**
     * 示例6：异步调用超时控制
     */
    private static void asyncCallTimeoutExample(AsyncUserService userService) {
        System.out.println("6. 异步调用超时控制：");

        User requestUser = new User("周九", 40);

        AsyncResult<User> asyncResult = userService.getUserAsync(requestUser);

        try {
            // 设置超时时间为1秒
            User result = asyncResult.get(1, java.util.concurrent.TimeUnit.SECONDS);
            System.out.println("   在超时时间内获取到结果: " + result);
        } catch (java.util.concurrent.TimeoutException e) {
            System.err.println("   调用超时: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("   其他错误: " + e.getMessage());
        }

        System.out.println();
    }

    /**
     * 示例7：同步与异步混合调用
     */
    private static void syncAndAsyncMixedExample(AsyncUserService userService) {
        System.out.println("7. 同步与异步混合调用：");

        User user1 = new User("吴十", 45);
        User user2 = new User("郑十一", 38);

        try {
            // 同步调用
            System.out.println("   发起同步调用...");
            User syncResult = userService.getUser(user1);
            System.out.println("   同步调用结果: " + syncResult);

            // 异步调用
            System.out.println("   发起异步调用...");
            AsyncResult<User> asyncResult = userService.getUserAsync(user2);

            // 在等待异步结果时做其他事情
            System.out.println("   处理其他业务逻辑...");
            Thread.sleep(500);

            // 获取异步结果
            User asyncResultUser = asyncResult.get();
            System.out.println("   异步调用结果: " + asyncResultUser);

        } catch (Exception e) {
            System.err.println("   混合调用出错: " + e.getMessage());
        }

        System.out.println();
    }
}