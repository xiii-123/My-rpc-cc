package com.yupi.yurpc.proxy;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.yupi.yurpc.RpcApplication;
import com.yupi.yurpc.config.RpcConfig;
import com.yupi.yurpc.constant.RpcConstant;
import com.yupi.yurpc.fault.retry.RetryStrategy;
import com.yupi.yurpc.fault.retry.RetryStrategyFactory;
import com.yupi.yurpc.fault.tolerant.TolerantStrategy;
import com.yupi.yurpc.fault.tolerant.TolerantStrategyFactory;
import com.yupi.yurpc.loadbalancer.LoadBalancer;
import com.yupi.yurpc.loadbalancer.LoadBalancerFactory;
import com.yupi.yurpc.model.RpcRequest;
import com.yupi.yurpc.model.RpcResponse;
import com.yupi.yurpc.model.ServiceMetaInfo;
import com.yupi.yurpc.registry.EtcdRegistry;
import com.yupi.yurpc.registry.Registry;
import com.yupi.yurpc.registry.RegistryFactory;
import com.yupi.yurpc.serializer.Serializer;
import com.yupi.yurpc.serializer.SerializerFactory;
import com.yupi.yurpc.server.tcp.VertxTcpClient;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 服务代理（JDK 动态代理）
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @learn <a href="https://codefather.cn">编程宝典</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
public class ServiceProxy implements InvocationHandler {

    /**
     * 获取动态策略配置
     * 优先从ETCD获取，如果没有则使用全局配置
     *
     * @param serviceName    服务名称
     * @param serviceVersion 服务版本
     * @param host           主机地址
     * @param port           端口
     * @param strategyType   策略类型
     * @param defaultValue   默认值
     * @return 策略值
     */
    private String getDynamicStrategy(String serviceName, String serviceVersion, String host, int port,
                                    String strategyType, String defaultValue) {
        try {
            RpcConfig rpcConfig = RpcApplication.getRpcConfig();
            Registry registry = RegistryFactory.getInstance(rpcConfig.getRegistryConfig().getRegistry());

            // 如果是ETCD注册中心，尝试获取动态策略
            if (registry instanceof EtcdRegistry) {
                EtcdRegistry etcdRegistry = (EtcdRegistry) registry;
                String strategy = etcdRegistry.getNodeStrategy(serviceName, serviceVersion, host, port, strategyType);
                if (strategy != null && !strategy.trim().isEmpty()) {
                    System.out.println("Using dynamic strategy from ETCD - " + strategyType + ": " + strategy);
                    return strategy;
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to get dynamic strategy, using default. Error: " + e.getMessage());
        }
        return defaultValue;
    }

    /**
     * 记录调用指标到ETCD
     *
     * @param serviceName    服务名称
     * @param serviceVersion 服务版本
     * @param host           主机地址
     * @param port           端口
     * @param duration       调用耗时
     * @param success        是否成功
     */
    private void recordCallMetrics(String serviceName, String serviceVersion, String host, int port,
                                  long duration, boolean success) {
        try {
            RpcConfig rpcConfig = RpcApplication.getRpcConfig();
            Registry registry = RegistryFactory.getInstance(rpcConfig.getRegistryConfig().getRegistry());

            // 如果是ETCD注册中心，记录监控指标
            if (registry instanceof EtcdRegistry) {
                EtcdRegistry etcdRegistry = (EtcdRegistry) registry;

                // 增加调用次数
                etcdRegistry.incrementMetrics(serviceName, serviceVersion, host, port, "calls", 1);

                if (success) {
                    // 增加成功次数
                    etcdRegistry.incrementMetrics(serviceName, serviceVersion, host, port, "success", 1);
                } else {
                    // 增加失败次数
                    etcdRegistry.incrementMetrics(serviceName, serviceVersion, host, port, "failure", 1);
                }

                // 更新平均耗时（简单移动平均）
                etcdRegistry.recordMetrics(serviceName, serviceVersion, host, port, "avg_time", String.valueOf(duration));
            }
        } catch (Exception e) {
            // 监控记录失败不应该影响主业务流程
            System.err.println("Failed to record call metrics: " + e.getMessage());
        }
    }

    /**
     * 调用代理
     *
     * @return
     * @throws Throwable
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 构造请求
        // fixme https://github.com/liyupi/yu-rpc/issues/7
        String serviceName = method.getDeclaringClass().getName();
        RpcRequest rpcRequest = RpcRequest.builder()
                .serviceName(serviceName)
                .methodName(method.getName())
                .parameterTypes(method.getParameterTypes())
                .args(args)
                .build();

        // 从注册中心获取服务提供者请求地址
        RpcConfig rpcConfig = RpcApplication.getRpcConfig();
        Registry registry = RegistryFactory.getInstance(rpcConfig.getRegistryConfig().getRegistry());
        ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
        serviceMetaInfo.setServiceName(serviceName);
        serviceMetaInfo.setServiceVersion(RpcConstant.DEFAULT_SERVICE_VERSION);
        List<ServiceMetaInfo> serviceMetaInfoList = registry.serviceDiscovery(serviceMetaInfo.getServiceKey());
        if (CollUtil.isEmpty(serviceMetaInfoList)) {
            throw new RuntimeException("暂无服务地址");
        }

        // 负载均衡 - 使用动态策略
        ServiceMetaInfo selectedServiceMetaInfo = serviceMetaInfoList.get(0); // 先选择第一个服务获取地址信息
        String serviceNameStr = serviceMetaInfo.getServiceName();
        String serviceVersionStr = serviceMetaInfo.getServiceVersion();
        String host = selectedServiceMetaInfo.getServiceHost();
        int port = selectedServiceMetaInfo.getServicePort();

        // 从ETCD获取动态负载均衡策略
        String loadBalanceStrategy = getDynamicStrategy(serviceNameStr, serviceVersionStr, host, port,
                "loadbalance", rpcConfig.getLoadBalancer());
        LoadBalancer loadBalancer = LoadBalancerFactory.getInstance(loadBalanceStrategy);

        // 将调用方法名（请求路径）作为负载均衡参数
        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("methodName", rpcRequest.getMethodName());
        ServiceMetaInfo finalSelectedServiceMetaInfo = loadBalancer.select(requestParams, serviceMetaInfoList);

        // rpc 请求 - 使用动态重试和容错策略
        RpcResponse rpcResponse;
        long startTime = System.currentTimeMillis();
        boolean success = false;

        try {
            // 从ETCD获取动态重试策略
            String retryStrategy = getDynamicStrategy(serviceNameStr, serviceVersionStr, host, port,
                    "retry", rpcConfig.getRetryStrategy());
            RetryStrategy retryStrategyInstance = RetryStrategyFactory.getInstance(retryStrategy);

            rpcResponse = retryStrategyInstance.doRetry(() ->
                    VertxTcpClient.doRequest(rpcRequest, finalSelectedServiceMetaInfo)
            );
            success = true;
        } catch (Exception e) {
            // 从ETCD获取动态容错策略
            String tolerantStrategy = getDynamicStrategy(serviceNameStr, serviceVersionStr, host, port,
                    "tolerant", rpcConfig.getTolerantStrategy());
            TolerantStrategy tolerantStrategyInstance = TolerantStrategyFactory.getInstance(tolerantStrategy);

            rpcResponse = tolerantStrategyInstance.doTolerant(null, e);
        } finally {
            // 记录调用指标
            long duration = System.currentTimeMillis() - startTime;
            recordCallMetrics(serviceNameStr, serviceVersionStr, host, port, duration, success);
        }

        return rpcResponse.getData();
    }

    /**
     * 发送 HTTP 请求
     *
     * @param selectedServiceMetaInfo
     * @param bodyBytes
     * @return
     * @throws IOException
     */
    private static RpcResponse doHttpRequest(ServiceMetaInfo selectedServiceMetaInfo, byte[] bodyBytes) throws IOException {
        final Serializer serializer = SerializerFactory.getInstance(RpcApplication.getRpcConfig().getSerializer());
        // 发送 HTTP 请求
        try (HttpResponse httpResponse = HttpRequest.post(selectedServiceMetaInfo.getServiceAddress())
                .body(bodyBytes)
                .execute()) {
            byte[] result = httpResponse.bodyBytes();
            // 反序列化
            RpcResponse rpcResponse = serializer.deserialize(result, RpcResponse.class);
            return rpcResponse;
        }
    }
}
