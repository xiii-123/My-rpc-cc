package com.yupi.yurpc.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * yu-rpc Admin 启动类
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 */
@SpringBootApplication
public class YurpcAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(YurpcAdminApplication.class, args);
        System.out.println("yurpc-admin started successfully!");
        System.out.println("ETCD Management API: http://localhost:8081/api/etcd");
    }
}