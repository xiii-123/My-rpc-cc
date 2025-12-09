package com.yupi.yurpc.async;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import com.yupi.yurpc.RpcApplication;
import com.yupi.yurpc.constant.AsyncKeys;
import com.yupi.yurpc.constant.RpcConstant;
import com.yupi.yurpc.config.RpcConfig;
import com.yupi.yurpc.fault.retry.RetryStrategy;
import com.yupi.yurpc.fault.retry.RetryStrategyFactory;
import com.yupi.yurpc.fault.tolerant.TolerantStrategy;
import com.yupi.yurpc.fault.tolerant.TolerantStrategyFactory;
import com.yupi.yurpc.loadbalancer.LoadBalancer;
import com.yupi.yurpc.loadbalancer.LoadBalancerFactory;
import com.yupi.yurpc.model.RpcRequest;
import com.yupi.yurpc.model.RpcResponse;
import com.yupi.yurpc.model.ServiceMetaInfo;
import com.yupi.yurpc.proxy.ServiceProxy;
import com.yupi.yurpc.registry.Registry;
import com.yupi.yurpc.registry.RegistryFactory;
import com.yupi.yurpc.server.tcp.VertxTcpClient;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * 异步服务代理
 *
 * 支持同步和异步两种调用方式
 * - 同步方法：直接返回结果
 * - 异步方法：返回AsyncResult
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @learn <a href="https://codefather.cn">鱼皮的编程宝典</a>
 * @from <a href="https://yupi.icu">编程导航学习圈</a>
 */
public class AsyncServiceProxy implements InvocationHandler {

    /**
     * 线程池
     */
    private static ExecutorService executorService;

    /**
     * 同步服务代理（用于同步调用）
     */
    private final ServiceProxy syncServiceProxy;

    /**
     * 初始化线程池
     */
    static {
        RpcConfig.ThreadPoolConfig config = RpcApplication.getRpcConfig().getAsyncThreadPoolConfig();
        executorService = Executors.newFixedThreadPool(config.getMaxPoolSize(), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("yurpc-async-" + thread.getId());
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    /**
     * 构造函数
     */
    public AsyncServiceProxy() {
        this.syncServiceProxy = new ServiceProxy();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 检查返回类型是否为AsyncResult
        Class<?> returnType = method.getReturnType();
        if (AsyncResult.class.isAssignableFrom(returnType)) {
            // 异步调用
            return invokeAsync(method, args);
        } else {
            // 同步调用，委托给原ServiceProxy
            return syncServiceProxy.invoke(proxy, method, args);
        }
    }

    /**
     * 执行异步调用
     *
     * @param method 方法
     * @param args 参数
     * @param <T> 返回类型
     * @return AsyncResult
     */
    @SuppressWarnings("unchecked")
    private <T> AsyncResult<T> invokeAsync(Method method, Object[] args) {
        // 生成请求ID
        String requestId = IdUtil.getSnowflakeNextIdStr();

        // 创建CompletableFuture
        CompletableFuture<T> future = new CompletableFuture<>();

        // 在线程池中执行异步调用
        executorService.submit(() -> {
            try {
                // 构造请求
                RpcRequest rpcRequest = buildRpcRequest(method, args);

                // 执行RPC调用
                RpcResponse rpcResponse = doRequest(rpcRequest);

                // 完成future
                future.complete((T) rpcResponse.getData());
            } catch (Exception e) {
                // 异常完成future
                future.completeExceptionally(e);
            }
        });

        return new AsyncResult<>(future, requestId);
    }

    /**
     * 构造RPC请求
     *
     * @param method 方法
     * @param args 参数
     * @return RPC请求
     */
    private RpcRequest buildRpcRequest(Method method, Object[] args) {
        String serviceName = method.getDeclaringClass().getName();
        return RpcRequest.builder()
                .serviceName(serviceName)
                .methodName(method.getName())
                .parameterTypes(method.getParameterTypes())
                .args(args)
                .build();
    }

    /**
     * 执行RPC请求
     *
     * @param rpcRequest RPC请求
     * @return RPC响应
     * @throws Exception 异常
     */
    private RpcResponse doRequest(RpcRequest rpcRequest) throws Exception {
        // 从注册中心获取服务提供者
        RpcConfig rpcConfig = RpcApplication.getRpcConfig();
        Registry registry = RegistryFactory.getInstance(rpcConfig.getRegistryConfig().getRegistry());
        ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
        serviceMetaInfo.setServiceName(rpcRequest.getServiceName());
        serviceMetaInfo.setServiceVersion(RpcConstant.DEFAULT_SERVICE_VERSION);

        // 服务发现
        String serviceKey = serviceMetaInfo.getServiceKey();
        List<ServiceMetaInfo> serviceMetaInfoList = registry.serviceDiscovery(serviceKey);
        if (CollUtil.isEmpty(serviceMetaInfoList)) {
            throw new RuntimeException("暂无服务地址");
        }

        // 负载均衡
        LoadBalancer loadBalancer = LoadBalancerFactory.getInstance(rpcConfig.getLoadBalancer());
        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("methodName", rpcRequest.getMethodName());
        ServiceMetaInfo selectedServiceMetaInfo = loadBalancer.select(requestParams, serviceMetaInfoList);

        // 使用重试机制
        RpcResponse rpcResponse;
        try {
            RetryStrategy retryStrategy = RetryStrategyFactory.getInstance(rpcConfig.getRetryStrategy());
            rpcResponse = retryStrategy.doRetry(() ->
                    VertxTcpClient.doRequest(rpcRequest, selectedServiceMetaInfo)
            );
        } catch (Exception e) {
            // 容错机制
            TolerantStrategy tolerantStrategy = TolerantStrategyFactory.getInstance(rpcConfig.getTolerantStrategy());
            rpcResponse = tolerantStrategy.doTolerant(null, e);
        }

        return rpcResponse;
    }

    /**
     * 关闭线程池
     */
    public static void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}