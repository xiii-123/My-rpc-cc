package com.yupi.yurpc.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yupi.yurpc.admin.YurpcAdminApplication;
import com.yupi.yurpc.admin.controller.EtcdManagementController;
import com.yupi.yurpc.admin.dto.KeyValueDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ETCD管理控制器测试 - 重构版本（粒度更小的单元测试）
 *
 * 测试原则：
 * 1. 每个测试方法只测试一个具体功能
 * 2. 测试方法命名清晰，描述测试目的
 * 3. 避免重复的断言和复杂的测试逻辑
 *
 * @author Claude (Refactored)
 */
@SpringBootTest(classes = YurpcAdminApplication.class)
@AutoConfigureWebMvc
public class EtcdManagementControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private EtcdManagementController etcdManagementController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        System.out.println("=== 测试开始，确保ETCD服务已启动 (http://localhost:2379) ===");
    }

    // ========== 基础CRUD操作 ==========

    @Test
    void testCreateKeyValue() throws Exception {
        System.out.println("\n--- 测试：创建键值对 ---");

        KeyValueDTO dto = new KeyValueDTO();
        dto.setKey("/rpc/test/basic/service1");
        dto.setValue("{\"name\":\"testService\",\"version\":\"1.0\"}");

        mockMvc.perform(post("/api/etcd/key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn();

        System.out.println("✓ 创建键值对成功");
    }

    @Test
    void testGetExistingKeyValue() throws Exception {
        System.out.println("\n--- 测试：获取已存在的键值对 ---");

        String testKey = "/rpc/test/basic/service2";
        String testValue = "{\"name\":\"testService2\",\"version\":\"1.0\"}";

        // 先创建一个键值对
        KeyValueDTO createDto = new KeyValueDTO();
        createDto.setKey(testKey);
        createDto.setValue(testValue);

        mockMvc.perform(post("/api/etcd/key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
                .andReturn();

        Thread.sleep(500);

        // 测试获取键值对
        MvcResult getResult = mockMvc.perform(get("/api/etcd/key")
                .param("key", testKey))
                .andExpect(status().isOk())
                .andReturn();

        KeyValueDTO result = objectMapper.readValue(
            getResult.getResponse().getContentAsString(),
            KeyValueDTO.class
        );

        System.out.println("✓ 获取键值对成功:");
        System.out.println("  Key: " + result.getKey());
        System.out.println("  Value: " + result.getValue());

        assert result.getKey().equals(testKey);
        assert result.getValue().contains("testService2");
    }

    @Test
    void testUpdateKeyValue() throws Exception {
        System.out.println("\n--- 测试：更新键值对 ---");

        String key = "/rpc/test/update/basic";
        String originalValue = "original_value";
        String updatedValue = "updated_value_" + System.currentTimeMillis();

        // 先创建原始键值对
        KeyValueDTO createDto = new KeyValueDTO();
        createDto.setKey(key);
        createDto.setValue(originalValue);

        mockMvc.perform(post("/api/etcd/key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
                .andReturn();

        Thread.sleep(500);

        // 更新键值对
        KeyValueDTO updateDto = new KeyValueDTO();
        updateDto.setKey(key);
        updateDto.setValue(updatedValue);

        mockMvc.perform(put("/api/etcd/key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andReturn();

        System.out.println("✓ 键值对更新操作成功");
    }

    @Test
    void testDeleteKeyValue() throws Exception {
        System.out.println("\n--- 测试：删除键值对 ---");

        String key = "/rpc/test/delete/basic";
        KeyValueDTO dto = new KeyValueDTO();
        dto.setKey(key);
        dto.setValue("to_be_deleted");

        // 先创建键值对
        mockMvc.perform(post("/api/etcd/key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andReturn();

        Thread.sleep(500);

        // 删除键值对
        mockMvc.perform(delete("/api/etcd/key")
                .param("key", key))
                .andExpect(status().isOk())
                .andReturn();

        System.out.println("✓ 键值对删除操作成功");
    }

    // ========== 服务注册测试 ==========

    @Test
    void testCreateServiceRegistration() throws Exception {
        System.out.println("\n--- 测试：创建服务注册信息 ---");

        KeyValueDTO serviceDto = new KeyValueDTO();
        serviceDto.setKey("/rpc/UserService:1.0/localhost:8080");
        serviceDto.setValue("{\"serviceName\":\"UserService\",\"serviceVersion\":\"1.0\"," +
                           "\"serviceHost\":\"localhost\",\"servicePort\":8080,\"serviceGroup\":\"default\"}");

        mockMvc.perform(post("/api/etcd/key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(serviceDto)))
                .andExpect(status().isOk())
                .andReturn();

        System.out.println("✓ 服务注册信息创建成功");
    }

    @Test
    void testGetServiceRegistration() throws Exception {
        System.out.println("\n--- 测试：获取服务注册信息 ---");

        String serviceKey = "/rpc/UserService:1.0/localhost:8081";
        String serviceValue = "{\"serviceName\":\"UserService\",\"serviceVersion\":\"1.0\"," +
                            "\"serviceHost\":\"localhost\",\"servicePort\":8081,\"serviceGroup\":\"default\"}";

        // 先创建服务注册信息
        KeyValueDTO serviceDto = new KeyValueDTO();
        serviceDto.setKey(serviceKey);
        serviceDto.setValue(serviceValue);

        mockMvc.perform(post("/api/etcd/key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(serviceDto)))
                .andReturn();

        Thread.sleep(500);

        // 获取服务注册信息
        MvcResult getResult = mockMvc.perform(get("/api/etcd/key")
                .param("key", serviceKey))
                .andExpect(status().isOk())
                .andReturn();

        KeyValueDTO result = objectMapper.readValue(
            getResult.getResponse().getContentAsString(),
            KeyValueDTO.class
        );

        System.out.println("✓ 获取服务注册信息成功");
        assert result.getKey().equals(serviceKey);
        assert result.getValue().contains("UserService");
    }

    // ========== 策略配置测试 ==========

    @Test
    void testCreateLoadBalanceStrategy() throws Exception {
        System.out.println("\n--- 测试：创建负载均衡策略配置 ---");

        String key = "/rpc/strategy/UserService:1.0/localhost:8082/loadbalance";
        String value = "round_robin";

        KeyValueDTO dto = new KeyValueDTO();
        dto.setKey(key);
        dto.setValue(value);

        mockMvc.perform(post("/api/etcd/key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn();

        System.out.println("✓ 负载均衡策略配置创建成功");
    }

    @Test
    void testCreateRetryStrategy() throws Exception {
        System.out.println("\n--- 测试：创建重试策略配置 ---");

        String key = "/rpc/strategy/UserService:1.0/localhost:8082/retry";
        String value = "fixed_interval";

        KeyValueDTO dto = new KeyValueDTO();
        dto.setKey(key);
        dto.setValue(value);

        mockMvc.perform(post("/api/etcd/key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn();

        System.out.println("✓ 重试策略配置创建成功");
    }

    @Test
    void testCreateTolerantStrategy() throws Exception {
        System.out.println("\n--- 测试：创建容错策略配置 ---");

        String key = "/rpc/strategy/UserService:1.0/localhost:8082/tolerant";
        String value = "fail_fast";

        KeyValueDTO dto = new KeyValueDTO();
        dto.setKey(key);
        dto.setValue(value);

        mockMvc.perform(post("/api/etcd/key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn();

        System.out.println("✓ 容错策略配置创建成功");
    }

    @Test
    void testCreateWeightStrategy() throws Exception {
        System.out.println("\n--- 测试：创建权重策略配置 ---");

        String key = "/rpc/strategy/UserService:1.0/localhost:8082/weight";
        String value = "10";

        KeyValueDTO dto = new KeyValueDTO();
        dto.setKey(key);
        dto.setValue(value);

        mockMvc.perform(post("/api/etcd/key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn();

        System.out.println("✓ 权重策略配置创建成功");
    }

    // ========== 监控指标测试 ==========

    @Test
    void testCreateCallsMetrics() throws Exception {
        System.out.println("\n--- 测试：创建调用次数监控指标 ---");

        String key = "/rpc/metrics/UserService:1.0/localhost:8083/calls";
        String value = "1000";

        KeyValueDTO dto = new KeyValueDTO();
        dto.setKey(key);
        dto.setValue(value);

        mockMvc.perform(post("/api/etcd/key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn();

        System.out.println("✓ 调用次数监控指标创建成功");
    }

    @Test
    void testCreateSuccessMetrics() throws Exception {
        System.out.println("\n--- 测试：创建成功次数监控指标 ---");

        String key = "/rpc/metrics/UserService:1.0/localhost:8083/success";
        String value = "950";

        KeyValueDTO dto = new KeyValueDTO();
        dto.setKey(key);
        dto.setValue(value);

        mockMvc.perform(post("/api/etcd/key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn();

        System.out.println("✓ 成功次数监控指标创建成功");
    }

    @Test
    void testCreateFailureMetrics() throws Exception {
        System.out.println("\n--- 测试：创建失败次数监控指标 ---");

        String key = "/rpc/metrics/UserService:1.0/localhost:8083/failure";
        String value = "50";

        KeyValueDTO dto = new KeyValueDTO();
        dto.setKey(key);
        dto.setValue(value);

        mockMvc.perform(post("/api/etcd/key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn();

        System.out.println("✓ 失败次数监控指标创建成功");
    }

    @Test
    void testCreateAvgTimeMetrics() throws Exception {
        System.out.println("\n--- 测试：创建平均耗时监控指标 ---");

        String key = "/rpc/metrics/UserService:1.0/localhost:8083/avg_time";
        String value = "150";

        KeyValueDTO dto = new KeyValueDTO();
        dto.setKey(key);
        dto.setValue(value);

        mockMvc.perform(post("/api/etcd/key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn();

        System.out.println("✓ 平均耗时监控指标创建成功");
    }

    // ========== 查询操作测试 ==========

    @Test
    void testQueryByPrefix() throws Exception {
        System.out.println("\n--- 测试：按前缀查询键值对 ---");

        // 先创建一些测试数据
        String[] testKeys = {
            "/rpc/test/query/service1",
            "/rpc/test/query/service2",
            "/rpc/other/query/config1"
        };

        for (String key : testKeys) {
            KeyValueDTO dto = new KeyValueDTO();
            dto.setKey(key);
            dto.setValue("test_value_" + System.currentTimeMillis());

            mockMvc.perform(post("/api/etcd/key")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andReturn();

            Thread.sleep(200);
        }

        // 按/rpc/test前缀查询
        MvcResult result = mockMvc.perform(get("/api/etcd/keys")
                .param("prefix", "/rpc/test/query"))
                .andExpect(status().isOk())
                .andReturn();

        KeyValueDTO[] results = objectMapper.readValue(
            result.getResponse().getContentAsString(),
            KeyValueDTO[].class
        );

        System.out.println("✓ 按'/rpc/test/query'前缀查询成功，找到 " + results.length + " 个键值对");
        assert results.length >= 2 : "应该找到至少2个键值对";
    }

    // ========== 策略专用接口测试 ==========

    @Test
    void testPublishNodeStrategy() throws Exception {
        System.out.println("\n--- 测试：发布节点策略配置 ---");

        MvcResult result = mockMvc.perform(post("/api/etcd/strategy/UserService/1.0/localhost/8084")
                .param("strategyType", "loadbalance")
                .param("value", "random"))
                .andExpect(status().isOk())
                .andReturn();

        System.out.println("✓ 节点策略发布成功");
    }

    @Test
    void testGetNodeStrategy() throws Exception {
        System.out.println("\n--- 测试：获取节点策略配置 ---");

        // 先发布策略
        mockMvc.perform(post("/api/etcd/strategy/UserService/1.0/localhost/8085")
                .param("strategyType", "loadbalance")
                .param("value", "consistent_hash"))
                .andReturn();

        Thread.sleep(500);

        // 获取策略
        MvcResult result = mockMvc.perform(get("/api/etcd/strategy/UserService/1.0/localhost/8085")
                .param("strategyType", "loadbalance"))
                .andExpect(status().isOk())
                .andReturn();

        String strategyValue = result.getResponse().getContentAsString();
        System.out.println("✓ 节点策略获取成功: " + strategyValue);
        assert "consistent_hash".equals(strategyValue);
    }

    // ========== 简化的综合测试 ==========

    @Test
    void testBasicServiceWorkflow() throws Exception {
        System.out.println("\n--- 测试：基础服务工作流程 ---");

        String serviceName = "WorkflowService";
        String version = "1.0";
        String host = "localhost";
        int port = 9001;

        // 1. 注册服务
        String serviceKey = String.format("/rpc/%s:%s/%s:%d", serviceName, version, host, port);
        KeyValueDTO serviceDto = new KeyValueDTO();
        serviceDto.setKey(serviceKey);
        serviceDto.setValue(String.format(
            "{\"serviceName\":\"%s\",\"serviceVersion\":\"%s\",\"serviceHost\":\"%s\",\"servicePort\":%d}",
            serviceName, version, host, port
        ));

        mockMvc.perform(post("/api/etcd/key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(serviceDto)))
                .andReturn();

        System.out.println("✓ 服务注册完成");

        // 2. 添加负载均衡策略
        String strategyKey = String.format("/rpc/strategy/%s:%s/%s:%d/loadbalance", serviceName, version, host, port);
        KeyValueDTO strategyDto = new KeyValueDTO();
        strategyDto.setKey(strategyKey);
        strategyDto.setValue("round_robin");

        mockMvc.perform(post("/api/etcd/key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(strategyDto)))
                .andReturn();

        System.out.println("✓ 策略配置完成");

        // 3. 添加监控指标
        String metricsKey = String.format("/rpc/metrics/%s:%s/%s:%d/calls", serviceName, version, host, port);
        KeyValueDTO metricsDto = new KeyValueDTO();
        metricsDto.setKey(metricsKey);
        metricsDto.setValue("100");

        mockMvc.perform(post("/api/etcd/key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(metricsDto)))
                .andReturn();

        System.out.println("✓ 监控指标添加完成");

        Thread.sleep(500);

        // 4. 验证配置存在
        MvcResult serviceResult = mockMvc.perform(get("/api/etcd/key")
                .param("key", serviceKey))
                .andExpect(status().isOk())
                .andReturn();

        assert serviceResult.getResponse().getStatus() == 200;
        System.out.println("✓ 基础服务工作流程测试完成");
    }
}