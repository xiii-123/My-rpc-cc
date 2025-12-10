package com.yupi.examplespringbootconsumer;

import com.yupi.example.common.model.User;
import com.yupi.example.common.service.UserService;
import com.yupi.yurpc.RpcApplication;
import com.yupi.yurpc.proxy.ServiceProxyFactory;

/**
 * 快速RPC测试
 *
 * 最简单的RPC调用测试，直接运行main方法即可
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 */
public class QuickTest {

    public static void main(String[] args) {
        System.out.println("=== Quick RPC Test Start ===");

        try {
            // 1. 初始化RPC框架
            System.out.println("1. Initializing RPC framework...");
            RpcApplication.init();

            // 2. 获取服务代理
            System.out.println("2. Getting UserService proxy...");
            UserService userService = ServiceProxyFactory.getProxy(UserService.class);

            // 3. 执行RPC调用
            System.out.println("3. Executing RPC call...");
            User user = new User("Test User", 25);

            long startTime = System.currentTimeMillis();
            User result = userService.getUser(user);
            long endTime = System.currentTimeMillis();

            // 4. 输出结果
            System.out.println("Call result: " + result);
            System.out.println("Call duration: " + (endTime - startTime) + "ms");
            System.out.println("=== Quick RPC Test Completed ===");

        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}