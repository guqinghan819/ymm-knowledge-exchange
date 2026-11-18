package wcg.yhm.comment.biz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.retry.annotation.EnableRetry;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-04-02
 * @Description:
 * @Version: 1.0
 */

@SpringBootApplication
@EnableRetry
@EnableFeignClients(basePackages = "wcg.yhm")
public class YmmCommentApplication {
    public static void main(String[] args) {
        SpringApplication.run(YmmCommentApplication.class, args);
    }
}
    
