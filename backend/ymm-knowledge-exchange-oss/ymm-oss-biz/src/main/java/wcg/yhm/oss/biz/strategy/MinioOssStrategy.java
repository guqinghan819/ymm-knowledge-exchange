package wcg.yhm.oss.biz.strategy;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import io.minio.*;
import io.minio.errors.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import wcg.yhm.framework.context.utils.UserContextHolder;
import wcg.yhm.oss.biz.config.minio.MinioProperties;
import wcg.yhm.oss.biz.enums.ErrorCodeEnum;
import wcg.yhm.oss.biz.utils.FileUtils;
import wcg.yhm.ymmknowledgeexchange.common.exception.BizException;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-26
 * @Description: minio存储策略
 * @Version: 1.0
 */

@Slf4j
@Component("MINIO")
public class MinioOssStrategy implements FileStrategy {
    @Resource
    private MinioClient minioClient;
    @Resource
    private MinioProperties minioProperties;
    @Override
    public String uploadFile(MultipartFile file) {
        log.info("=============> MinioOssStrategy =============");
        try {
            //查询桶名是否存在, 不存在就创建
            String bucketName = minioProperties.getBucket();
            boolean result = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!result) {
                //创建桶
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            } else {
                log.info("=========== [{}] 桶已经存在 ヾ(*´∀ ˋ*)ﾉ ===========", bucketName);
            }
            String originalFilename = file.getOriginalFilename();
            //获取文件类型
            String contentType = file.getContentType();
            //获取当前用户id
            Long userId = UserContextHolder.getUserId();
            //String userId = "2";
            //创建文件存储路径
            String filePath = FileUtils.createFilePath(userId.toString(), originalFilename);
            //上传文件
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .contentType(contentType)
                            .bucket(bucketName)
                            .object(filePath)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .build()
            );
            //返回访问地址 服务地址/桶名/文件地址
            String url = String.format("%s/%s/%s", minioProperties.getEndPoint(), bucketName, filePath);
            log.info("================ 图片所在地址 url: [{}] =====================", url);
            return url;
        } catch (Exception e) {
            log.error("================ Minio服务文件上传异常 (#`Д´)ﾉ message: {}=================", e.getMessage());
            throw new BizException(ErrorCodeEnum.FILE_UPLOAD_FAIL);
        }
    }

    @Override
    public String uploadPptx(MultipartFile file) {
        log.info("=============> MinioOssStrategy uploadPptx =============");
        try {
            String bucketName = minioProperties.getBucket();
            boolean result = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!result) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            } else {
                log.info("=========== [{}] 桶已经存在 ===========", bucketName);
            }
            String contentType = file.getContentType();
            Long userId = UserContextHolder.getUserId();
            String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String fileName = UUID.randomUUID().toString();
            String filePath = String.format("pptx/%s/%s/%s.pptx", userId, date, fileName);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .contentType(contentType)
                            .bucket(bucketName)
                            .object(filePath)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .build()
            );
            String url = String.format("%s/%s/%s", minioProperties.getEndPoint(), bucketName, filePath);
            log.info("================ PPTX文件地址 url: [{}] =================", url);
            return url;
        } catch (Exception e) {
            log.error("================ Minio PPTX上传异常 message: {} =================", e.getMessage());
            throw new BizException(ErrorCodeEnum.FILE_UPLOAD_FAIL);
        }
    }
}
    
