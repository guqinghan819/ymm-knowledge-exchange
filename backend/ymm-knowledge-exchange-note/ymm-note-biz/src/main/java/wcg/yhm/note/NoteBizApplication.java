package wcg.yhm.note;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-12-23
 * @Description:
 * @Version: 1.0
 */

@SpringBootApplication
@MapperScan("wcg.yhm.note.domain.mapper")
@EnableFeignClients(basePackages = "wcg.yhm")
@EnableScheduling
public class NoteBizApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(NoteBizApplication.class, args);
        String nacosAddr = context.getEnvironment().getProperty("spring.cloud.nacos.discovery.server-addr");
        System.out.println("======= 当前程序读取到的 Nacos 地址是: " + nacosAddr + " =======");
    }
}
    
