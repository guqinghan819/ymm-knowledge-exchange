package wcg.yhm.ai.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-25
 * @Description:
 * @Version: 1.0
 */

@SpringBootApplication
@MapperScan("wcg.yhm.ai.biz.mapper")
@EnableFeignClients(basePackages = "wcg.yhm")
public class YmmAiApplication {
    public static void main(String[] args) {
        SpringApplication.run(YmmAiApplication.class, args);
    }
}
    
