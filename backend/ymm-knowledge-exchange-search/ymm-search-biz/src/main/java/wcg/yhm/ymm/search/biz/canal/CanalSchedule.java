package wcg.yhm.ymm.search.biz.canal;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.alibaba.otter.canal.protocol.exception.CanalClientException;
import com.google.protobuf.InvalidProtocolBufferException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import wcg.yhm.ymm.search.biz.enums.DeletedEnum;
import wcg.yhm.ymm.search.biz.enums.NoteStatusEnum;
import wcg.yhm.ymm.search.biz.enums.NoteVisibleEnum;
import wcg.yhm.ymm.search.biz.enums.StatusEnum;
import wcg.yhm.ymm.search.biz.index.NoteIndex;
import wcg.yhm.ymm.search.biz.index.UserIndex;
import wcg.yhm.ymm.search.biz.mapper.SelectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-04-02
 * @Description: 消费 Canal 的数据库变更并同步到 Elasticsearch
 * @Version: 1.0
 */
@Component
@Slf4j
public class CanalSchedule implements Runnable {
    @Resource
    private CanalProperties canalProperties;
    @Resource
    private CanalConnector canalConnector;
    @Resource
    private RestHighLevelClient restHighLevelClient;
    @Resource
    private SelectMapper selectMapper;
    @Override
    @Scheduled(fixedDelay = 100) //每个100ms执行一次
    public void run() {
        // 拉取后先处理完整批次，成功才 ACK；异常回滚后 Canal 会重新投递。
        long batchId = -1; //初始化批次id，-1表示未开始或没获取到数据
        try {
            //从canalConnector获取批量消息，返回的数据量由batchSize控制，不足则拉取已有的
            Message message = canalConnector.getWithoutAck(canalProperties.getBatchSize());
            //获取当前拉取到的批次Id
            batchId  = message.getId();
            //获取当前批次中的数据条数
            long size = message.getEntries().size();
            if (batchId == -1 || size == 0) {
                try {
                    //休眠一秒，防止频繁拉取
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {}
            } else {
                //当前批次中有数据就es增量构建索引
                processEntry(message.getEntries());
            }
            //进行ack确认，表示该批次数据已经被消费
            canalConnector.ack(batchId);
        } catch (Exception e) {
            log.error("## 消费Canal 批次数据异常", e);
            //出现异常，对数据进行回滚,以便重新消费这批次的数据
            canalConnector.rollback();
        }
    }


    /**
     * 处理这一批次的数据
     * @param entrys
     */
    private void processEntry(List<CanalEntry.Entry> entrys) throws Exception {
        for (CanalEntry.Entry entry : entrys) {
            //只处理ROWDATA行数据类型的entry， 忽略事务类型等其他类型
            if (entry.getEntryType() == CanalEntry.EntryType.ROWDATA) {
                //获取事件类型(INSERT,UPDATE,DELETE)
                CanalEntry.EventType eventType = entry.getHeader().getEventType();
                //获取数据库名
                String database = entry.getHeader().getSchemaName();
                //获取表名
                String table = entry.getHeader().getTableName();
                // 解析出 RowChange 对象，包含 RowData 和事件相关信息
                CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
                //遍历
                for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
                    // DELETE 事件没有 afterColumns，主键等被删除行数据位于 beforeColumns 中
                    List<CanalEntry.Column> columns = eventType == CanalEntry.EventType.DELETE
                            ? rowData.getBeforeColumnsList()
                            : rowData.getAfterColumnsList();
                    Map<String, Object> map = parseColumns2Map(columns);
                    log.info("EventType: {}, Database: {}, Table: {}, Columns: {}", eventType, database, table, map);
                    //处理事件
                    processEvent(eventType, table, map);
                }
            }
        }
    }

    /**
     * 处理事件
     * @param eventType 事件类型(INSERT,UPDATE,DELETE) 这里是逻辑删除算修改
     * @param table 表名
     * @param map 列数据map
     */
    private void processEvent(CanalEntry.EventType eventType, String table, Map<String, Object> map) throws IOException {
        switch (table) {
            case "note" -> processNoteEvent(eventType, map);
            case "user" -> processUserEvent(eventType, map);
            default -> log.error("not support table: {}", table);
        }
    }

    /**
     * 处理用户表
     * @param eventType 事件类型(INSERT,UPDATE,DELETE) 这里是逻辑删除算修改
     * @param map 列数据
     */
    private void processUserEvent(CanalEntry.EventType eventType, Map<String, Object> map) throws IOException {
        Long userId = parseId(map, eventType, "user");
        if (userId == null) {
            return;
        }
        switch (eventType) {
            case INSERT -> syncUserIndex(userId);
            case UPDATE -> {
                Integer status = Integer.valueOf(map.get("status").toString());
                Boolean isDeleted = Boolean.valueOf(map.get("is_delete").toString());
                if (Objects.equals(status, StatusEnum.ENABLE.getValue()) &&
                Objects.equals(isDeleted, DeletedEnum.NO.getValue())) {
                    //更新用户索引笔记索引
                    syncNoteIndexAndUserIndex(userId);
                } else if (Objects.equals(status, StatusEnum.DISABLED.getValue()) // 用户状态为禁用
                        || Objects.equals(isDeleted, DeletedEnum.YES.getValue())) {
                    //删除文档
                    deleteUserDocument(userId);
                }
            }
            case DELETE -> deleteUserDocument(userId);
            default -> log.warn("not support eventType: {}", eventType);
        }
    }

    /**
     * 同步es
     * @param userId 用户id
     */
    private void syncNoteIndexAndUserIndex(Long userId) throws IOException {
        BulkRequest bulkRequest = new BulkRequest(); //批量操作es
        List<Map<String, Object>> maps = selectMapper.selectEsUserIndexData(userId);
        //遍历
        for (Map<String, Object> map : maps) {
            IndexRequest indexRequest = new IndexRequest(UserIndex.NAME);
            indexRequest.id(String.valueOf(map.get(UserIndex.FIELD_USER_ID)));
            indexRequest.source(map);
            bulkRequest.add(indexRequest);
        }
        List<Map<String, Object>> noteResult = selectMapper.selectEsNoteIndexData(null, userId);
        for (Map<String, Object> recordMap : noteResult) {
            // 创建索引请求对象，指定索引名称
            IndexRequest indexRequest = new IndexRequest(NoteIndex.NAME);
            // 设置文档的 ID，使用记录中的主键 “id” 字段值
            indexRequest.id((String.valueOf(recordMap.get(NoteIndex.FIELD_NOTE_ID))));
            // 设置文档的内容，使用查询结果的记录数据
            indexRequest.source(recordMap);
            // 将每个 IndexRequest 加入到 BulkRequest
            bulkRequest.add(indexRequest);
        }
        restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
    }

    /**
     * 删除用户文档
     * @param userId
     */
    private void deleteUserDocument(Long userId) throws IOException {
        String documentId = String.valueOf(userId);
        DeleteRequest deleteRequest = new DeleteRequest(UserIndex.NAME, documentId);
        restHighLevelClient.delete(deleteRequest, RequestOptions.DEFAULT);
    }

    /**
     * 同步用户数据到es
     * @param userId 用户id
     */
    private void syncUserIndex(Long userId) throws IOException {
        List<Map<String, Object>> maps = selectMapper.selectEsUserIndexData(userId);
        for (Map<String, Object> map : maps) {
            IndexRequest indexRequest = new IndexRequest(UserIndex.NAME);
            indexRequest.id(String.valueOf(map.get(UserIndex.FIELD_USER_ID)));
            indexRequest.source(map);
            restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
        }
    }

    /**
     * 处理笔记表
     * @param eventType 事件类型(INSERT,UPDATE,DELETE) 这里是逻辑删除算修改
     * @param map 列数据
     */
    private void processNoteEvent(CanalEntry.EventType eventType, Map<String, Object> map) throws IOException {
        Long noteId = parseId(map, eventType, "note");
        if (noteId == null) {
            return;
        }
        switch (eventType) {
            case INSERT -> syncNoteIndex(noteId); //同步数据
            case UPDATE -> {
                //记录更新
                Integer status = Integer.parseInt(map.get("status").toString());
                Integer visible = Integer.parseInt(map.get("visible").toString());
                if (Objects.equals(status, NoteStatusEnum.NORMAL.getValue())
                && Objects.equals(visible, NoteVisibleEnum.PUBLIC.getValue())) {
                    //对索引进行覆盖更新
                    syncNoteIndex(noteId);
                } else if (Objects.equals(status, NoteStatusEnum.DOWNED.getValue()) ||
                Objects.equals(status, NoteStatusEnum.DELETED.getValue()) ||
                Objects.equals(visible, NoteVisibleEnum.PRIVATE.getValue())) {
                    //进行删除文档
                    deleteNoteDocument(noteId);
                }
            }
            case DELETE -> deleteNoteDocument(noteId);
            default -> log.warn("not support eventType: {}", eventType);
        }
    }

    /**
     * 解析行主键。异常行只跳过当前事件，避免同一 Canal 批次反复回滚并阻塞后续数据。
     */
    private Long parseId(Map<String, Object> map, CanalEntry.EventType eventType, String table) {
        Object id = map.get("id");
        if (id == null) {
            log.error("Canal事件缺少主键，跳过本行: eventType={}, table={}, columns={}", eventType, table, map);
            return null;
        }
        try {
            return Long.valueOf(id.toString());
        } catch (NumberFormatException e) {
            log.error("Canal事件主键格式非法，跳过本行: eventType={}, table={}, id={}", eventType, table, id, e);
            return null;
        }
    }

    /**
     * 删除文档
     * @param noteId 笔记id
     */
    private void deleteNoteDocument(Long noteId) throws IOException {
        String documentId = String.valueOf(noteId);
        DeleteRequest deleteRequest = new DeleteRequest(NoteIndex.NAME, documentId);
        restHighLevelClient.delete(deleteRequest, RequestOptions.DEFAULT);
    }

    /**
     *将列数据转换成map
     */
    private Map<String, Object> parseColumns2Map(List<CanalEntry.Column>  columns) {
            Map<String, Object> map = new HashMap<>();
            columns.forEach(column -> {
                if (Objects.isNull(column)) return;
                map.put(column.getName(), column.getValue());
            });
            return map;
    }

    /**
     * 处理insert事件
     * @param noteId 笔记id
     */
    private void syncNoteIndex(Long noteId) throws IOException {
        //从db查询笔记索引的信息
        List<Map<String, Object>> maps = selectMapper.selectEsNoteIndexData(noteId, null);
        for (Map<String, Object> recordMap : maps) {
            //创建索引请求对象，指定索引名称
            IndexRequest indexRequest = new IndexRequest(NoteIndex.NAME);
            // 设置文档的id
            indexRequest.id(String.valueOf(recordMap.get(NoteIndex.FIELD_NOTE_ID)));
            //设置文档内容
            indexRequest.source(recordMap);
            //将数据写入es索引
            restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
        }
    }

}
    
