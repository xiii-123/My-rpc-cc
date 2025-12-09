package com.yupi.examplespringbootconsumer;

import com.yupi.example.common.model.User;
import com.yupi.example.common.service.UserService;
import com.yupi.yurpc.springboot.starter.annotation.RpcReference;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 示例服务实现类
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @learn <a href="https://codefather.cn">编程宝典</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@Service
public class ExampleServiceImpl {

    /**
     * 使用 Rpc 框架注入（同步调用）
     */
    @RpcReference
    private UserService userService;

    /**
     * 异步服务示例
     */
    @Resource
    private AsyncServiceExample asyncServiceExample;

    /**
     * 原有测试方法（同步调用）
     */
    public void test() {
        User user = new User();
        user.setName("yupi");
        User resultUser = userService.getUser(user);
        System.out.println("同步调用结果: " + resultUser.getName());
    }

    /**
     * 异步调用示例测试方法
     */
    public void testAsyncExamples() {
        System.out.println("开始测试异步调用示例...");
        asyncServiceExample.runAllExamples();
    }

    /**
     * 快速测试同步vs异步性能对比
     */
    public void quickPerformanceTest() {
        System.out.println("=== 快速性能测试 ===");
        asyncServiceExample.concurrentCallsExample();
    }

}
