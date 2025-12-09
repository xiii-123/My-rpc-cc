package com.yupi.yurpc.constant;

/**
 * 异步调用相关常量
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @learn <a href="https://codefather.cn">鱼皮的编程宝典</a>
 * @from <a href="https://yupi.icu">编程导航学习圈</a>
 */
public interface AsyncKeys {

    /**
     * 异步调用方式：CompletableFuture
     */
    String COMPLETABLE_FUTURE = "completableFuture";

    /**
     * 异步调用方式：Future
     */
    String FUTURE = "future";

    /**
     * 异步调用方式：Callback
     */
    String CALLBACK = "callback";

    /**
     * 默认超时时间（毫秒）
     */
    long DEFAULT_TIMEOUT = 30000L;

    /**
     * 默认核心线程池大小
     */
    int DEFAULT_CORE_POOL_SIZE = 10;

    /**
     * 默认最大线程池大小
     */
    int DEFAULT_MAX_POOL_SIZE = 50;

    /**
     * 默认线程保活时间（秒）
     */
    long DEFAULT_KEEP_ALIVE_TIME = 60L;

    /**
     * 默认队列容量
     */
    int DEFAULT_QUEUE_CAPACITY = 1000;

    /**
     * 异步调用结果缓存前缀
     */
    String ASYNC_RESULT_CACHE_PREFIX = "async:result:";

    /**
     * 异步调用上下文键
     */
    String ASYNC_CONTEXT_KEY = "asyncContext";

    /**
     * 异步调用请求ID键
     */
    String ASYNC_REQUEST_ID_KEY = "asyncRequestId";

    /**
     * 异步调用超时键
     */
    String ASYNC_TIMEOUT_KEY = "asyncTimeout";
}