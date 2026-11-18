package wcg.yhm.note.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import wcg.yhm.note.model.vo.ChannelResponse;
import wcg.yhm.note.model.vo.FindNoteDetailResponse;
import wcg.yhm.ymmknowledgeexchange.common.utils.JsonUtil;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Author: 雨好绵
 * @CreateTime: 2026-01-22
 * @Description:
 * @Version: 1.0
 */

public class CaffeineUtil {

    private static final Cache<Long, String> LOCAL_CACHE =
            Caffeine.newBuilder()
                    .initialCapacity(10000)
                    .maximumSize(10000)
                    .expireAfterWrite(1, TimeUnit.HOURS)
                    .build();

    private static final Cache<String, List<ChannelResponse>> CHANNEL_LOCAL_CACHE =
            Caffeine.newBuilder()
                    .initialCapacity(40)
                    .maximumSize(100)
                    .expireAfterWrite(1, TimeUnit.HOURS)
                    .build();
    /**
     * 本地缓存获取笔记详情
     * @param noteId
     * @return
     */
    public static FindNoteDetailResponse getNoteDetail(Long noteId) {
        String noteDetail = LOCAL_CACHE.getIfPresent(noteId);
        if (StrUtil.isBlank(noteDetail)) {
            return null;
        }
        return JsonUtil.parseObject(noteDetail, FindNoteDetailResponse.class);
    }

    /**
     * 将笔记信息保存到本地缓存
     * @param noteId
     */
    public static void saveNoteDetail(Long noteId, String noteDetail) {
        LOCAL_CACHE.put(noteId, noteDetail);
    }

    /**
     * 删除缓存
     * @param noteId 笔记id
     */
    public static void deleteNoteDetail(Long noteId) {
        LOCAL_CACHE.invalidate(noteId);
    }

    /**
     * 获取所有频道
     * @return
     */
    public static List<ChannelResponse> getChannelCache() {
        List<ChannelResponse> allChannel = CHANNEL_LOCAL_CACHE.getIfPresent("allChannel");
        if (CollUtil.isEmpty(allChannel)) {
            return Collections.emptyList();
        }
        return allChannel;
    }

    /**
     * 保存频道
     * @param list
     */
    public static void setChannelCache(List<ChannelResponse> list) {
        CHANNEL_LOCAL_CACHE.put("allChannel", list);
    }

    /**
     * 清除本地频道缓存
     */
    public static void clearChannelCache() {
        CHANNEL_LOCAL_CACHE.invalidate("allChannel");
    }
}
    
