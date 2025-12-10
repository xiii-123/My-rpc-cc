package com.yupi.examplespringbootconsumer;

import com.yupi.example.common.model.User;
import com.yupi.example.common.service.UserService;
import com.yupi.yurpc.RpcApplication;
import com.yupi.yurpc.config.RpcConfig;
import com.yupi.yurpc.proxy.ServiceProxyFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * RPC测试运行器
 *
 * 独立运行测试，不依赖HTTP服务
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @learn <a href="https://codefather.cn">程序员鱼皮的编程宝典</a>
 * @from <a href="https://yupi.icu">编程导航学习圈</a>
 */
public class RpcTestRunner {

    public static void main(String[] args) {
        try {
            System.out.println("=== 开始RPC测试 ===");

            // 初始化RPC框架
            RpcApplication.init();
            System.out.println("RPC框架初始化完成");

            // 获取服务代理
            UserService userService = ServiceProxyFactory.getProxy(UserService.class);
            System.out.println("获取UserService代理成功");

            // 运行各种测试
            runBasicTest(userService);
            runMultipleUserTest(userService);
            runRepeatedCallTest(userService);
            runDifferentParamsTest(userService);

            System.out.println("=== 所有RPC测试完成 ===");

        } catch (Exception e) {
            System.err.println("RPC测试失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 基础调用测试
     */
    public static void runBasicTest(UserService userService) {
        System.out.println("\n=== 基础调用测试 ===");

        User user = new User("张三", 25);
        long startTime = System.currentTimeMillis();

        // 服务调用
        User result = userService.getUser(user);

        long endTime = System.currentTimeMillis();
        System.out.println("调用结果: " + result);
        System.out.println("调用耗时: " + (endTime - startTime) + "ms");
    }

    /**
     * 多用户调用测试
     */
    public static void runMultipleUserTest(UserService userService) {
        System.out.println("\n=== 多用户调用测试 ===");

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
    }

    /**
     * 重复调用测试
     */
    public static void runRepeatedCallTest(UserService userService) {
        System.out.println("\n=== 重复调用测试 ===");

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
    }

    /**
     * 不同参数测试
     */
    public static void runDifferentParamsTest(UserService userService) {
        System.out.println("\n=== 不同参数测试 ===");

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
    }
}