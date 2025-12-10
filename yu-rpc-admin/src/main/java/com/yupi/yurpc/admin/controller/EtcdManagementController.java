package com.yupi.yurpc.admin.controller;

import com.yupi.yurpc.admin.dto.KeyValueDTO;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.options.GetOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * ETCD管理控制器
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 */
@RestController
@RequestMapping("/api/etcd")
@CrossOrigin
public class EtcdManagementController {

    @Value("${etcd.address:http://localhost:2379}")
    private String etcdAddress;

    private Client etcdClient;

    /**
     * 初始化ETCD客户端
     */
    private Client getEtcdClient() {
        if (etcdClient == null) {
            etcdClient = Client.builder()
                    .endpoints(etcdAddress)
                    .build();
        }
        return etcdClient;
    }

    /**
     * 根据前缀查询键值对
     *
     * @param prefix 键前缀
     * @return 键值对列表
     */
    @GetMapping("/keys")
    public List<KeyValueDTO> getKeys(@RequestParam String prefix) {
        try {
            // 正确方式：创建 GetOption 并设置前缀查询
            GetOption getOption = GetOption.builder()
                    .withPrefix(ByteSequence.from(prefix, StandardCharsets.UTF_8))
                    .build();

            CompletableFuture<GetResponse> future =
                    getEtcdClient().getKVClient()
                            .get(ByteSequence.from(prefix, StandardCharsets.UTF_8), getOption);

            GetResponse response = future.get();
            return response.getKvs().stream()
                    .map(KeyValueDTO::fromEtcdKeyValue)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get keys with prefix: " + e.getMessage(), e);
        }
    }

    /**
     * 获取单个键值对
     *
     * @param key 键
     * @return 键值对
     */
    @GetMapping("/key")
    public KeyValueDTO getKey(@RequestParam String key) {
        try {
            CompletableFuture<GetResponse> future =
                    getEtcdClient().getKVClient().get(
                            ByteSequence.from(key, StandardCharsets.UTF_8));

            GetResponse response = future.get();
            List<io.etcd.jetcd.KeyValue> keyValues = response.getKvs();
            if (keyValues.isEmpty()) {
                throw new RuntimeException("Key not found: " + key);
            }
            return KeyValueDTO.fromEtcdKeyValue(keyValues.get(0));
        } catch (Exception e) {
            throw new RuntimeException("Failed to get key: " + e.getMessage(), e);
        }
    }

    /**
     * 设置键值对
     *
     * @param dto 键值对DTO
     */
    @PostMapping("/key")
    public void setKey(@RequestBody KeyValueDTO dto) {
        try {
            getEtcdClient().getKVClient().put(
                    ByteSequence.from(dto.getKey(), StandardCharsets.UTF_8),
                    ByteSequence.from(dto.getValue(), StandardCharsets.UTF_8)).get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to set key: " + e.getMessage(), e);
        }
    }

    /**
     * 更新键值对
     *
     * @param dto 键值对DTO
     */
    @PutMapping("/key")
    public void updateKey(@RequestBody KeyValueDTO dto) {
        // ETCD的put本身就是更新操作
        setKey(dto);
    }

    /**
     * 删除键值对
     *
     * @param key 键
     */
    @DeleteMapping("/key")
    public void deleteKey(@RequestParam String key) {
        try {
            getEtcdClient().getKVClient().delete(
                    ByteSequence.from(key, StandardCharsets.UTF_8)).get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete key: " + e.getMessage(), e);
        }
    }

    /**
     * 批量删除键值对
     *
     * @param prefix 键前缀
     */
    @DeleteMapping("/keys")
    public void deleteKeys(@RequestParam String prefix) {
        try {
            CompletableFuture<GetResponse> future =
                    getEtcdClient().getKVClient().get(
                            ByteSequence.from(prefix, StandardCharsets.UTF_8));

            GetResponse response = future.get();
            for (io.etcd.jetcd.KeyValue keyValue : response.getKvs()) {
                String key = keyValue.getKey().toString(StandardCharsets.UTF_8);
                getEtcdClient().getKVClient().delete(
                        ByteSequence.from(key, StandardCharsets.UTF_8)).get();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete keys: " + e.getMessage(), e);
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
    @PostMapping("/strategy/{serviceName}/{version}/{host}/{port}")
    public void publishNodeStrategy(
            @PathVariable String serviceName,
            @PathVariable String version,
            @PathVariable String host,
            @PathVariable Integer port,
            @RequestParam String strategyType,
            @RequestParam String value) {
        try {
            String key = String.format("/rpc/strategy/%s:%s/%s:%d/%s", serviceName, version, host, port, strategyType);
            getEtcdClient().getKVClient().put(
                    ByteSequence.from(key, StandardCharsets.UTF_8),
                    ByteSequence.from(value, StandardCharsets.UTF_8)).get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish strategy: " + e.getMessage(), e);
        }
    }

    /**
     * 获取节点策略配置
     *
     * @param serviceName  服务名称
     * @param version      服务版本
     * @param host         主机地址
     * @param port         端口
     * @param strategyType 策略类型
     * @return 策略值
     */
    @GetMapping("/strategy/{serviceName}/{version}/{host}/{port}")
    public String getNodeStrategy(
            @PathVariable String serviceName,
            @PathVariable String version,
            @PathVariable String host,
            @PathVariable Integer port,
            @RequestParam String strategyType) {
        try {
            String key = String.format("/rpc/strategy/%s:%s/%s:%d/%s", serviceName, version, host, port, strategyType);
            CompletableFuture<GetResponse> future =
                    getEtcdClient().getKVClient().get(
                            ByteSequence.from(key, StandardCharsets.UTF_8));

            GetResponse response = future.get();
            List<io.etcd.jetcd.KeyValue> keyValues = response.getKvs();
            if (keyValues.isEmpty()) {
                return null;
            }
            return keyValues.get(0).getValue().toString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get strategy: " + e.getMessage(), e);
        }
    }
}