package wcg.yhm.ymm.search.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.User;
import org.apache.lucene.search.TotalHits;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Service;

import wcg.yhm.ymm.search.api.domain.request.RebuildUserDocumentRequest;
import wcg.yhm.ymm.search.biz.domain.request.SearchUserRequest;
import wcg.yhm.ymm.search.biz.domain.response.SearchUserResponse;
import wcg.yhm.ymm.search.biz.index.UserIndex;
import wcg.yhm.ymm.search.biz.mapper.SelectMapper;
import wcg.yhm.ymm.search.biz.service.UserService;
import wcg.yhm.ymmknowledgeexchange.common.response.PageResponse;
import wcg.yhm.ymmknowledgeexchange.common.utils.NumberUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-30
 * @Description:
 * @Version: 1.0
 */

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Resource
    private RestHighLevelClient restHighLevelClient;
    @Resource
    private SelectMapper selectMapper;

    @Override
    public PageResponse<SearchUserResponse> searchUser(SearchUserRequest searchUserRequest) {
        String keyword = searchUserRequest.getKeyword();
        int pageNo = searchUserRequest.getPageNo();
        //创建查询请求，指定索引
        SearchRequest searchRequest = new SearchRequest(UserIndex.NAME);
        //构建查询内容
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //构建多重匹配查询，查询nickname和ymmId
        searchSourceBuilder.query(QueryBuilders.multiMatchQuery(
                keyword, UserIndex.FIELD_USER_NICKNAME, UserIndex.FIELD_USER_YMM_ID
        ));
        //排序，按照fansTotal降序排序
        FieldSortBuilder sortBuilder = new FieldSortBuilder(UserIndex.FIELD_USER_FANS_TOTAL)
                .order(SortOrder.DESC);
        searchSourceBuilder.sort(sortBuilder);

        //设置分页
        int pageSize = 10;
        int from = (pageNo - 1) * pageSize; //偏移量
        searchSourceBuilder.from(from);
        searchSourceBuilder.size(pageSize);
        //设置关键字匹配高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field(UserIndex.FIELD_USER_NICKNAME)
                .preTags("<strong>")
                .postTags("</strong>");
        searchSourceBuilder.highlighter(highlightBuilder);
        //将查询条件放到请求中
        searchRequest.source(searchSourceBuilder);
        //响应列表
        List<SearchUserResponse> searchUserResponses = null;
        //命中文档总数 默认为0
        long total = 0;
        try {
            log.info("===========> request: {}", searchRequest);
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            total = searchResponse.getHits().getTotalHits().value;
            log.info("===========> 命中文档数量: {}", total);
            searchUserResponses = new ArrayList<>();
            //获取响应中命中的文档列表
            SearchHits hits = searchResponse.getHits();
            for (SearchHit hit :  hits) {
                log.info("==========> 命中文档内容: {}", hit.getSourceAsString());
                //将文档字段转成map
                Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                //提取字段
                //转换成Number兼容所有数字类型 -> Long 类型
                Long userId = ((Number) sourceAsMap.get(UserIndex.FIELD_USER_ID)).longValue();
                String ymmId = (String) sourceAsMap.get(UserIndex.FIELD_USER_YMM_ID);
                String nickname = (String) sourceAsMap.get(UserIndex.FIELD_USER_NICKNAME);
                String avatar = (String) sourceAsMap.get(UserIndex.FIELD_USER_AVATAR);
                Integer fansTotal = (Integer) sourceAsMap.get(UserIndex.FIELD_USER_FANS_TOTAL);
                Integer noteTotal = (Integer) sourceAsMap.get(UserIndex.FIELD_USER_NOTE_TOTAL);
                //获取高亮字段
                String highlightNickname = null;
                if (CollUtil.isNotEmpty(hit.getHighlightFields()) &&
                    hit.getHighlightFields().containsKey(UserIndex.FIELD_USER_NICKNAME)) {
                    highlightNickname = hit.getHighlightFields().get(UserIndex.FIELD_USER_NICKNAME).fragments()[0].string();
                }
                //构建实体类
                SearchUserResponse searchUserResponse = SearchUserResponse.builder()
                        .userId(userId)
                        .avatar(avatar)
                        .nickname(nickname)
                        .noteTotal(noteTotal.longValue())
                        .fansTotal(NumberUtils.formatNumberString(fansTotal.longValue()))
                        .highlightNickname(highlightNickname)
                        .ymm_id(ymmId).build();
                searchUserResponses.add(searchUserResponse);
            }
        } catch (IOException e) {
            log.error("## es 查询用户异常: ", e);
        }
        return PageResponse.success(searchUserResponses, pageNo, total);
    }

    @Override
    public void rebuildDocument(RebuildUserDocumentRequest request) {
        Long userId = request.getUserId();
        List<Map<String, Object>> maps = selectMapper.selectEsUserIndexData(userId);
        for (Map<String, Object> map : maps) {
            IndexRequest indexRequest = new IndexRequest(UserIndex.NAME);
            indexRequest.id(String.valueOf(map.get(UserIndex.FIELD_USER_ID)));
            indexRequest.source(map);
            try {
                restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
            } catch (IOException e) {
                log.error("## 重建用户文档失败, e:", e);
            }
        }
    }
}
    
