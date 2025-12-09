package com.yupi.yurpc.async;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 异步结果封装类
 *
 * 封装CompletableFuture，提供更友好的API
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @learn <a href="https://codefather.cn">鱼皮的编程宝典</a>
 * @from <a href="https://yupi.icu">编程导航学习圈</a>
 */
public class AsyncResult<T> {

    private final CompletableFuture<T> future;
    private final String requestId;

    /**
     * 构造函数
     *
     * @param future CompletableFuture实例
     * @param requestId 请求ID
     */
    public AsyncResult(CompletableFuture<T> future, String requestId) {
        this.future = future;
        this.requestId = requestId;
    }

    /**
     * 阻塞获取结果
     *
     * @return 结果
     * @throws ExecutionException 执行异常
     * @throws InterruptedException 中断异常
     */
    public T get() throws ExecutionException, InterruptedException {
        return future.get();
    }

    /**
     * 带超时时间的阻塞获取结果
     *
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return 结果
     * @throws ExecutionException 执行异常
     * @throws InterruptedException 中断异常
     * @throws TimeoutException 超时异常
     */
    public T get(long timeout, TimeUnit unit)
            throws ExecutionException, InterruptedException, TimeoutException {
        return future.get(timeout, unit);
    }

    /**
     * 非阻塞检查是否完成
     *
     * @return 是否完成
     */
    public boolean isDone() {
        return future.isDone();
    }

    /**
     * 检查是否异常完成
     *
     * @return 是否异常
     */
    public boolean isCompletedExceptionally() {
        return future.isCompletedExceptionally();
    }

    /**
     * 注册完成回调
     *
     * @param action 回调动作
     * @return 当前AsyncResult实例（支持链式调用）
     */
    public AsyncResult<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
        future.whenComplete(action);
        return this;
    }

    /**
     * 注册成功回调
     *
     * @param action 成功时的回调
     * @return 当前AsyncResult实例（支持链式调用）
     */
    public AsyncResult<T> thenAccept(java.util.function.Consumer<? super T> action) {
        future.thenAccept(action);
        return this;
    }

    /**
     * 注册转换回调
     *
     * @param fn 转换函数
     * @param <U> 转换后的类型
     * @return 新的AsyncResult实例
     */
    public <U> AsyncResult<U> thenApply(Function<? super T, ? extends U> fn) {
        return new AsyncResult<>(future.thenApply(fn), requestId);
    }

    /**
     * 注册异常处理回调
     *
     * @param fn 异常处理函数
     * @return 当前AsyncResult实例（支持链式调用）
     */
    public AsyncResult<T> exceptionally(Function<Throwable, ? extends T> fn) {
        future.exceptionally(fn);
        return this;
    }

    /**
     * 组合另一个AsyncResult
     *
     * @param other 另一个AsyncResult
     * @param fn 组合函数
     * @param <U> 另一个AsyncResult的结果类型
     * @param <V> 组合后的结果类型
     * @return 新的AsyncResult实例
     */
    public <U, V> AsyncResult<V> thenCombine(
            AsyncResult<U> other,
            java.util.function.BiFunction<? super T, ? super U, ? extends V> fn) {
        CompletableFuture<V> combined = future.thenCombine(other.future, fn);
        return new AsyncResult<>(combined, requestId);
    }

    /**
     * 完成future
     *
     * @param value 结果值
     */
    public void complete(T value) {
        future.complete(value);
    }

    /**
     * 异常完成future
     *
     * @param ex 异常
     */
    public void completeExceptionally(Throwable ex) {
        future.completeExceptionally(ex);
    }

    /**
     * 取消future
     *
     * @param mayInterruptIfRunning 是否中断正在运行的任务
     * @return 是否取消成功
     */
    public boolean cancel(boolean mayInterruptIfRunning) {
        return future.cancel(mayInterruptIfRunning);
    }

    /**
     * 检查是否已取消
     *
     * @return 是否已取消
     */
    public boolean isCancelled() {
        return future.isCancelled();
    }

    /**
     * 获取请求ID
     *
     * @return 请求ID
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * 静态工厂方法
     *
     * @param <T> 结果类型
     * @return 新的AsyncResult实例
     */
    public static <T> AsyncResult<T> create() {
        return new AsyncResult<>(new CompletableFuture<>(), null);
    }

    /**
     * 静态工厂方法
     *
     * @param future CompletableFuture实例
     * @param requestId 请求ID
     * @param <T> 结果类型
     * @return 新的AsyncResult实例
     */
    public static <T> AsyncResult<T> of(CompletableFuture<T> future, String requestId) {
        return new AsyncResult<>(future, requestId);
    }

    /**
     * 创建已完成的AsyncResult
     *
     * @param value 结果值
     * @param <T> 结果类型
     * @return 已完成的AsyncResult实例
     */
    public static <T> AsyncResult<T> completed(T value) {
        return new AsyncResult<>(CompletableFuture.completedFuture(value), null);
    }

    /**
     * 创建异常完成的AsyncResult
     *
     * @param ex 异常
     * @param <T> 结果类型
     * @return 异常完成的AsyncResult实例
     */
    public static <T> AsyncResult<T> failed(Throwable ex) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(ex);
        return new AsyncResult<>(future, null);
    }

    @Override
    public String toString() {
        return "AsyncResult{" +
                "requestId='" + requestId + '\'' +
                ", isDone=" + isDone() +
                ", isCompletedExceptionally=" + isCompletedExceptionally() +
                '}';
    }
}