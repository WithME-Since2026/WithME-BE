package yooze.withme.common.storage;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import yooze.withme.common.exception.GeneralException;
import yooze.withme.common.status.ErrorStatus;

/** 프로필 이미지 S3 업로드/삭제. 자격 증명은 SDK 기본 체인(인스턴스 역할, AWS_* 환경변수)을 쓴다 */
@Slf4j
@Component
public class S3ImageStorage {

    private final S3Client s3Client;
    private final String bucket;
    private final String urlPrefix;

    public S3ImageStorage(@Value("${aws.region:ap-northeast-2}") String region, @Value("${aws.s3.bucket:}") String bucket) {
        this.s3Client = S3Client.builder().region(Region.of(region)).build();
        this.bucket = bucket;
        this.urlPrefix = "https://" + bucket + ".s3." + region + ".amazonaws.com/";
    }

    /** 업로드 후 공개 URL 반환 */
    public String upload(String key, byte[] bytes, String contentType) {
        try {
            s3Client.putObject(req -> req.bucket(bucket).key(key).contentType(contentType), RequestBody.fromBytes(bytes));
            return urlPrefix + key;
        } catch (SdkException e) {
            log.error("[*] S3 업로드 실패 - key: {}", key, e);
            throw new GeneralException(ErrorStatus.IMAGE_UPLOAD_FAILED);
        }
    }

    /** 이 버킷에 올린 이미지일 때만 삭제. 실패해도 요청은 성공시키고 로그만 남긴다 */
    public void delete(String url) {
        if (url == null || !url.startsWith(urlPrefix)) {
            return;
        }
        String key = url.substring(urlPrefix.length());
        try {
            s3Client.deleteObject(req -> req.bucket(bucket).key(key));
        } catch (SdkException e) {
            log.warn("[*] S3 이미지 삭제 실패 - key: {} : {}", key, e.getMessage());
        }
    }
}
