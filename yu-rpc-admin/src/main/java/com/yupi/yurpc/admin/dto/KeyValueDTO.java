package com.yupi.yurpc.admin.dto;

import io.etcd.jetcd.KeyValue;
import lombok.Data;

import java.nio.charset.StandardCharsets;

/**
 * 键值对数据传输对象
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 */
@Data
public class KeyValueDTO {

    /**
     * 键
     */
    private String key;

    /**
     * 值
     */
    private String value;

    /**
     * 创建时间
     */
    private Long createRevision;

    /**
     * 修改时间
     */
    private Long modRevision;

    /**
     * 版本号
     */
    private Long version;

    public KeyValueDTO() {}

    public KeyValueDTO(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public static KeyValueDTO fromEtcdKeyValue(KeyValue keyValue) {
        KeyValueDTO dto = new KeyValueDTO();
        dto.setKey(keyValue.getKey().toString(StandardCharsets.UTF_8));
        dto.setValue(keyValue.getValue().toString(StandardCharsets.UTF_8));
        dto.setCreateRevision(keyValue.getCreateRevision());
        dto.setModRevision(keyValue.getModRevision());
        dto.setVersion(keyValue.getVersion());
        return dto;
    }
}