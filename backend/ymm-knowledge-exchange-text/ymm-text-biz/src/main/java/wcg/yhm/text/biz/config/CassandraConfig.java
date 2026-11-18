package wcg.yhm.text.biz.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-12-02
 * @Description: Cassandra 配置类
 * @Version: 1.0
 */

@Configuration
public class CassandraConfig extends AbstractCassandraConfiguration {
    @Value("${spring.cassandra.contact-points}")
    private String contactPoints;
    @Value("${spring.cassandra.port}")
    private int port;
    @Value("${spring.cassandra.keyspace-name}")
    private String keySpaceName;


    @Override
    public String getContactPoints() {
        return contactPoints;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public String getKeyspaceName() {
        return keySpaceName;
    }
}
    
