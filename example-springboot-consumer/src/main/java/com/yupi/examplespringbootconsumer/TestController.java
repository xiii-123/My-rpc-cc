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
    @GetMapping("/async")
    public String testAsync() {
        try {
            exampleService.testAsyncExamples();
            return "异步调用测试完成！请查看控制台输出。";
        } catch (Exception e) {
            return "异步调用测试失败：" + e.getMessage();
        }
    }

    /**
     * 快速性能测试
     */
    @GetMapping("/performance")
    public String testPerformance() {
        try {
            exampleService.quickPerformanceTest();
            return "性能测试完成！请查看控制台输出的性能对比数据。";
        } catch (Exception e) {
            return "性能测试失败：" + e.getMessage();
        }
    }

    /**
     * 测试页面说明
     */
    @GetMapping("/")
    public String index() {
        return "<h1>yu-rpc 异步调用测试</h1>" +
               "<p><a href='/test/sync'>测试同步调用</a></p>" +
               "<p><a href='/test/async'>测试异步调用</a></p>" +
               "<p><a href='/test/performance'>性能对比测试</a></p>" +
               "<h3>使用说明：</h3>" +
               "<ul>" +
               "<li><b>同步调用</b>：使用 @RpcReference(async = false)，阻塞等待结果</li>" +
               "<li><b>异步调用</b>：使用 @RpcReference(async = true)，返回AsyncResult，非阻塞</li>" +
               "<li><b>性能测试</b>：对比同步串行和异步并发的性能差异</li>" +
               "</ul>" +
               "<p>所有测试结果都会在控制台输出详细日志。</p>";
    }
}