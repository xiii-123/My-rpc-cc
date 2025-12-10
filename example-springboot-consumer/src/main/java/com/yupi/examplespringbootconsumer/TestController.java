package com.yupi.examplespringbootconsumer;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 测试控制器
 *
 * 提供HTTP接口测试同步和异步调用
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @learn <a href="https://codefather.cn">程序员鱼皮的编程宝典</a>
 * @from <a href="https://yupi.icu">编程导航学习圈</a>
 */
@RestController
@RequestMapping("/test")
public class TestController {

    @Resource
    private ExampleServiceImpl exampleService;

    /**
     * 测试同步调用
     */
    @GetMapping("/sync")
    public String testSync() {
        try {
            exampleService.test();
            return "同步调用测试完成！请查看控制台输出。";
        } catch (Exception e) {
            return "同步调用测试失败：" + e.getMessage();
        }
    }

    /**
     * 测试异步调用
     */
//    @GetMapping("/async")
//    public String testAsync() {
//        try {
//            exampleService.testAsyncExamples();
//            return "异步调用测试完成！请查看控制台输出。";
//        } catch (Exception e) {
//            return "异步调用测试失败：" + e.getMessage();
//        }
//    }

    /**
     * 测试服务调用示例
     */
    @GetMapping("/service")
    public String testService() {
        try {
            exampleService.testServiceExamples();
            return "服务调用测试完成！请查看控制台输出。";
        } catch (Exception e) {
            return "服务调用测试失败：" + e.getMessage();
        }
    }

    /**
     * 测试多次调用
     */
    @GetMapping("/multiple")
    public String testMultiple() {
        try {
            exampleService.testMultipleCalls();
            return "多次调用测试完成！请查看控制台输出。";
        } catch (Exception e) {
            return "多次调用测试失败：" + e.getMessage();
        }
    }

    /**
     * 测试页面说明
     */
    @GetMapping("/")
    public String index() {
        return "<h1>yu-rpc 服务调用测试</h1>" +
               "<p><a href='/test/sync'>基础同步调用</a></p>" +
               "<p><a href='/test/service'>服务调用示例</a></p>" +
               "<p><a href='/test/multiple'>多次调用测试</a></p>" +
               "<h3>使用说明：</h3>" +
               "<ul>" +
               "<li><b>基础同步调用</b>：使用 @RpcReference 注入服务，阻塞等待结果</li>" +
               "<li><b>服务调用示例</b>：展示各种服务调用场景和用法</li>" +
               "<li><b>多次调用测试</b>：测试连续多次服务调用的稳定性</li>" +
               "</ul>" +
               "<p>所有测试结果都会在控制台输出详细日志。</p>";
    }
}