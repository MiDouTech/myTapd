package com.miduo.cloud.ticket.application.wecom;

import com.miduo.cloud.ticket.infrastructure.external.qiniu.QiniuProperties;
import com.miduo.cloud.ticket.infrastructure.external.qiniu.QiniuUploadService;
import com.miduo.cloud.ticket.infrastructure.external.wework.WecomClient;
import com.miduo.cloud.ticket.infrastructure.external.wework.WecomProperties;
import com.miduo.cloud.ticket.infrastructure.external.wework.WeworkRuntimeConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;

/**
 * 企微图片下载与持久化服务
 * 接口编号：（内部服务，无对外 REST 接口）
 * <p>
 * 企微智能机器人（aibot）图片消息格式（官方文档 /document/path/100719）：
 *   {"msgtype":"image","image":{"url":"https://ww-aibot-img-...（5分钟有效）"}}
 * <p>
 * 图片内容已使用回调 callbackAesKey 做 AES-256-CBC 加密：
 *   - AES Key  = Base64.decode(callbackAesKey + "=")，32字节
 *   - IV       = AES Key 前 16 字节
 *   - 填充     = PKCS#7，填充至 32 字节倍数
 * 视频消息（msgtype=video）的 video.url 与图片规则相同；大视频采用临时文件 + 分块解密，避免密文与明文双份大数组常驻堆。
 */
@Service
public class WecomImageDownloadService {

    private static final Logger log = LoggerFactory.getLogger(WecomImageDownloadService.class);

    private static final int AES_KEY_BYTES = 32;
    private static final int IV_BYTES = 16;
    private static final int PKCS7_BLOCK_SIZE = 32;

    private static final int STREAM_BUFFER_SIZE = 65536;

    private final WecomClient wecomClient;
    private final QiniuUploadService qiniuUploadService;
    private final QiniuProperties qiniuProperties;
    private final WecomProperties wecomProperties;
    private final WeworkRuntimeConfigProvider runtimeConfigProvider;

    public WecomImageDownloadService(WecomClient wecomClient,
                                     QiniuUploadService qiniuUploadService,
                                     QiniuProperties qiniuProperties,
                                     WecomProperties wecomProperties,
                                     WeworkRuntimeConfigProvider runtimeConfigProvider) {
        this.wecomClient = wecomClient;
        this.qiniuUploadService = qiniuUploadService;
        this.qiniuProperties = qiniuProperties;
        this.wecomProperties = wecomProperties;
        this.runtimeConfigProvider = runtimeConfigProvider;
    }

    /**
     * 下载企微图片并上传到七牛云，返回持久化 URL。
     * 兼容两种来源：
     *   1. 智能机器人（aibot）图片：imageUrl = image.url，需用 callbackAesKey 解密
     *   2. 普通应用 MediaId / PicUrl（保留降级能力）
     *
     * @param mediaId   企微 MediaId（普通应用场景，可为空）
     * @param picUrl    企微临时预览 URL（可为空）
     * @param imageUrl  企微 aibot 图片加密下载 URL（来自 image.url，可为空）
     * @param msgId     消息 ID（用于日志）
     * @return 七牛云持久化 URL，全部失败时返回 null
     */
    public String downloadAndUpload(String mediaId, String picUrl, String imageUrl, String msgId) {
        log.info("企微图片下载开始: msgId={}, hasImageUrl={}, hasMediaId={}, hasPicUrl={}",
                msgId, isNotBlank(imageUrl), isNotBlank(mediaId), isNotBlank(picUrl));

        if (!isNotBlank(imageUrl) && !isNotBlank(mediaId) && !isNotBlank(picUrl)) {
            log.error("企微图片下载字段全部为空，无法下载: msgId={}", msgId);
            return null;
        }

        byte[] imageBytes = null;
        String downloadSource = null;

        // 策略1：aibot 加密图片 URL（主路径）
        if (isNotBlank(imageUrl)) {
            imageBytes = downloadDecryptAibotEncryptedUrlToBytes(imageUrl, msgId);
            if (imageBytes != null && imageBytes.length > 0) {
                downloadSource = "AibotImageUrl";
                log.info("企微aibot图片解密下载成功: msgId={}, size={}", msgId, imageBytes.length);
            } else {
                log.warn("企微aibot图片解密下载失败，降级MediaId: msgId={}", msgId);
            }
        }

        // 策略2：普通应用 MediaId API
        if (imageBytes == null && isNotBlank(mediaId)) {
            imageBytes = wecomClient.downloadMediaById(mediaId);
            if (imageBytes != null && imageBytes.length > 0) {
                downloadSource = "MediaId";
                log.info("企微图片MediaId下载成功: msgId={}, size={}", msgId, imageBytes.length);
            } else {
                log.warn("企微图片MediaId下载失败，降级PicUrl: msgId={}, mediaId={}", msgId, mediaId);
            }
        }

        // 策略3：PicUrl HTTP 直链
        if (imageBytes == null && isNotBlank(picUrl)) {
            imageBytes = wecomClient.downloadImageByUrl(picUrl);
            if (imageBytes != null && imageBytes.length > 0) {
                downloadSource = "PicUrl";
                log.info("企微图片PicUrl下载成功: msgId={}, size={}", msgId, imageBytes.length);
            } else {
                log.warn("企微图片PicUrl下载也失败: msgId={}, picUrl={}", msgId, picUrl);
            }
        }

        if (imageBytes == null || imageBytes.length == 0) {
            log.error("企微图片下载全部失败，放弃上传: msgId={}", msgId);
            return null;
        }

        String originalName = "wecom_" + msgId + ".jpg";
        String qiniuUrl = qiniuUploadService.uploadImageBytes(imageBytes, originalName);
        if (qiniuUrl == null) {
            log.error("企微图片上传七牛云失败: msgId={}, downloadSource={}", msgId, downloadSource);
            return null;
        }

        log.info("企微图片下载并上传成功: msgId={}, downloadSource={}, qiniuUrl={}", msgId, downloadSource, qiniuUrl);
        return qiniuUrl;
    }

    /**
     * 兼容旧签名：mediaId + picUrl + downloadUrl(imageUrl) + aesKey(已废弃，来自消息字段不再使用)
     * AES Key 统一从 callbackAesKey 配置读取。
     */
    public String downloadAndUpload(String mediaId, String picUrl, String imageUrl, String ignoredAesKey, String msgId) {
        return downloadAndUpload(mediaId, picUrl, imageUrl, msgId);
    }

    /**
     * 下载并解密智能机器人 video.url（与 image.url 加密规则相同），再上传七牛云。
     * 大视频：HTTP 流写入临时密文文件 → 分块解密至临时明文文件 → 去填充 → FileInputStream 上传 → 删除临时文件。
     * 小视频（密文体积 ≤ 配置阈值）：内存解密后 ByteArrayInputStream 上传。
     *
     * @param videoUrl 视频加密下载地址
     * @param msgId    消息 ID（日志）
     * @return 七牛持久化 URL，失败返回 null
     */
    public String downloadDecryptAndUploadVideo(String videoUrl, String msgId) {
        if (!isNotBlank(videoUrl)) {
            log.warn("企微视频下载 URL 为空: msgId={}", msgId);
            return null;
        }

        Path encFile = null;
        Path plainFile = null;
        try {
            encFile = streamEncryptedUrlToTempFile(videoUrl.trim(), msgId);
            if (encFile == null) {
                return null;
            }
            long encSize = Files.size(encFile);
            if (encSize <= 0) {
                log.warn("企微视频加密包为空: msgId={}", msgId);
                return null;
            }

            long thresholdBytes = resolveVideoInMemoryThresholdBytes();
            String originalName = "wecom_" + msgId + ".mp4";

            if (encSize <= thresholdBytes) {
                byte[] encrypted = Files.readAllBytes(encFile);
                byte[] plain = decryptAibotCiphertextRemovePadding(encrypted, msgId);
                if (plain == null || plain.length == 0) {
                    log.warn("企微视频内存解密失败: msgId={}", msgId);
                    return null;
                }
                try (ByteArrayInputStream in = new ByteArrayInputStream(plain)) {
                    String url = qiniuUploadService.uploadStream(in, originalName, "video/mp4",
                            qiniuProperties.getVideoPathPrefix());
                    if (url != null) {
                        log.info("企微视频解密并上传成功(内存路径): msgId={}, size={}", msgId, plain.length);
                    }
                    return url;
                }
            }

            plainFile = Files.createTempFile(resolveVideoTempDirectory(), "wecom-vid-dec-", ".mp4");
            decryptAibotCiphertextFileToFile(encFile, plainFile, msgId);
            stripPkcs7PaddingFromPlainFile(plainFile, msgId);

            try (InputStream in = Files.newInputStream(plainFile)) {
                String url = qiniuUploadService.uploadStream(in, originalName, "video/mp4",
                        qiniuProperties.getVideoPathPrefix());
                if (url != null) {
                    log.info("企微视频解密并上传成功(临时文件路径): msgId={}, encSize={}", msgId, encSize);
                }
                return url;
            }
        } catch (Exception e) {
            log.warn("企微视频解密或上传失败: msgId={}, error={}", msgId, e.getMessage());
            return null;
        } finally {
            deleteQuietly(encFile);
            deleteQuietly(plainFile);
        }
    }

    private long resolveVideoInMemoryThresholdBytes() {
        int mb = 8;
        WecomProperties.ImageConfig image = wecomProperties.getImage();
        if (image != null) {
            mb = image.getVideoDecryptInMemoryMaxMb();
        }
        if (mb < 0) {
            mb = 0;
        }
        return mb * 1024L * 1024L;
    }

    private Path resolveVideoTempDirectory() throws java.io.IOException {
        WecomProperties.ImageConfig image = wecomProperties.getImage();
        String custom = image != null ? image.getVideoTempDirectory() : null;
        if (custom != null && !custom.trim().isEmpty()) {
            Path dir = Paths.get(custom.trim());
            Files.createDirectories(dir);
            return dir;
        }
        return Paths.get(System.getProperty("java.io.tmpdir"));
    }

    /**
     * 将加密 URL 内容流式写入临时文件。
     */
    private Path streamEncryptedUrlToTempFile(String url, String msgId) {
        WeworkRuntimeConfigProvider.RuntimeConfig cfg = runtimeConfigProvider.getRuntimeConfig();
        int connectTimeout = cfg.getConnectTimeoutMs() != null ? cfg.getConnectTimeoutMs() : 10000;
        int readTimeout = cfg.getReadTimeoutMs() != null ? cfg.getReadTimeoutMs() : 600000;

        Path encFile = null;
        try {
            encFile = Files.createTempFile(resolveVideoTempDirectory(), "wecom-vid-enc-", ".bin");
            cn.hutool.http.HttpResponse response = cn.hutool.http.HttpRequest.get(url)
                    .timeout(connectTimeout + readTimeout)
                    .execute();

            String contentType = response.header("Content-Type");
            if (contentType != null && contentType.contains("application/json")) {
                String body = response.body();
                log.warn("企微视频URL返回错误(JSON): msgId={}, response={}", msgId,
                        body != null && body.length() > 200 ? body.substring(0, 200) : body);
                deleteQuietly(encFile);
                return null;
            }

            try (InputStream in = response.bodyStream();
                 OutputStream out = Files.newOutputStream(encFile)) {
                if (in == null) {
                    log.warn("企微视频URL响应体为空: msgId={}", msgId);
                    deleteQuietly(encFile);
                    return null;
                }
                byte[] buf = new byte[STREAM_BUFFER_SIZE];
                int n;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                }
            }
            return encFile;
        } catch (Exception e) {
            log.warn("企微视频加密包下载失败: msgId={}, error={}", msgId, e.getMessage());
            deleteQuietly(encFile);
            return null;
        }
    }

    /**
     * 分块解密：密文文件 → 明文文件（仍含 PKCS#7 填充）。
     * 手动按 16 字节块喂给 Cipher，避免不同 JCE 对非对齐 {@code update} 的行为差异。
     */
    private void decryptAibotCiphertextFileToFile(Path encFile, Path plainFile, String msgId) throws Exception {
        final int aesBlock = 16;
        Cipher cipher = buildAibotDecryptCipher();
        try (InputStream fis = Files.newInputStream(encFile);
             OutputStream fos = Files.newOutputStream(plainFile)) {
            byte[] inBuf = new byte[STREAM_BUFFER_SIZE];
            byte[] carry = new byte[aesBlock];
            int carryLen = 0;
            int read;
            while ((read = fis.read(inBuf)) != -1) {
                int offset = 0;
                if (carryLen > 0) {
                    int need = Math.min(aesBlock - carryLen, read);
                    System.arraycopy(inBuf, 0, carry, carryLen, need);
                    carryLen += need;
                    offset = need;
                    if (carryLen == aesBlock) {
                        byte[] out = cipher.update(carry, 0, aesBlock);
                        if (out != null && out.length > 0) {
                            fos.write(out);
                        }
                        carryLen = 0;
                    }
                }
                int avail = read - offset;
                int aligned = (avail / aesBlock) * aesBlock;
                if (aligned > 0) {
                    byte[] out = cipher.update(inBuf, offset, aligned);
                    if (out != null && out.length > 0) {
                        fos.write(out);
                    }
                    offset += aligned;
                }
                int tail = read - offset;
                if (tail > 0) {
                    System.arraycopy(inBuf, offset, carry, 0, tail);
                    carryLen = tail;
                }
            }
            if (carryLen != 0) {
                throw new IllegalStateException("企微视频密文长度非16字节对齐, remainder=" + carryLen);
            }
            byte[] finalOut = cipher.doFinal();
            if (finalOut != null && finalOut.length > 0) {
                fos.write(finalOut);
            }
        } catch (Exception e) {
            log.warn("企微视频分块解密失败: msgId={}, error={}", msgId, e.getMessage());
            throw e;
        }
    }

    /**
     * 对明文文件尾部做 PKCS#7(块32) 校验并截断。
     */
    private void stripPkcs7PaddingFromPlainFile(Path plainFile, String msgId) {
        try {
            long len = Files.size(plainFile);
            if (len <= 0) {
                return;
            }
            try (RandomAccessFile raf = new RandomAccessFile(plainFile.toFile(), "rw")) {
                raf.seek(len - 1);
                int pad = raf.readByte() & 0xFF;
                if (pad < 1 || pad > PKCS7_BLOCK_SIZE) {
                    log.warn("企微视频明文填充字节值非法（{}），保留原文件: msgId={}", pad, msgId);
                    return;
                }
                if (len < pad) {
                    log.warn("企微视频明文长度小于填充值: msgId={}", msgId);
                    return;
                }
                raf.seek(len - pad);
                for (int i = 0; i < pad; i++) {
                    if ((raf.readByte() & 0xFF) != pad) {
                        log.warn("企微视频明文填充校验不一致，保留原文件: msgId={}", msgId);
                        return;
                    }
                }
                raf.setLength(len - pad);
            }
        } catch (Exception e) {
            log.warn("企微视频去填充失败: msgId={}, error={}", msgId, e.getMessage());
        }
    }

    private byte[] decryptAibotCiphertextRemovePadding(byte[] encryptedBytes, String msgId) {
        try {
            Cipher cipher = buildAibotDecryptCipher();
            byte[] decrypted = cipher.doFinal(encryptedBytes);
            return removePkcs7Padding(decrypted, msgId);
        } catch (Exception e) {
            log.warn("企微aibot密文解密失败: msgId={}, error={}", msgId, e.getMessage());
            return null;
        }
    }

    private Cipher buildAibotDecryptCipher() throws Exception {
        byte[] keyBytes = decodeCallbackAesKey();
        byte[] iv = Arrays.copyOfRange(keyBytes, 0, IV_BYTES);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
        return cipher;
    }

    /**
     * 下载 URL 到内存并解密（图片主路径）。
     */
    private byte[] downloadDecryptAibotEncryptedUrlToBytes(String resourceUrl, String msgId) {
        try {
            byte[] encryptedBytes = cn.hutool.http.HttpUtil.downloadBytes(resourceUrl.trim());
            if (encryptedBytes == null || encryptedBytes.length == 0) {
                log.warn("企微aibot资源URL下载返回空数据: msgId={}", msgId);
                return null;
            }
            return decryptAibotCiphertextRemovePadding(encryptedBytes, msgId);
        } catch (Exception e) {
            log.warn("企微aibot资源下载或解密失败: msgId={}, error={}", msgId, e.getMessage());
            return null;
        }
    }

    /**
     * 从配置读取并解码回调 AES 密钥（32字节）。
     * 与 WecomCallbackCryptoService.decodeAesKey() 使用相同逻辑。
     */
    private byte[] decodeCallbackAesKey() {
        String callbackAesKey = runtimeConfigProvider.getRuntimeConfig().getCallbackAesKey();
        if (callbackAesKey == null || callbackAesKey.trim().isEmpty()) {
            throw new IllegalStateException("企微回调AESKey未配置，无法解密图片");
        }
        byte[] key = Base64.getDecoder().decode(callbackAesKey.trim() + "=");
        if (key.length != AES_KEY_BYTES) {
            throw new IllegalStateException("企微回调AESKey解码长度非法: " + key.length + "字节，期望32字节");
        }
        return key;
    }

    /**
     * 去除 PKCS#7 填充（块大小 PKCS7_BLOCK_SIZE=32）。
     */
    private byte[] removePkcs7Padding(byte[] data, String msgId) {
        if (data == null || data.length == 0) {
            return data;
        }
        int pad = data[data.length - 1] & 0xFF;
        if (pad < 1 || pad > PKCS7_BLOCK_SIZE) {
            log.warn("企微aibot解密：填充字节值非法（{}），按原始数据返回: msgId={}", pad, msgId);
            return data;
        }
        for (int i = data.length - pad; i < data.length; i++) {
            if ((data[i] & 0xFF) != pad) {
                log.warn("企微aibot解密：填充字节内容不一致，按原始数据返回: msgId={}", msgId);
                return data;
            }
        }
        return Arrays.copyOfRange(data, 0, data.length - pad);
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (Exception ignored) {
            // 临时文件删除失败仅忽略，避免掩盖主异常
        }
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
