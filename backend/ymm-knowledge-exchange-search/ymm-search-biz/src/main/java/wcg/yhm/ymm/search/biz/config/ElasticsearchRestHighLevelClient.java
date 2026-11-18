package wcg.yhm.ymm.search.biz.config;

import jakarta.annotation.Resource;
import org.apache.http.HttpHost;
import org.checkerframework.checker.units.qual.C;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-30
 * @Description: ElasticsearchRestHighLevelClient 客户端
 * @Version: 1.0
 */

@Configuration
public class ElasticsearchRestHighLevelClient {

    @Resource
    private ElasticsearchProperties elasticsearchProperties;

    private static final String COLON = ":";
    private static final String HTTP = "http";

    @Bean
    public RestHighLevelClient restHighLevelClient() {
        String address = elasticsearchProperties.getAddress();
        String[] addressArr = address.split(COLON);
        String host = addressArr[0]; //ip
        int port = Integer.parseInt(addressArr[1]);
        HttpHost httpHost = new HttpHost(host, port, HTTP);
        return new RestHighLevelClient(RestClient.builder(httpHost));
    }
}
    
