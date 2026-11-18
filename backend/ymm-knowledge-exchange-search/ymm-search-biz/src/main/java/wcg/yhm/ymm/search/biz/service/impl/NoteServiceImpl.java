package wcg.yhm.ymm.search.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.search.TotalHits;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.common.lucene.search.function.FieldValueFactorFunction;
import org.elasticsearch.common.lucene.search.function.FunctionScoreQuery;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FieldValueFactorFunctionBuilder;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Service;

import wcg.yhm.ymm.search.api.domain.request.RebuildNoteDocumentRequest;
import wcg.yhm.ymm.search.biz.domain.request.SearchNoteRequest;
import wcg.yhm.ymm.search.biz.domain.response.SearchNoteResponse;
import wcg.yhm.ymm.search.biz.enums.NotePublishTimeRangeEnum;
import wcg.yhm.ymm.search.biz.enums.NoteSortEnum;
import wcg.yhm.ymm.search.biz.index.NoteIndex;
import wcg.yhm.ymm.search.biz.mapper.SelectMapper;
import wcg.yhm.ymm.search.biz.service.NoteService;
import wcg.yhm.ymmknowledgeexchange.common.constant.DateConstant;
import wcg.yhm.ymmknowledgeexchange.common.exception.BizException;
import wcg.yhm.ymmknowledgeexchange.common.response.PageResponse;
import wcg.yhm.ymmknowledgeexchange.common.utils.DateUtil;
import wcg.yhm.ymmknowledgeexchange.common.utils.NumberUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-03-30
 * @Description:
 * @Version: 1.0
 */
@Service
@Slf4j
public class NoteServiceImpl implements NoteService {
    @Resource
    private RestHighLevelClient restHighLevelClient;

    @Resource
    private SelectMapper selectMapper;

    @Override
    public PageResponse<SearchNoteResponse> searchNote(SearchNoteRequest searchNoteRequest) {
        String keyword = searchNoteRequest.getKeyword();
        int pageNo = searchNoteRequest.getPageNo();
        Integer type = searchNoteRequest.getType(); //笔记类型
        Integer sort = searchNoteRequest.getSort(); //排序字段
        Integer publishTimeRange = searchNoteRequest.getPublishTimeRange(); //发布时间范围
        SearchRequest searchRequest = new SearchRequest(NoteIndex.NAME);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        //创建查询条件
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery().must(
                QueryBuilders.multiMatchQuery(keyword)
                        .field(NoteIndex.FIELD_NOTE_TITLE, 2.0f) //设置笔记标题的权重 标题匹配大于话题
                        .field(NoteIndex.FIELD_NOTE_TOPIC) //不设置默认为1.0.
        );

        //勾选了笔记类型，添加过滤条件
        if (Objects.nonNull(type)) {
            boolQueryBuilder.filter(QueryBuilders.termQuery(NoteIndex.FIELD_NOTE_TYPE, type));
        }
        //发布时间范围过滤
        NotePublishTimeRangeEnum notePublishTimeRangeEnum = NotePublishTimeRangeEnum.valueOf(publishTimeRange);
        if (Objects.nonNull(notePublishTimeRangeEnum)) {
            //结束时间
            String endTime = DateUtil.localDateTime2String(LocalDateTime.now());
            //开始时间
            String startTime = null;
            switch (notePublishTimeRangeEnum) {
                case DAY -> startTime = DateUtil.localDateTime2String(LocalDateTime.now().minusDays(1)); //一天前的时间
                case WEEK -> startTime = DateUtil.localDateTime2String(LocalDateTime.now().minusWeeks(1)); //一周前的时间
                case HALF_YEAR -> startTime = DateUtil.localDateTime2String(LocalDateTime.now().minusMonths(6)); //半年前的时间
            }
            //设置时间范围
            if (StrUtil.isNotBlank(startTime)) {
                boolQueryBuilder
                        .filter(QueryBuilders.rangeQuery(NoteIndex.FIELD_NOTE_CREATE_TIME)
                                .gte(startTime) //大于等于
                                .lte(endTime)); //小于等于
            }
        }
        //添加排序条件
        NoteSortEnum noteSortEnum = NoteSortEnum.valueOf(sort);
        if (Objects.nonNull(noteSortEnum)) {
            switch (noteSortEnum) {
                case LATEST ->
                        searchSourceBuilder.sort(new FieldSortBuilder(NoteIndex.FIELD_NOTE_CREATE_TIME).order(SortOrder.DESC)); //发布时间降序
                case MOST_LIKE -> searchSourceBuilder.sort(new FieldSortBuilder(NoteIndex.FIELD_NOTE_LIKE_TOTAL).order(SortOrder.DESC)); //点赞数量降序
                case MOST_COMMENT -> searchSourceBuilder.sort(new FieldSortBuilder(NoteIndex.FIELD_NOTE_COMMENT_TOTAL).order(SortOrder.DESC)); //评论数量
                case MOST_COLLECT -> searchSourceBuilder.sort(new FieldSortBuilder(NoteIndex.FIELD_NOTE_COLLECT_TOTAL).order(SortOrder.DESC)); //收藏
            }
            searchSourceBuilder.query(boolQueryBuilder);
        } else {
            //枚举为空就综合排序
            searchSourceBuilder.sort(new FieldSortBuilder("_score").order(SortOrder.DESC)); // 按照 _score 降序
            FunctionScoreQueryBuilder.FilterFunctionBuilder[] filterFunctionBuilders = new FunctionScoreQueryBuilder.FilterFunctionBuilder[] {
                    // function 1 点赞评分
                    new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                            new FieldValueFactorFunctionBuilder(NoteIndex.FIELD_NOTE_LIKE_TOTAL)
                                    .factor(0.5f) // * 0.5
                                    .modifier(FieldValueFactorFunction.Modifier.SQRT) //结果开平方避免差距太大
                                    .missing(0)
                    ),
                    // function 2 收藏评分
                    new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                            new FieldValueFactorFunctionBuilder(NoteIndex.FIELD_NOTE_COLLECT_TOTAL)
                                    .factor(0.3f)
                                    .modifier(FieldValueFactorFunction.Modifier.SQRT)
                                    .missing(0)
                    ),
                    // function 3 评论评分
                    new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                            new FieldValueFactorFunctionBuilder(NoteIndex.FIELD_NOTE_COMMENT_TOTAL)
                                    .factor(0.2f)
                                    .modifier(FieldValueFactorFunction.Modifier.SQRT)
                                    .missing(0)
                    )
            };
            FunctionScoreQueryBuilder functionScoreQueryBuilder = QueryBuilders.functionScoreQuery(boolQueryBuilder, filterFunctionBuilders)
                    .scoreMode(FunctionScoreQuery.ScoreMode.SUM)
                    .boostMode(CombineFunction.SUM);
            searchSourceBuilder.query(functionScoreQueryBuilder);
        }
        // 设置分页，from 和 size
        int pageSize = 10; // 每页展示数据量
        int from = (pageNo - 1) * pageSize; // 偏移量
        searchSourceBuilder.from(from);
        searchSourceBuilder.size(pageSize);

        // 设置高亮字段
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field(NoteIndex.FIELD_NOTE_TITLE)
                .preTags("<strong>") // 设置包裹标签
                .postTags("</strong>");
        searchSourceBuilder.highlighter(highlightBuilder);

        // 将构建的查询条件设置到 SearchRequest 中
        searchRequest.source(searchSourceBuilder);

        long total = 0; //命中文档条数
        List<SearchNoteResponse> searchNoteResponses = null;

        try {
            log.info("===========> request: {}", searchNoteRequest);
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            searchNoteResponses = new ArrayList<>();
            //文档数
            total = searchResponse.getHits().getTotalHits().value;
            SearchHits hits = searchResponse.getHits();
            for (SearchHit hit : hits) {
                log.info("==========> 命中文档内容: {}", hit.getSourceAsString());
                Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                // 提取特定字段值
                Long noteId = (Long) sourceAsMap.get(NoteIndex.FIELD_NOTE_ID);
                String cover = (String) sourceAsMap.get(NoteIndex.FIELD_NOTE_COVER);
                String title = (String) sourceAsMap.get(NoteIndex.FIELD_NOTE_TITLE);
                String avatar = (String) sourceAsMap.get(NoteIndex.FIELD_NOTE_AVATAR);
                String nickname = (String) sourceAsMap.get(NoteIndex.FIELD_NOTE_NICKNAME);
                // 获取更新时间
                String updateTimeStr = (String) sourceAsMap.get(NoteIndex.FIELD_NOTE_UPDATE_TIME);
                LocalDateTime updateTime = LocalDateTime.parse(updateTimeStr, DateConstant.DATE_FORMAT_Y_M_D_H_M_S);
                Integer likeTotal = (Integer) sourceAsMap.get(NoteIndex.FIELD_NOTE_LIKE_TOTAL);
                Integer commentTotal = (Integer) sourceAsMap.get(NoteIndex.FIELD_NOTE_COMMENT_TOTAL);
                Integer collectTotal = (Integer) sourceAsMap.get(NoteIndex.FIELD_NOTE_COLLECT_TOTAL);
                // 获取高亮字段
                String highlightedTitle = null;
                if (CollUtil.isNotEmpty(hit.getHighlightFields())
                        && hit.getHighlightFields().containsKey(NoteIndex.FIELD_NOTE_TITLE)) {
                    highlightedTitle = hit.getHighlightFields().get(NoteIndex.FIELD_NOTE_TITLE).fragments()[0].string();
                }

                // 构建 VO 实体类
                SearchNoteResponse searchNoteResponse = SearchNoteResponse.builder()
                        .noteId(noteId)
                        .cover(cover)
                        .title(title)
                        .highlightTitle(highlightedTitle)
                        .avatar(avatar)
                        .nickname(nickname)
                        .updateTime(DateUtil.formatRelativeTime(updateTime))
                        .likeTotal(NumberUtils.formatNumberString(likeTotal))
                        .commentTotal(NumberUtils.formatNumberString(commentTotal))
                        .collectTotal(NumberUtils.formatNumberString(collectTotal))
                        .build();
                searchNoteResponses.add(searchNoteResponse);
            }
        } catch (IOException e) {
            log.error("## es 笔记查询异常: ", e);
        }
        return PageResponse.success(searchNoteResponses, pageNo, total);
    }

    @Override
    public void rebuildDocument(RebuildNoteDocumentRequest request) {
        //db查询数据
        Long noteId = request.getNoteId();
        List<Map<String, Object>> maps = selectMapper.selectEsNoteIndexData(noteId, null);
        for (Map<String, Object> map : maps) {
            IndexRequest indexRequest = new IndexRequest(NoteIndex.NAME);
            indexRequest.id(String.valueOf(map.get(NoteIndex.FIELD_NOTE_ID)));
            indexRequest.source(map);
            try {
                restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
            } catch (IOException e) {
                log.error("## 重建笔记文档失败: ", e);
            }
        }

    }
}
    
