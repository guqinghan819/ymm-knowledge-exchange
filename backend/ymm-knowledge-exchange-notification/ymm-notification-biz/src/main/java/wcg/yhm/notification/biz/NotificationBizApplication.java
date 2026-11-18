package wcg.yhm.notification.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@MapperScan("wcg.yhm.notification.biz.mapper")
@EnableFeignClients("wcg.yhm")
public class NotificationBizApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationBizApplication.class, args);
    }
}
