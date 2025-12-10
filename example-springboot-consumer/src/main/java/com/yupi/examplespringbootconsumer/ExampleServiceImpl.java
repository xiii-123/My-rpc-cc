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
     * 服务调用示例
     */
    @Resource
    private ServiceExample serviceExample;

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
     * 服务调用示例测试方法
     */
    public void testServiceExamples() {
        System.out.println("开始测试服务调用示例...");
        serviceExample.runAllExamples();
    }

    /**
     * 多次调用测试方法
     */
    public void testMultipleCalls() {
        System.out.println("=== 开始多次调用测试 ===");

        // 测试多次调用同一用户
        serviceExample.repeatedCallExample();

        // 测试多用户调用
        serviceExample.multipleUserExample();

        // 测试不同参数
        serviceExample.differentParamsExample();

        System.out.println("=== 多次调用测试完成 ===");
    }

}
