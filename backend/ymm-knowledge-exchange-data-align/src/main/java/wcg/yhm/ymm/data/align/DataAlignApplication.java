package wcg.yhm.ymm.data.align;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-16
 * @Description:
 * @Version: 1.0
 */
@SpringBootApplication
@MapperScan(basePackages = "wcg.yhm.ymm.data.align.mapper")
@EnableFeignClients(basePackages = "wcg.yhm")
public class DataAlignApplication {
    public static void main(String[] args) {
        SpringApplication.run(DataAlignApplication.class, args);
    }
}
    
