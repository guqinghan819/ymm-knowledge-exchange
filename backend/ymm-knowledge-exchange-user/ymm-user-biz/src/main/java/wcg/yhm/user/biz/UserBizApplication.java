package wcg.yhm.user.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-27
 * @Description:
 * @Version: 1.0
 */

@SpringBootApplication
@MapperScan("wcg.yhm.user.biz.mapper")
@EnableFeignClients(basePackages = "wcg.yhm")
public class UserBizApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserBizApplication.class, args);
    }
}
    
