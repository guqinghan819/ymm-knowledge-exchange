package wcg.yhm.distributed.id.service;

import com.alibaba.druid.pool.DruidDataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import wcg.yhm.distributed.id.constant.Constants;
import wcg.yhm.distributed.id.core.IDGen;
import wcg.yhm.distributed.id.core.common.PropertyFactory;
import wcg.yhm.distributed.id.core.common.Result;
import wcg.yhm.distributed.id.core.common.ZeroIDGen;
import wcg.yhm.distributed.id.core.segment.SegmentIDGenImpl;
import wcg.yhm.distributed.id.core.segment.dao.IDAllocDao;
import wcg.yhm.distributed.id.core.segment.dao.impl.IDAllocDaoImpl;
import wcg.yhm.distributed.id.exception.InitException;

import java.sql.SQLException;
import java.util.Properties;

/** 从数据库号段批量分配 ID；未启用号段模式时使用占位生成器。 */
@Service("SegmentService")
public class SegmentService {
    private Logger logger = LoggerFactory.getLogger(SegmentService.class);

    private IDGen idGen;
    private DruidDataSource dataSource;

    public SegmentService() throws SQLException, InitException {
        Properties properties = PropertyFactory.getProperties();
        boolean flag = Boolean.parseBoolean(properties.getProperty(Constants.LEAF_SEGMENT_ENABLE, "true"));
        if (flag) {
            // 号段生成器通过独立数据源和分配表申请 ID 区间。
            dataSource = new DruidDataSource();
            dataSource.setUrl(properties.getProperty(Constants.LEAF_JDBC_URL));
            dataSource.setUsername(properties.getProperty(Constants.LEAF_JDBC_USERNAME));
            dataSource.setPassword(properties.getProperty(Constants.LEAF_JDBC_PASSWORD));
            dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
            dataSource.setValidationQuery("select 1");
            dataSource.init();

            // 将数据库分配器交给号段生成器初始化。
            IDAllocDao dao = new IDAllocDaoImpl(dataSource);

            idGen = new SegmentIDGenImpl();
            ((SegmentIDGenImpl) idGen).setDao(dao);
            if (idGen.init()) {
                logger.info("Segment Service Init Successfully");
            } else {
                throw new InitException("Segment Service Init Fail");
            }
        } else {
            idGen = new ZeroIDGen();
            logger.info("Zero ID Gen Service Init Successfully");
        }
    }

    public Result getId(String key) {
        return idGen.get(key);
    }

    public SegmentIDGenImpl getIdGen() {
        if (idGen instanceof SegmentIDGenImpl) {
            return (SegmentIDGenImpl) idGen;
        }
        return null;
    }
}
