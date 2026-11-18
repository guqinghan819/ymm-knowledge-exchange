package wcg.yhm.user.relation.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-01-25
 * @Description:
 * @Version: 1.0
 */

@SpringBootApplication
@MapperScan("wcg.yhm.user.relation.biz.mapper")
@EnableFeignClients("wcg.yhm")
public class UserRelationBizApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserRelationBizApplication.class, args);
    }
}
    
