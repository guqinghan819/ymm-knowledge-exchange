package wcg.yhm.ymmknowledgeexchange.common.utils;


import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;

import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-20
 * @Description: json工具
 * @Version: 1.0
 */


public class JsonUtil {
    private static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        OBJECT_MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        OBJECT_MAPPER.registerModules(new JavaTimeModule()); // 解决 LocalDateTime 的序列化问题
    }

    /**
     * 初始化：统一使用 Spring Boot 个性化配置的 ObjectMapper
     *
     * @param objectMapper
     */
    public static void init(ObjectMapper objectMapper) {
        OBJECT_MAPPER = objectMapper;
    }

    /**
     * 将对象转换成json字符串
     * @param object
     * @return
     */
    @SneakyThrows
    public static String toJsonString(Object object) {
        return OBJECT_MAPPER.writeValueAsString(object);
    }

    /**
     * 将json字符串转换成对象
     * @param jsonStr
     * @param clazz
     * @return
     * @param <T>
     */
    @SneakyThrows
    public static <T> T parseObject(String jsonStr, Class<T> clazz) {
        if (StrUtil.isBlank(jsonStr)) {
            return null;
        }
        return OBJECT_MAPPER.readValue(jsonStr, clazz);
    }


    @SneakyThrows
    public static <K, V> Map<K, V> Str2Map(String jsonStr, Class<K> keyClazz, Class<V> valueClazz) {
        if (StrUtil.isBlank(jsonStr)) {
            return null;
        }
        //创建TypeReference 指定泛型类型
        TypeReference<Map<K, V>> typeReference = new TypeReference<>() {
        };

        return OBJECT_MAPPER
                .readValue(jsonStr, OBJECT_MAPPER.getTypeFactory().constructMapType(Map.class, keyClazz, valueClazz));
    }

    @SneakyThrows
    public static <T> List<T> str2List(String jsonStr, Class<T> clazz) {
        if (StrUtil.isBlank(jsonStr)) {
            return null;
        }
        TypeReference<List<T>> typeReference = new TypeReference<>(){};
        return OBJECT_MAPPER.readValue(jsonStr, OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
    }


    /**
     * 将 JSON 字符串解析为指定类型的 Set 对象
     *
     * @param jsonStr
     * @param clazz
     * @return
     * @param <T>
     * @throws Exception
     */
    public static <T> Set<T> parseSet(String jsonStr, Class<T> clazz) throws Exception {
        // 使用 TypeReference 指定 Set<T> 的泛型类型
        return OBJECT_MAPPER.readValue(jsonStr, new TypeReference<>() {
            @Override
            public CollectionType getType() {
                return OBJECT_MAPPER.getTypeFactory().constructCollectionType(Set.class, clazz);
            }
        });
    }
}
    
