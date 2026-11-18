package wcg.yhm.oss.biz.strategy;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import wcg.yhm.framework.context.utils.UserContextHolder;
import wcg.yhm.oss.biz.config.cos.CosProperties;
import wcg.yhm.oss.biz.enums.ErrorCodeEnum;
import wcg.yhm.oss.biz.utils.FileUtils;
import wcg.yhm.ymmknowledgeexchange.common.exception.BizException;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * @Author: 雨好绵
 * @CreateTime: 2025-11-26
 * @Description: 腾讯云oss策略
 * @Version: 1.0
 */

@Slf4j
@Component("COS")
public class CosOssStrategy implements FileStrategy {
    @Resource
    private COSClient cosClient;
    @Resource
    private CosProperties cosProperties;
    @Override
    public String uploadFile(MultipartFile file) {
        // 普通文件先转临时文件供 COS SDK 上传，上传调用结束后在 finally 中清理。
        log.info("==============> CosOssStrategy ===================");
        //校验文件合法性
        if (file.getSize() == 0) {
            throw new BizException(ErrorCodeEnum.EMPTY_FILE_ERROR);
        }
        //对文件名重新拼接
        String originalFilename = file.getOriginalFilename();
        String filePath = FileUtils.createFilePath("2", originalFilename);
        //因为cos 上传的是File 类型， 先要将 MultipartFile 转换成 File
        //创建临时文件
        File tempFile = null;
        try {
            String tempPath = "temp/" + filePath;
            tempFile = File.createTempFile(tempPath, null);
            file.transferTo(tempFile);
        } catch (IOException e) {
            throw new BizException(ErrorCodeEnum.CREATE_TEMP_FILE_FAIL);
        }
        String bucket = cosProperties.getBucket();
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, filePath, tempFile);
        try {
            PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);
            return cosProperties.getHost() + "/" + filePath;
        } catch (CosClientException e) {
            log.info("============== cos 上传文件失败 =================");
            throw new BizException(ErrorCodeEnum.FILE_UPLOAD_FAIL);
        } finally {
            //删除临时文件
            FileUtils.deleteTempFile(tempFile);
        }
    }

    @Override
    public String uploadPptx(MultipartFile file) {
        // PPTX 按用户和日期生成对象路径，直接使用输入流并设置文件类型元数据。
        log.info("==============> CosOssStrategy uploadPptx ===================");
        if (file.getSize() == 0) {
            throw new BizException(ErrorCodeEnum.EMPTY_FILE_ERROR);
        }

        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BizException(ErrorCodeEnum.FILE_UPLOAD_FAIL);
        }

        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String filePath = String.format("pptx/%s/%s/%s.pptx", userId, date, UUID.randomUUID());

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType("application/vnd.openxmlformats-officedocument.presentationml.presentation");

        try {
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    cosProperties.getBucket(),
                    filePath,
                    file.getInputStream(),
                    metadata
            );
            PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);
            log.info("============== COS PPTX上传成功, etag: {}, path: {} =================",
                    putObjectResult.getETag(), filePath);
            return cosProperties.getHost() + "/" + filePath;
        } catch (IOException | CosClientException e) {
            log.error("============== cos 上传PPTX失败 =================", e);
            throw new BizException(ErrorCodeEnum.FILE_UPLOAD_FAIL);
        }
    }
}
    
