package wcg.yhm.oss.biz.utils;

import lombok.extern.slf4j.Slf4j;
import wcg.yhm.oss.biz.enums.ErrorCodeEnum;
import wcg.yhm.ymmknowledgeexchange.common.exception.BizException;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-27
 * @Description: 文件工具类
 * @Version: 1.0
 */
@Slf4j
public class FileUtils {

    /**
     * 创建存储地址
     * @param userId 当前用户id
     * @param originalFilename 原始文件名
     * @return 文件存储路径(不含服务地址)
     * images/user123/2025-12-01/a1b2c3d4-e5f6-7890-g1h2-i3j4k5l6m7n8.jpg
     * video/user123/2025-12-01/a1b2c3d4-e5f6-7890-g1h2-i3j4k5l6m7n8.mp4
     */
    public static String createFilePath(String userId, String originalFilename) {
        //检验文件名
        if (originalFilename == null) {
            throw new BizException(ErrorCodeEnum.FILE_NAME_ERROR);
        }
        int index = originalFilename.lastIndexOf('.');
        //校验文件是否有后缀
        if (index == -1) {
            throw new BizException(ErrorCodeEnum.FILE_NAME_ERROR);
        }
        //获取文件后缀 .jpg
        //可以补充校验允许上传文件后缀
        //根据文件后缀 补充上传前缀，是图片类型就放在 /images 下， 视频 /video
        String suffix = originalFilename.substring(index);
        List<String> photoList = Arrays.asList(".jpeg", ".png", ".jpg", ".gif", ".webp");
        List<String> videoList = Arrays.asList(".mp4", ".mov", ".avi");
        //suffix.toLowerCase() 统一转小写进行匹配
        suffix = suffix.toLowerCase();
        String uploadPrefix = ""; //默认是图片；
        if (photoList.contains(suffix)) {
            uploadPrefix = "images";
        } else if (videoList.contains(suffix)) {
            uploadPrefix = "video";
        }
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String fileName = UUID.randomUUID().toString();
        String filePath = String.format("%s/%s/%s/%s",uploadPrefix, userId, date, fileName + suffix);
        log.info("=============== 生成的路径: [{}] =====================", filePath);
        return filePath;
    }

    /**
     * 删除创建的临时文件
     * @param file
     */
    public static void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        boolean deleteResult = file.delete();
        if (!deleteResult) {
            log.error("临时文件删除失败, path: {}", file.getAbsolutePath());
        }
    }
}
    
