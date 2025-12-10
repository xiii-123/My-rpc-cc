package com.yupi.yurpc.registry;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.task.Task;
import cn.hutool.json.JSONUtil;
import com.yupi.yurpc.config.RegistryConfig;
import com.yupi.yurpc.model.ServiceMetaInfo;
import io.etcd.jetcd.*;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.kv.PutResponse;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;
import io.etcd.jetcd.watch.WatchEvent;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Etcd 注册中心
 *
 * @author <a href="https://github.com/liyupi">coder_yupi</a>
 * @from <a href="https://yupi.icu">编程导航学习圈</a>
 * @learn <a href="https://codefather.cn">yupi 的编程宝典</a>
 */
public class EtcdRegistry implements Registry {

    private Client client;

    private KV kvClient;

    /**
     * 本机注册的节点 key 集合（用于维护续期）
     */
    private final Set<String> localRegisterNodeKeySet = new HashSet<>();

    /**
     * 注册中心服务缓存（只支持单个服务缓存，已废弃，请使用下方的 RegistryServiceMultiCache）
     */
    @Deprecated
    private final RegistryServiceCache registryServiceCache = new RegistryServiceCache();

    /**
     * 注册中心服务缓存（支持多个服务键）
     */
    private final RegistryServiceMultiCache registryServiceMultiCache = new RegistryServiceMultiCache();

    /**
     * 正在监听的 key 集合
     */
    private final Set<String> watchingKeySet = new ConcurrentHashSet<>();

    /**
     * 根节点
     */
    private static final String ETCD_ROOT_PATH = "/rpc/";

    @Override
    public void init(RegistryConfig registryConfig) {
        client = Client.builder()
                .endpoints(registryConfig.getAddress())
                .connectTimeout(Duration.ofMillis(registryConfig.getTimeout()))
                .build();
        kvClient = client.getKVClient();
        heartBeat();
    }

    @Override
    public void register(ServiceMetaInfo serviceMetaInfo) throws Exception {
        // 创建 Lease 和 KV 客户端
        Lease leaseClient = client.getLeaseClient();

        // 创建一个 30 秒的租约
        long leaseId = leaseClient.grant(30).get().getID();

        // 设置要存储的键值对
        String registerKey = ETCD_ROOT_PATH + serviceMetaInfo.getServiceNodeKey();
        ByteSequence key = ByteSequence.from(registerKey, StandardCharsets.UTF_8);
        ByteSequence value = ByteSequence.from(JSONUtil.toJsonStr(serviceMetaInfo), StandardCharsets.UTF_8);

        // 将键值对与租约关联起来，并设置过期时间
        PutOption putOption = PutOption.builder().withLeaseId(leaseId).build();
        kvClient.put(key, value, putOption).get();
        // 添加节点信息到本地缓存
        localRegisterNodeKeySet.add(registerKey);
    }

    @Override
    public void unRegister(ServiceMetaInfo serviceMetaInfo) {
        String registerKey = ETCD_ROOT_PATH + serviceMetaInfo.getServiceNodeKey();
        kvClient.delete(ByteSequence.from(registerKey, StandardCharsets.UTF_8));
        // 也要从本地缓存移除
        localRegisterNodeKeySet.remove(registerKey);
    }

    @Override
    public List<ServiceMetaInfo> serviceDiscovery(String serviceKey) {
        // 优先从缓存获取服务
        // 原教程代码，不支持多个服务同时缓存
        // List<ServiceMetaInfo> cachedServiceMetaInfoList = registryServiceCache.readCache();
        // 优化后的代码，支持多个服务同时缓存
        List<ServiceMetaInfo> cachedServiceMetaInfoList = registryServiceMultiCache.readCache(serviceKey);
        if (cachedServiceMetaInfoList != null) {
            return cachedServiceMetaInfoList;
        }

        // 前缀搜索，结尾一定要加 '/'
        String searchPrefix = ETCD_ROOT_PATH + serviceKey + "/";

        try {
            // 前缀查询
            GetOption getOption = GetOption.builder().isPrefix(true).build();
            List<KeyValue> keyValues = kvClient.get(
                            ByteSequence.from(searchPrefix, StandardCharsets.UTF_8),
                            getOption)
                    .get()
                    .getKvs();
            // 解析服务信息
            List<ServiceMetaInfo> serviceMetaInfoList = keyValues.stream()
                    .map(keyValue -> {
                        String key = keyValue.getKey().toString(StandardCharsets.UTF_8);
                        // 监听 key 的变化
                        watch(key);
                        String value = keyValue.getValue().toString(StandardCharsets.UTF_8);
                        return JSONUtil.toBean(value, ServiceMetaInfo.class);
                    })
                    .collect(Collectors.toList());
            // 写入服务缓存
            // 原教程代码，不支持多个服务同时缓存
            // registryServiceCache.writeCache(serviceMetaInfoList);
            // 优化后的代码，支持多个服务同时缓存
            registryServiceMultiCache.writeCache(serviceKey, serviceMetaInfoList);
            return serviceMetaInfoList;
        } catch (Exception e) {
            throw new RuntimeException("获取服务列表失败", e);
        }
    }

    @Override
    public void heartBeat() {
        // 10 秒续签一次
        CronUtil.schedule("*/10 * * * * *", new Task() {
            @Override
            public void execute() {
                // 遍历本节点所有的 key
                for (String key : localRegisterNodeKeySet) {
                    try {
                        List<KeyValue> keyValues = kvClient.get(ByteSequence.from(key, StandardCharsets.UTF_8))
                                .get()
                                .getKvs();
                        // 该节点已过期（需要重启节点才能重新注册）
                        if (CollUtil.isEmpty(keyValues)) {
                            continue;
                        }
                        // 节点未过期，重新注册（相当于续签）
                        KeyValue keyValue = keyValues.get(0);
                        String value = keyValue.getValue().toString(StandardCharsets.UTF_8);
                        ServiceMetaInfo serviceMetaInfo = JSONUtil.toBean(value, ServiceMetaInfo.class);
                        register(serviceMetaInfo);
                    } catch (Exception e) {
                        throw new RuntimeException(key + "续签失败", e);
                    }
                }
            }
        });

        // 支持秒级别定时任务
        CronUtil.setMatchSecond(true);
        CronUtil.start();
    }

    /**
     * 监听（消费端）
     *
     * @param serviceNodeKey
     */
    @Override
    public void watch(String serviceNodeKey) {
        Watch watchClient = client.getWatchClient();
        // 之前未被监听，开启监听
        boolean newWatch = watchingKeySet.add(serviceNodeKey);
        if (newWatch) {
            watchClient.watch(ByteSequence.from(serviceNodeKey, StandardCharsets.UTF_8), response -> {
                for (WatchEvent event : response.getEvents()) {
                    switch (event.getEventType()) {
                        // key 删除时触发
                        case DELETE:
                            // 清理注册服务缓存
                            // 原教程代码，不支持多个服务同时缓存
                            // registryServiceCache.clearCache();
                            // 优化后的代码，支持多个服务同时缓存
                            // fixme 这里需要改为 serviceKey，而不是 serviceNodeKey
                            registryServiceMultiCache.clearCache(serviceNodeKey);
                            break;
                        case PUT:
                        default:
                            break;
                    }
                }
            });
        }
    }

    @Override
    public void destroy() {
        System.out.println("当前节点下线");
        // 下线节点
        // 遍历本节点所有的 key
        for (String key : localRegisterNodeKeySet) {
            try {
                kvClient.delete(ByteSequence.from(key, StandardCharsets.UTF_8)).get();
            } catch (Exception e) {
                throw new RuntimeException(key + "节点下线失败");
            }
        }

        // 释放资源
        if (kvClient != null) {
            kvClient.close();
        }
        if (client != null) {
            client.close();
        }
    }

    // ========== 配置中心相关方法 ==========

    /**
     * 获取节点策略配置
     *
     * @param serviceName   服务名称
     * @param version       服务版本
     * @param host          主机地址
     * @param port          端口
     * @param strategyType  策略类型 (loadbalance, retry, tolerant, weight)
     * @return 策略配置值
     */
    public String getNodeStrategy(String serviceName, String version, String host, int port, String strategyType) {
        String key = String.format("/rpc/strategy/%s:%s/%s:%d/%s", serviceName, version, host, port, strategyType);
        try {
            GetResponse response = kvClient.get(ByteSequence.from(key, StandardCharsets.UTF_8)).get();
            List<KeyValue> keyValues = response.getKvs();
            if (keyValues.isEmpty()) {
                return null;
            }
            return keyValues.get(0).getValue().toString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("Failed to get node strategy for key: " + key + ", error: " + e.getMessage());
            return null;
        }
    }

    /**
     * 发布节点策略配置
     *
     * @param serviceName   服务名称
     * @param version       服务版本
     * @param host          主机地址
     * @param port          端口
     * @param strategyType  策略类型
     * @param value         策略值
     */
    public void publishNodeStrategy(String serviceName, String version, String host, int port,
                                   String strategyType, String value) {
        String key = String.format("/rpc/strategy/%s:%s/%s:%d/%s", serviceName, version, host, port, strategyType);
        try {
            ByteSequence keyByteSeq = ByteSequence.from(key, StandardCharsets.UTF_8);
            ByteSequence valueByteSeq = ByteSequence.from(value, StandardCharsets.UTF_8);
            PutResponse response = kvClient.put(keyByteSeq, valueByteSeq).get();
            System.out.println("Published strategy config - Key: " + key + ", Value: " + value);
        } catch (Exception e) {
            System.err.println("Failed to publish node strategy for key: " + key + ", error: " + e.getMessage());
            throw new RuntimeException("发布节点策略配置失败", e);
        }
    }

    /**
     * 记录监控指标
     *
     * @param serviceName  服务名称
     * @param version      服务版本
     * @param host         主机地址
     * @param port         端口
     * @param metricType   指标类型 (calls, success, failure, avg_time)
     * @param value        指标值
     */
    public void recordMetrics(String serviceName, String version, String host, int port,
                            String metricType, String value) {
        String key = String.format("/rpc/metrics/%s:%s/%s:%d/%s", serviceName, version, host, port, metricType);
        try {
            ByteSequence keyByteSeq = ByteSequence.from(key, StandardCharsets.UTF_8);
            ByteSequence valueByteSeq = ByteSequence.from(value, StandardCharsets.UTF_8);
            kvClient.put(keyByteSeq, valueByteSeq).get();
        } catch (Exception e) {
            // 监控记录失败不应该影响主业务流程
            System.err.println("Failed to record metrics for key: " + key + ", error: " + e.getMessage());
        }
    }

    /**
     * 增量更新监控指标
     *
     * @param serviceName  服务名称
     * @param version      服务版本
     * @param host         主机地址
     * @param port         端口
     * @param metricType   指标类型
     * @param increment    增量值
     */
    public void incrementMetrics(String serviceName, String version, String host, int port,
                                String metricType, long increment) {
        String key = String.format("/rpc/metrics/%s:%s/%s:%d/%s", serviceName, version, host, port, metricType);
        try {
            // 先获取当前值
            GetResponse response = kvClient.get(ByteSequence.from(key, StandardCharsets.UTF_8)).get();
            List<KeyValue> keyValues = response.getKvs();

            long currentValue = 0;
            if (!keyValues.isEmpty()) {
                String valueStr = keyValues.get(0).getValue().toString(StandardCharsets.UTF_8);
                try {
                    currentValue = Long.parseLong(valueStr);
                } catch (NumberFormatException e) {
                    currentValue = 0;
                }
            }

            // 更新值
            long newValue = currentValue + increment;
            ByteSequence keyByteSeq = ByteSequence.from(key, StandardCharsets.UTF_8);
            ByteSequence valueByteSeq = ByteSequence.from(String.valueOf(newValue), StandardCharsets.UTF_8);
            kvClient.put(keyByteSeq, valueByteSeq).get();

        } catch (Exception e) {
            // 监控记录失败不应该影响主业务流程
            System.err.println("Failed to increment metrics for key: " + key + ", error: " + e.getMessage());
        }
    }

    /**
     * 获取所有键值对（用于管理界面）
     *
     * @param prefix 键前缀
     * @return 键值对列表
     */
    public List<KeyValue> getAllKeys(String prefix) {
        try {
            GetOption getOption = GetOption.builder().isPrefix(true).build();
            GetResponse response = kvClient.get(ByteSequence.from(prefix, StandardCharsets.UTF_8), getOption).get();
            return response.getKvs();
        } catch (Exception e) {
            System.err.println("Failed to get all keys with prefix: " + prefix + ", error: " + e.getMessage());
            throw new RuntimeException("获取键值对失败", e);
        }
    }

    /**
     * 设置键值对（用于管理界面）
     *
     * @param key   键
     * @param value 值
     */
    public void setKeyValue(String key, String value) {
        try {
            ByteSequence keyByteSeq = ByteSequence.from(key, StandardCharsets.UTF_8);
            ByteSequence valueByteSeq = ByteSequence.from(value, StandardCharsets.UTF_8);
            kvClient.put(keyByteSeq, valueByteSeq).get();
        } catch (Exception e) {
            System.err.println("Failed to set key-value - Key: " + key + ", error: " + e.getMessage());
            throw new RuntimeException("设置键值对失败", e);
        }
    }

    /**
     * 删除键值对（用于管理界面）
     *
     * @param key 键
     */
    public void deleteKeyValue(String key) {
        try {
            kvClient.delete(ByteSequence.from(key, StandardCharsets.UTF_8)).get();
        } catch (Exception e) {
            System.err.println("Failed to delete key: " + key + ", error: " + e.getMessage());
            throw new RuntimeException("删除键值对失败", e);
        }
    }

    /**
     * 获取ETCD客户端（用于管理界面）
     *
     * @return ETCD客户端
     */
    public Client getClient() {
        return client;
    }
}
