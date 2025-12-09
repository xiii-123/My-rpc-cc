package com.yupi.examplespringbootconsumer;

import com.yupi.example.common.model.User;
import com.yupi.example.common.service.UserService;
import com.yupi.yurpc.async.AsyncResult;
import com.yupi.yurpc.springboot.starter.annotation.RpcReference;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Spring Boot 异步调用示例
 *
 * 展示如何使用 @RpcReference 注解进行同步和异步调用
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @learn <a href="https://codefather.cn">程序员鱼皮的编程宝典</a>
 * @from <a href="https://yupi.icu">编程导航学习圈</a>
 */
@Service
public class AsyncServiceExample {

    /**
     * 同步服务代理（默认）
     */
    @RpcReference(async = false)
    private UserService syncUserService;

    /**
     * 异步服务代理
     */
    @RpcReference(async = true)
    private UserService asyncUserService;

    /**
     * 异步服务接口（用于返回AsyncResult的方法调用）
     */
    @RpcReference(async = true)
    private UserService asyncResultUserService;

    /**
     * 基础同步调用示例
     */
    public void basicSyncExample() {
        System.out.println("=== 基础同步调用示例 ===");

        User user = new User("张三", 25);
        long startTime = System.currentTimeMillis();

        // 同步调用，阻塞等待结果
        User result = syncUserService.getUser(user);

        long endTime = System.currentTimeMillis();
        System.out.println("同步调用结果: " + result);
        System.out.println("同步调用耗时: " + (endTime - startTime) + "ms");
        System.out.println();
    }

    /**
     * 基础异步调用示例
     */
    public void basicAsyncExample() {
        System.out.println("=== 基础异步调用示例 ===");

        User user = new User("李四", 30);
        long startTime = System.currentTimeMillis();

        // 异步调用，立即返回AsyncResult
        AsyncResult<User> asyncResult = (AsyncResult<User>) asyncUserService.getUser(user);
        System.out.println("异步调用已发起，请求ID: " + asyncResult.getRequestId());

        // 可以在等待结果时做其他事情
        System.out.println("正在处理其他业务逻辑...");

        // 获取异步结果
        try {
            User result = asyncResult.get();
            long endTime = System.currentTimeMillis();
            System.out.println("异步调用结果: " + result);
            System.out.println("异步调用总耗时: " + (endTime - startTime) + "ms");
        } catch (Exception e) {
            System.err.println("异步调用失败: " + e.getMessage());
        }
        System.out.println();
    }

    /**
     * 并发调用对比示例
     */
    public void concurrentCallsExample() {
        System.out.println("=== 并发调用对比示例 ===");

        List<User> users = Arrays.asList(
            new User("用户1", 25),
            new User("用户2", 30),
            new User("用户3", 35),
            new User("用户4", 40),
            new User("用户5", 45)
        );

        // 同步串行调用
        long syncStart = System.currentTimeMillis();
        for (User user : users) {
            syncUserService.getUser(user);
        }
        long syncEnd = System.currentTimeMillis();
        long syncTime = syncEnd - syncStart;

        // 异步并发调用
        long asyncStart = System.currentTimeMillis();

        // 发起所有异步调用
        List<AsyncResult<User>> asyncResults = users.stream()
            .map(user -> (AsyncResult<User>) asyncUserService.getUser(user))
            .collect(Collectors.toList());

        System.out.println("已发起 " + asyncResults.size() + " 个异步调用");

        // 等待所有结果
        for (AsyncResult<User> result : asyncResults) {
            try {
                result.get();
            } catch (Exception e) {
                System.err.println("异步调用失败: " + e.getMessage());
            }
        }

        long asyncEnd = System.currentTimeMillis();
        long asyncTime = asyncEnd - asyncStart;

        System.out.println("同步串行调用耗时: " + syncTime + "ms");
        System.out.println("异步并发调用耗时: " + asyncTime + "ms");
        System.out.println("性能提升: " + String.format("%.2f", (double) syncTime / asyncTime) + "x");
        System.out.println();
    }

    /**
     * CompletableFuture 高级异步示例
     */
    public void completableFutureExample() {
        System.out.println("=== CompletableFuture 高级异步示例 ===");

        User user1 = new User("用户A", 28);
        User user2 = new User("用户B", 32);

        long startTime = System.currentTimeMillis();

        try {
            // 发起异步调用
            AsyncResult<User> result1 = (AsyncResult<User>) asyncUserService.getUser(user1);
            AsyncResult<User> result2 = (AsyncResult<User>) asyncUserService.getUser(user2);

            // 转换为CompletableFuture进行链式操作
            CompletableFuture<User> future1 = result1.getFuture();
            CompletableFuture<User> future2 = result2.getFuture();

            // 组合异步操作
            CompletableFuture<String> combinedFuture = CompletableFuture.allOf(future1, future2)
                .thenApply(v -> {
                    try {
                        User u1 = future1.get();
                        User u2 = future2.get();
                        return "组合结果: " + u1.getName() + " 和 " + u2.getName();
                    } catch (Exception e) {
                        return "组合失败: " + e.getMessage();
                    }
                });

            // 异步处理结果
            combinedFuture.thenAccept(result -> {
                long endTime = System.currentTimeMillis();
                System.out.println("异步组合操作完成: " + result);
                System.out.println("总耗时: " + (endTime - startTime) + "ms");
            }).exceptionally(e -> {
                System.err.println("异步组合操作失败: " + e.getMessage());
                return null;
            });

            // 等待完成
            combinedFuture.get();

        } catch (Exception e) {
            System.err.println("CompletableFuture操作失败: " + e.getMessage());
        }
        System.out.println();
    }

    /**
     * 异步回调示例
     */
    public void asyncCallbackExample() {
        System.out.println("=== 异步回调示例 ===");

        User user = new User("回调用户", 35);

        // 发起异步调用
        AsyncResult<User> asyncResult = (AsyncResult<User>) asyncUserService.getUser(user);

        // 注册成功回调
        asyncResult.whenComplete((result, throwable) -> {
            if (throwable != null) {
                System.err.println("异步调用失败 - 回调处理: " + throwable.getMessage());
            } else {
                System.out.println("异步调用成功 - 回调处理: " + result);
            }
        });

        // 注册异常处理回调
        asyncResult.exceptionally(e -> {
            System.err.println("异步调用异常处理: " + e.getMessage());
            // 返回默认值
            return new User("默认用户", 0);
        });

        // 主线程继续执行其他操作
        System.out.println("主线程继续执行其他操作...");

        try {
            Thread.sleep(1000); // 等待回调执行
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println();
    }

    /**
     * 完整示例方法
     */
    public void runAllExamples() {
        System.out.println("=== Spring Boot 异步调用完整示例 ===\n");

        basicSyncExample();
        basicAsyncExample();
        concurrentCallsExample();
        completableFutureExample();
        asyncCallbackExample();

        System.out.println("=== 所有示例执行完成 ===");
    }
}