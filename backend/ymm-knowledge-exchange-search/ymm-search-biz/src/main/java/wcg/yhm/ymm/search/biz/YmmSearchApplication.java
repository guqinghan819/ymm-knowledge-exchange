package wcg.yhm.ymm.search.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-30
 * @Description:
 * @Version: 1.0
 */

@SpringBootApplication
@EnableScheduling //开启调度
@MapperScan(basePackages = "wcg.yhm.ymm.search.biz.mapper")
@EnableFeignClients(basePackages = "wcg.yhm")
public class YmmSearchApplication {
    public static void main(String[] args) {
        SpringApplication.run(YmmSearchApplication.class, args);
    }
}
    
