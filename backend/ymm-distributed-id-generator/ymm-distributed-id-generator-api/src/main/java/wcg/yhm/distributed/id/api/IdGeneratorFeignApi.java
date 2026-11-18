package wcg.yhm.distributed.id.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import wcg.yhm.distributed.id.constant.ApiConstants;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-12-22
 * @Description:
 * @Version: 1.0
 */

@FeignClient(name = ApiConstants.SERVICE_NAME)
public interface IdGeneratorFeignApi {
    String PREFIX = "/id";
    @GetMapping(value = PREFIX + "/segment/get/{key}")
    String getSegmentId(@PathVariable("key") String key);

    @GetMapping(value = PREFIX + "/snowflake/get/{key}")
    String getSnowflakeId(@PathVariable("key") String key);
}
    
