package wcg.yhm.ymmknowledgeexchange.auth;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@MapperScan("wcg.yhm.ymmknowledgeexchange.auth.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableFeignClients(basePackages = "wcg.yhm")
public class YmmKnowledgeExchangeAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(YmmKnowledgeExchangeAuthApplication.class, args);
    }

}
