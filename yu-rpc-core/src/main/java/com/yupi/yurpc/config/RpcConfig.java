package com.yupi.yurpc.config;

import com.yupi.yurpc.fault.retry.RetryStrategyKeys;
import com.yupi.yurpc.fault.tolerant.TolerantStrategyKeys;
import com.yupi.yurpc.loadbalancer.LoadBalancerKeys;
import com.yupi.yurpc.serializer.SerializerKeys;
import lombok.Data;

/**
 * RPC 框架全局配置
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @learn <a href="https://codefather.cn">鱼皮的编程宝典</a>
 * @from <a href="https://yupi.icu">编程导航学习圈</a>
 */
@Data
public class RpcConfig {

    /**
     * 名称
     */
    private String name = "yu-rpc";

    /**
     * 版本号
     */
    private String version = "1.0";

    /**
     * 服务器主机名
     */
    private String serverHost = "localhost";

    /**
     * 服务器端口号
     */
    private Integer serverPort = 8999;

    /**
     * 序列化器
     */
    private String serializer = SerializerKeys.JDK;

    /**
     * 负载均衡器
     */
    private String loadBalancer = LoadBalancerKeys.ROUND_ROBIN;

    /**
     * 重试策略
     */
    private String retryStrategy = RetryStrategyKeys.NO;

    /**
     * 容错策略
     */
    private String tolerantStrategy = TolerantStrategyKeys.FAIL_FAST;

    /**
     * 模拟调用
     */
    private boolean mock = false;

    /**
     * 注册中心配置
     */
    private RegistryConfig registryConfig = new RegistryConfig();

    /**
     * 是否启用异步调用
     */
    private boolean enableAsync = false;

    /**
     * 异步调用超时时间（毫秒）
     */
    private long asyncTimeout = 30000;

    /**
     * 异步调用线程池配置
     */
    private ThreadPoolConfig asyncThreadPoolConfig = new ThreadPoolConfig();

    /**
     * 线程池配置
     */
    @Data
    public static class ThreadPoolConfig {
        /**
         * 核心线程池大小
         */
        private int corePoolSize = 10;

        /**
         * 最大线程池大小
         */
        private int maxPoolSize = 50;

        /**
         * 线程保活时间（秒）
         */
        private long keepAliveTime = 60;

        /**
         * 队列容量
         */
        private int queueCapacity = 1000;
    }
}
