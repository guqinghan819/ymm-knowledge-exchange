package wcg.yhm.ymm.count;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-02-03
 * @Description: 计数服务启动
 * @Version: 1.0
 */

@SpringBootApplication
@MapperScan("wcg.yhm.ymm.count.mapper")
@EnableFeignClients(basePackages = "wcg.yhm")
public class CountBizApplication {
    public static void main(String[] args) {
        SpringApplication.run(CountBizApplication.class, args);
    }
}
    
