package wcg.yhm.ymm.search.biz.canal;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-04-02
 * @Description:
 * @Version: 1.0
 */

@Component
@Slf4j
public class CanalClient implements DisposableBean {

    @Resource
    private CanalProperties canalProperties;

    private CanalConnector canalConnector;

    @Bean
    public CanalConnector canalConnector() {
        String address = canalProperties.getAddress();
        String[] addressArr = address.split(":");
        String host = addressArr[0]; //ip
        int port = Integer.parseInt(addressArr[1]); //端口
        canalConnector = CanalConnectors.newSingleConnector(
                new InetSocketAddress(host, port),
                canalProperties.getDestination(),
                canalProperties.getUsername(),
                canalProperties.getPassword());
        //连接到Canal 服务端
        canalConnector.connect();
        //订阅canal中的数据变化，指定要监听的数据库和表
        canalConnector.subscribe(canalProperties.getSubscribe());
        canalConnector.rollback(); //回滚canal消费者的位点，回滚到上次提交的消费位置
        return canalConnector;
    }



    @Override
    public void destroy() throws Exception {
        if (Objects.nonNull(canalConnector)) {
            //断开与canal服务链接
            canalConnector.disconnect();
        }
    }
}
    
