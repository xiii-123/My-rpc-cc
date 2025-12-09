package com.yupi.yurpc.server.tcp;

import cn.hutool.core.util.IdUtil;
import com.yupi.yurpc.RpcApplication;
import com.yupi.yurpc.model.RpcRequest;
import com.yupi.yurpc.model.RpcResponse;
import com.yupi.yurpc.model.ServiceMetaInfo;
import com.yupi.yurpc.protocol.*;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetSocket;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Vertx TCP 请求客户端
 *
 * 支持同步和异步两种调用方式
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @learn <a href="https://codefather.cn">程序员鱼皮的编程宝典</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
public class VertxTcpClient {

    /**
     * 单例 Vertx 实例
     */
    private static final Vertx VERTX = Vertx.vertx();

    /**
     * 单例 NetClient 实例（连接复用）
     */
    private static final NetClient NET_CLIENT = VERTX.createNetClient();

    /**
     * 发送请求（同步方式，保持向后兼容）
     *
     * @param rpcRequest       RPC请求
     * @param serviceMetaInfo  服务元信息
     * @return RPC响应
     * @throws InterruptedException 中断异常
     * @throws ExecutionException  执行异常
     */
    public static RpcResponse doRequest(RpcRequest rpcRequest, ServiceMetaInfo serviceMetaInfo)
            throws InterruptedException, ExecutionException {
        CompletableFuture<RpcResponse> future = doRequestAsync(rpcRequest, serviceMetaInfo);
        try {
            return future.get();
        } catch (Exception e) {
            throw new ExecutionException(e);
        }
    }

    /**
     * 发送请求（异步方式）
     *
     * @param rpcRequest       RPC请求
     * @param serviceMetaInfo  服务元信息
     * @return CompletableFuture<RpcResponse>
     */
    public static CompletableFuture<RpcResponse> doRequestAsync(RpcRequest rpcRequest, ServiceMetaInfo serviceMetaInfo) {
        CompletableFuture<RpcResponse> responseFuture = new CompletableFuture<>();

        // 连接到服务器
        NET_CLIENT.connect(serviceMetaInfo.getServicePort(), serviceMetaInfo.getServiceHost(),
                result -> {
                    if (!result.succeeded()) {
                        responseFuture.completeExceptionally(
                            new RuntimeException("Failed to connect to TCP server: " + result.cause().getMessage())
                        );
                        return;
                    }

                    NetSocket socket = result.result();

                    // 构造协议消息
                    ProtocolMessage<RpcRequest> protocolMessage = buildProtocolMessage(rpcRequest);

                    try {
                        // 编码并发送请求
                        Buffer encodeBuffer = ProtocolMessageEncoder.encode(protocolMessage);
                        socket.write(encodeBuffer);
                    } catch (IOException e) {
                        responseFuture.completeExceptionally(
                            new RuntimeException("协议消息编码错误", e)
                        );
                        return;
                    }

                    // 设置响应处理器
                    TcpBufferHandlerWrapper bufferHandlerWrapper = new TcpBufferHandlerWrapper(
                            buffer -> {
                                try {
                                    ProtocolMessage<RpcResponse> rpcResponseProtocolMessage =
                                            (ProtocolMessage<RpcResponse>) ProtocolMessageDecoder.decode(buffer);
                                    responseFuture.complete(rpcResponseProtocolMessage.getBody());
                                } catch (IOException e) {
                                    responseFuture.completeExceptionally(
                                        new RuntimeException("协议消息解码错误", e)
                                    );
                                }
                            }
                    );

                    socket.handler(bufferHandlerWrapper);

                    // 设置异常处理器
                    socket.exceptionHandler(e -> {
                        responseFuture.completeExceptionally(e);
                    });

                    // 设置关闭处理器
                    socket.closeHandler(v -> {
                        if (!responseFuture.isDone()) {
                            responseFuture.completeExceptionally(
                                new RuntimeException("Connection closed before receiving response")
                            );
                        }
                    });
                });

        return responseFuture;
    }

    /**
     * 发送请求（异步方式，带超时）
     *
     * @param rpcRequest       RPC请求
     * @param serviceMetaInfo  服务元信息
     * @param timeout          超时时间
     * @param unit             时间单位
     * @return CompletableFuture<RpcResponse>
     */
    public static CompletableFuture<RpcResponse> doRequestAsync(
            RpcRequest rpcRequest,
            ServiceMetaInfo serviceMetaInfo,
            long timeout,
            TimeUnit unit) {

        CompletableFuture<RpcResponse> future = doRequestAsync(rpcRequest, serviceMetaInfo);

        // 添加超时处理
        return future.orTimeout(timeout, unit)
                .exceptionally(e -> {
                    if (e instanceof java.util.concurrent.TimeoutException) {
                        throw new RuntimeException("RPC request timeout", e);
                    }
                    throw new RuntimeException("RPC request failed", e);
                });
    }

    /**
     * 构建协议消息
     *
     * @param rpcRequest RPC请求
     * @return 协议消息
     */
    private static ProtocolMessage<RpcRequest> buildProtocolMessage(RpcRequest rpcRequest) {
        ProtocolMessage<RpcRequest> protocolMessage = new ProtocolMessage<>();
        ProtocolMessage.Header header = new ProtocolMessage.Header();

        header.setMagic(ProtocolConstant.PROTOCOL_MAGIC);
        header.setVersion(ProtocolConstant.PROTOCOL_VERSION);
        header.setSerializer((byte) ProtocolMessageSerializerEnum.getEnumByValue(
                RpcApplication.getRpcConfig().getSerializer()).getKey());
        header.setType((byte) ProtocolMessageTypeEnum.REQUEST.getKey());
        // 生成全局请求 ID
        header.setRequestId(IdUtil.getSnowflakeNextId());

        protocolMessage.setHeader(header);
        protocolMessage.setBody(rpcRequest);

        return protocolMessage;
    }

    /**
     * 关闭客户端
     */
    public static void close() {
        if (NET_CLIENT != null) {
            NET_CLIENT.close();
        }
        if (VERTX != null) {
            VERTX.close();
        }
    }
}
