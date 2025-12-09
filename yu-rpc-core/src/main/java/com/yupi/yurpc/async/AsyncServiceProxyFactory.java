package com.yupi.yurpc.async;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 异步服务代理工厂
 *
 * 用于创建支持异步调用的服务代理
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @learn <a href="https://codefather.cn">鱼皮的编程宝典</a>
 * @from <a href="https://yupi.icu">编程导航学习圈</a>
 */
public class AsyncServiceProxyFactory {

    /**
     * 代理缓存（服务类 -> 代理实例）
     */
    private static final Map<Class<?>, Object> PROXY_CACHE = new ConcurrentHashMap<>();

    /**
     * 获取服务代理
     *
     * @param serviceClass 服务接口类
     * @param <T> 服务类型
     * @return 服务代理实例
     */
    @SuppressWarnings("unchecked")
    public static <T> T getProxy(Class<T> serviceClass) {
        // 双重检查锁模式
        Object proxy = PROXY_CACHE.get(serviceClass);
        if (proxy == null) {
            synchronized (serviceClass) {
                proxy = PROXY_CACHE.get(serviceClass);
                if (proxy == null) {
                    // 创建代理实例
                    proxy = Proxy.newProxyInstance(
                            serviceClass.getClassLoader(),
                            new Class[]{serviceClass},
                            new AsyncServiceProxy()
                    );
                    // 放入缓存
                    PROXY_CACHE.put(serviceClass, proxy);
                }
            }
        }
        return (T) proxy;
    }

    /**
     * 创建新的代理实例（不使用缓存）
     *
     * @param serviceClass 服务接口类
     * @param <T> 服务类型
     * @return 新的服务代理实例
     */
    @SuppressWarnings("unchecked")
    public static <T> T newProxy(Class<T> serviceClass) {
        return (T) Proxy.newProxyInstance(
                serviceClass.getClassLoader(),
                new Class[]{serviceClass},
                new AsyncServiceProxy()
        );
    }

    /**
     * 清空代理缓存
     */
    public static void clearCache() {
        PROXY_CACHE.clear();
    }

    /**
     * 从缓存中移除指定服务的代理
     *
     * @param serviceClass 服务接口类
     */
    public static void removeProxy(Class<?> serviceClass) {
        PROXY_CACHE.remove(serviceClass);
    }

    /**
     * 获取缓存的代理数量
     *
     * @return 代理数量
     */
    public static int getCachedProxyCount() {
        return PROXY_CACHE.size();
    }

    /**
     * 关闭工厂资源
     */
    public static void shutdown() {
        // 清空缓存
        clearCache();
        // 关闭AsyncServiceProxy的线程池
        AsyncServiceProxy.shutdown();
    }
}