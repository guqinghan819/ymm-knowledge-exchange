package wcg.yhm.oss.biz.factory;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import wcg.yhm.oss.biz.strategy.FileStrategy;

import java.util.Locale;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-26
 * @Description: 文件存储策略工厂
 * @Version: 1.0
 */

@Component
@Slf4j
@RefreshScope // 表明该类被代理
public class FileStrategyFactory {
    @Value("${strategy.type}")
    private String strategyType;

    /**
     * 通过容器取已注入完成的策略 Bean，避免手动 new 导致 COSClient、配置项等依赖为空。
     */
    @Resource
    private ApplicationContext applicationContext;


    @Bean
    @RefreshScope // 表明该方法具备刷新能力
    public FileStrategy fileStrategy() {
        if (strategyType == null || strategyType.trim().isEmpty()) {
            throw new IllegalArgumentException("没有配置存储策略类型");
        }
        String strategyBeanName = strategyType.trim().toUpperCase(Locale.ROOT);
        if (!applicationContext.containsBean(strategyBeanName)) {
            throw new IllegalArgumentException("没有该存储策略类型: " + strategyType);
        }
        return applicationContext.getBean(strategyBeanName, FileStrategy.class);
    }
}
    
