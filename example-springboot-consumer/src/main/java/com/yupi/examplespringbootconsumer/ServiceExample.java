package com.yupi.examplespringbootconsumer;

import com.yupi.example.common.model.User;
import com.yupi.example.common.service.UserService;
import com.yupi.yurpc.RpcApplication;
import com.yupi.yurpc.proxy.ServiceProxyFactory;
import com.yupi.yurpc.springboot.starter.annotation.RpcReference;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * RPC服务调用示例
 *
 * 展示如何使用RPC服务进行调用，支持Spring和独立运行两种方式
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @learn <a href="https://codefather.cn">程序员鱼皮的编程宝典</a>
 * @from <a href="https://yupi.icu">编程导航学习圈</a>
 */
@Service
public class ServiceExample {

    /**
     * 用户服务代理（Spring环境）
     */
    @RpcReference
    private UserService userService;

    /**
     * 主方法 - 支持独立运行测试
     */
    public static void main(String[] args) {
        System.out.println("=== Starting Standalone RPC Service Test ===");

        try {
            // 初始化RPC框架
            RpcApplication.init();
            System.out.println("RPC framework initialized successfully");

            // 获取服务代理
            UserService userService = ServiceProxyFactory.getProxy(UserService.class);
            System.out.println("UserService proxy obtained successfully");

            // 创建示例实例并运行测试
            ServiceExample example = new ServiceExample();
            example.userService = userService;
            example.runAllExamples();

            System.out.println("=== Standalone Test Completed ===");

        } catch (Exception e) {
            System.err.println("Standalone test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 基础调用示例
     */
    public void basicExample() {
        System.out.println("=== 基础调用示例 ===");

        User user = new User("张三", 25);
        long startTime = System.currentTimeMillis();

        // 服务调用
        User result = userService.getUser(user);

        long endTime = System.currentTimeMillis();
        System.out.println("调用结果: " + result);
        System.out.println("调用耗时: " + (endTime - startTime) + "ms");
        System.out.println();
    }

    /**
     * 多用户调用示例
     */
    public void multipleUserExample() {
        System.out.println("=== 多用户调用示例 ===");

        List<User> users = new ArrayList<>();
        users.add(new User("用户1", 20));
        users.add(new User("用户2", 25));
        users.add(new User("用户3", 30));
        users.add(new User("用户4", 35));
        users.add(new User("用户5", 40));

        long startTime = System.currentTimeMillis();

        for (User user : users) {
            User result = userService.getUser(user);
            System.out.println("处理用户: " + result.getName() + ", 年龄: " + result.getAge());
        }

        long endTime = System.currentTimeMillis();
        System.out.println("多用户调用总耗时: " + (endTime - startTime) + "ms");
        System.out.println();
    }

    /**
     * 重复调用同一用户示例
     */
    public void repeatedCallExample() {
        System.out.println("=== 重复调用同一用户示例 ===");

        User user = new User("重复测试用户", 28);
        int callTimes = 10;

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < callTimes; i++) {
            User result = userService.getUser(user);
            System.out.println("第 " + (i + 1) + " 次调用结果: " + result.getName());
        }

        long endTime = System.currentTimeMillis();
        System.out.println("重复调用 " + callTimes + " 次总耗时: " + (endTime - startTime) + "ms");
        System.out.println("平均每次调用耗时: " + (endTime - startTime) / callTimes + "ms");
        System.out.println();
    }

    /**
     * 不同参数类型测试示例
     */
    public void differentParamsExample() {
        System.out.println("=== 不同参数类型测试示例 ===");

        // 测试不同年龄段
        User[] users = {
            new User("儿童用户", 8),
            new User("青少年用户", 16),
            new User("成年用户", 25),
            new User("中年用户", 40),
            new User("老年用户", 65)
        };

        for (User user : users) {
            long startTime = System.currentTimeMillis();
            User result = userService.getUser(user);
            long endTime = System.currentTimeMillis();

            System.out.println("用户: " + result.getName() +
                             ", 年龄: " + result.getAge() +
                             ", 调用耗时: " + (endTime - startTime) + "ms");
        }
        System.out.println();
    }

    /**
     * 完整示例方法
     */
    public void runAllExamples() {
        System.out.println("=== RPC Service Call Complete Examples ===\n");

        basicExample();
        multipleUserExample();
        repeatedCallExample();
        differentParamsExample();

        System.out.println("=== All Examples Completed ===");
    }
}