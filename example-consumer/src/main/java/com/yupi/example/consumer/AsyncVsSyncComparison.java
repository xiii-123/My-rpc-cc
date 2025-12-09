package com.yupi.example.consumer;

import com.yupi.example.common.model.User;
import com.yupi.example.common.service.UserService;
import com.yupi.yurpc.RpcApplication;
import com.yupi.yurpc.async.AsyncResult;
import com.yupi.yurpc.async.AsyncServiceProxyFactory;
import com.yupi.yurpc.config.RpcConfig;
import com.yupi.yurpc.proxy.ServiceProxyFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 异步调用与同步调用对比示例
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @learn <a href="https://codefather.cn">程序员鱼皮的编程宝典</a>
 * @from <a href="https://yupi.icu">编程导航学习圈</a>
 */
public class AsyncVsSyncComparison {

    // 异步服务接口
    public interface AsyncUserService {
        AsyncResult<User> getUserAsync(User user);
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        // 初始化RPC框架
        RpcConfig config = new RpcConfig();
        config.setEnableAsync(true);
        RpcApplication.init(config);

        System.out.println("=== 异步调用 vs 同步调用性能对比 ===\n");

        // 1. 单次调用对比
        singleCallComparison();

        // 2. 多次串行调用对比
        multipleSerialCallsComparison();

        // 3. 多次并发调用对比（关键区别）
        multipleConcurrentCallsComparison();

        // 4. 异步调用中做其他事情
        doOtherThingsDuringAsync();
    }

    /**
     * 1. 单次调用对比
     */
    private static void singleCallComparison() throws ExecutionException, InterruptedException {
        System.out.println("1. 单次调用对比：");

        // 同步调用
        UserService syncUserService = ServiceProxyFactory.getProxy(UserService.class);
        long syncStart = System.currentTimeMillis();
        User syncResult = syncUserService.getUser(new User("张三", 25));
        long syncEnd = System.currentTimeMillis();

        // 异步调用
        AsyncUserService asyncUserService = AsyncServiceProxyFactory.getProxy(AsyncUserService.class);
        long asyncStart = System.currentTimeMillis();
        AsyncResult<User> asyncResult = asyncUserService.getUserAsync(new User("李四", 30));
        User asyncUser = asyncResult.get();
        long asyncEnd = System.currentTimeMillis();

        System.out.println("   同步调用耗时: " + (syncEnd - syncStart) + "ms, 结果: " + syncResult);
        System.out.println("   异步调用耗时: " + (asyncEnd - asyncStart) + "ms, 结果: " + asyncUser);
        System.out.println("   结论：单次调用性能相似\n");
    }

    /**
     * 2. 多次串行调用对比
     */
    private static void multipleSerialCallsComparison() throws ExecutionException, InterruptedException {
        System.out.println("2. 多次串行调用对比：");

        List<User> users = Arrays.asList(
            new User("用户1", 25),
            new User("用户2", 30),
            new User("用户3", 35)
        );

        // 同步串行调用
        UserService syncUserService = ServiceProxyFactory.getProxy(UserService.class);
        long syncStart = System.currentTimeMillis();
        for (User user : users) {
            syncUserService.getUser(user);
        }
        long syncEnd = System.currentTimeMillis();

        // 异步串行调用（错误的用法）
        AsyncUserService asyncUserService = AsyncServiceProxyFactory.getProxy(AsyncUserService.class);
        long asyncStart = System.currentTimeMillis();
        for (User user : users) {
            AsyncResult<User> result = asyncUserService.getUserAsync(user);
            result.get(); // 立即等待结果，相当于同步
        }
        long asyncEnd = System.currentTimeMillis();

        System.out.println("   同步串行调用耗时: " + (syncEnd - syncStart) + "ms");
        System.out.println("   异步串行调用耗时: " + (asyncEnd - asyncStart) + "ms");
        System.out.println("   结论：串行调用性能相似\n");
    }

    /**
     * 3. 多次并发调用对比（异步的优势体现）
     */
    private static void multipleConcurrentCallsComparison() throws ExecutionException, InterruptedException {
        System.out.println("3. 多次并发调用对比（异步的优势）：");

        List<User> users = Arrays.asList(
            new User("并发用户1", 25),
            new User("并发用户2", 30),
            new User("并发用户3", 35),
            new User("并发用户4", 40),
            new User("并发用户5", 45)
        );

        // 同步串行调用（必须一个一个等）
        UserService syncUserService = ServiceProxyFactory.getProxy(UserService.class);
        long syncStart = System.currentTimeMillis();
        for (User user : users) {
            syncUserService.getUser(user);
        }
        long syncEnd = System.currentTimeMillis();

        // 异步并发调用（先全部发起，再一起等待）
        AsyncUserService asyncUserService = AsyncServiceProxyFactory.getProxy(AsyncUserService.class);
        long asyncStart = System.currentTimeMillis();

        // 先发起所有异步调用（非阻塞）
        List<AsyncResult<User>> asyncResults = users.stream()
            .map(user -> asyncUserService.getUserAsync(user))
            .collect(Collectors.toList());

        System.out.println("   已发起 " + asyncResults.size() + " 个异步调用");

        // 然后一起等待所有结果
        for (AsyncResult<User> result : asyncResults) {
            result.get();
        }
        long asyncEnd = System.currentTimeMillis();

        System.out.println("   同步串行耗时: " + (syncEnd - syncStart) + "ms");
        System.out.println("   异步并发耗时: " + (asyncEnd - asyncStart) + "ms");
        System.out.println("   结论：异步并发调用更快！\n");
    }

    /**
     * 4. 异步调用中做其他事情
     */
    private static void doOtherThingsDuringAsync() {
        System.out.println("4. 异步调用中做其他事情：");

        AsyncUserService asyncUserService = AsyncServiceProxyFactory.getProxy(AsyncUserService.class);

        // 发起异步调用
        System.out.println("   发起异步调用...");
        AsyncResult<User> asyncResult = asyncUserService.getUserAsync(new User("测试用户", 28));

        // 在等待结果的同时，可以做其他事情
        System.out.println("   在等待结果时，可以做其他事情：");

        // 模拟做其他计算
        int sum = 0;
        for (int i = 1; i <= 100; i++) {
            sum += i;
            // 模拟一些处理时间
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("   其他事情完成，计算结果: " + sum);

        // 获取异步结果
        try {
            User user = asyncResult.get();
            System.out.println("   异步调用结果: " + user);
        } catch (Exception e) {
            System.err.println("   获取结果失败: " + e.getMessage());
        }

        System.out.println("   结论：异步允许同时处理多个任务\n");
    }

    /**
     * 更优雅的异步并发示例
     */
    private static void elegantAsyncExample() {
        System.out.println("5. 更优雅的异步并发示例：");

        List<User> users = Arrays.asList(
            new User("优雅用户1", 25),
            new User("优雅用户2", 30),
            new User("优雅用户3", 35)
        );

        AsyncUserService asyncUserService = AsyncServiceProxyFactory.getProxy(AsyncUserService.class);

        long start = System.currentTimeMillis();

        // 使用 CompletableFuture 并行处理
        List<CompletableFuture<User>> futures = users.stream()
            .map(user -> {
                CompletableFuture<User> future = new CompletableFuture<>();
                AsyncResult<User> asyncResult = asyncUserService.getUserAsync(user);

                // 将 AsyncResult 转为 CompletableFuture
                asyncResult.whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        future.completeExceptionally(throwable);
                    } else {
                        future.complete(result);
                    }
                });

                return future;
            })
            .collect(Collectors.toList());

        // 等待所有异步调用完成
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );

        try {
            // 等待所有任务完成
            allFutures.get();

            // 收集结果
            List<User> results = futures.stream()
                .map(future -> {
                    try {
                        return future.get();
                    } catch (Exception e) {
                        System.err.println("获取结果失败: " + e.getMessage());
                        return null;
                    }
                })
                .collect(Collectors.toList());

            long end = System.currentTimeMillis();

            System.out.println("   并发调用完成，耗时: " + (end - start) + "ms");
            System.out.println("   结果数量: " + results.size());

        } catch (Exception e) {
            System.err.println("异步调用出错: " + e.getMessage());
        }
    }
}