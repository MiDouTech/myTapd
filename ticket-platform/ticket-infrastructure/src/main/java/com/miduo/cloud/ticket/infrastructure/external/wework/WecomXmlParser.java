package com.miduo.cloud.ticket.infrastructure.external.wework;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 企微XML解析工具
 */
public final class WecomXmlParser {

    private static final Logger log = LoggerFactory.getLogger(WecomXmlParser.class);

    private WecomXmlParser() {
    }

    /**
     * 从回调请求体中提取 Encrypt 字段。
     * 企微回调消息体可能是 XML 格式（&lt;xml&gt;&lt;Encrypt&gt;...&lt;/Encrypt&gt;&lt;/xml&gt;）
     * 也可能是 JSON 格式（{"encrypt":"..."}），此方法自动识别并提取。
     */
    public static String extractEncryptField(String requestBody) {
        if (requestBody == null || requestBody.trim().isEmpty()) {
            throw BusinessException.of(ErrorCode.WECOM_MSG_PARSE_FAILED, "回调消息体为空");
        }
        String trimmed = requestBody.trim();
        if (trimmed.startsWith("{")) {
            try {
                JSONObject json = JSONUtil.parseObj(trimmed);
                String encrypt = json.getStr("encrypt");
                if (encrypt == null || encrypt.trim().isEmpty()) {
                    encrypt = json.getStr("Encrypt");
                }
                if (encrypt == null || encrypt.trim().isEmpty()) {
                    log.error("企微JSON回调消息缺少encrypt字段，内容（前500字符）: {}",
                            trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed);
                    throw BusinessException.of(ErrorCode.WECOM_MSG_PARSE_FAILED, "企微JSON回调消息缺少encrypt字段");
                }
                return encrypt.trim();
            } catch (BusinessException ex) {
                throw ex;
            } catch (Exception ex) {
                log.error("企微JSON回调消息解析失败，内容（前500字符）: {}",
                        trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed, ex);
                throw BusinessException.of(ErrorCode.WECOM_MSG_PARSE_FAILED, "企微JSON回调消息解析失败");
            }
        } else {
            Map<String, String> map = parseFirstLevel(trimmed);
            String encrypt = map.get("Encrypt");
            if (encrypt == null || encrypt.trim().isEmpty()) {
                throw BusinessException.of(ErrorCode.WECOM_MSG_PARSE_FAILED, "企微XML回调消息缺少Encrypt字段");
            }
            return encrypt.trim();
        }
    }

    /**
     * 解析XML第一层节点为Map
     */
    public static Map<String, String> parseFirstLevel(String xml) {
        if (xml == null || xml.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setExpandEntityReferences(false);
            factory.setNamespaceAware(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xml)));
            Element root = document.getDocumentElement();
            if (root == null) {
                return Collections.emptyMap();
            }
            NodeList nodeList = root.getChildNodes();
            Map<String, String> result = new HashMap<>();
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    result.put(node.getNodeName(), node.getTextContent());
                }
            }
            return result;
        } catch (Exception ex) {
            log.error("企微XML解析失败，原始XML内容（前500字符）: {}", xml.length() > 500 ? xml.substring(0, 500) : xml, ex);
            throw BusinessException.of(ErrorCode.WECOM_MSG_PARSE_FAILED, "企微XML解析失败");
        }
    }

    /**
     * 解析企微解密后的消息体（自动识别 XML 或 JSON 格式）。
     * <p>
     * 企微普通应用/客服回调解密后为 XML；
     * 企微 AI 机器人（aibot）回调解密后为 JSON，字段名采用小写蛇形，
     * 且文本内容嵌套在 {@code text.content} 中、发送人位于 {@code from.userid} 中。
     * 此方法统一输出与 XML 解析结果字段名相同的 Map，供后续 buildMessage 使用：
     * <ul>
     *   <li>MsgType</li>
     *   <li>MsgId</li>
     *   <li>FromUserName</li>
     *   <li>ToUserName（aibot 场景映射为 aibotid）</li>
     *   <li>Content</li>
     *   <li>CreateTime</li>
     *   <li>ChatId</li>
     * </ul>
     */
    public static Map<String, String> parseDecryptedMessage(String plainText) {
        if (plainText == null || plainText.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        String trimmed = plainText.trim();
        if (trimmed.startsWith("{")) {
            return parseAibotJson(trimmed);
        }
        return parseFirstLevel(trimmed);
    }

    private static Map<String, String> parseAibotJson(String json) {
        try {
            JSONObject obj = JSONUtil.parseObj(json);
            Map<String, String> result = new HashMap<>();

            result.put("MsgId", nullToEmpty(obj.getStr("msgid")));
            result.put("MsgType", nullToEmpty(obj.getStr("msgtype")));
            result.put("CreateTime", nullToEmpty(obj.getStr("createtime")));
            result.put("ChatId", nullToEmpty(obj.getStr("chatid")));
            result.put("ResponseUrl", nullToEmpty(obj.getStr("response_url")));
            result.put("ChatType", nullToEmpty(obj.getStr("chattype")));

            // 发送人：from.userid
            JSONObject from = obj.getJSONObject("from");
            if (from != null) {
                result.put("FromUserName", nullToEmpty(from.getStr("userid")));
            } else {
                result.put("FromUserName", "");
            }

            // 接收人/机器人 id：aibotid 映射到 ToUserName
            result.put("ToUserName", nullToEmpty(obj.getStr("aibotid")));

            // 根据消息类型解析具体内容字段
            String msgType = result.get("MsgType");
            if ("text".equalsIgnoreCase(msgType)) {
                // 文本内容：text.content
                JSONObject text = obj.getJSONObject("text");
                if (text != null) {
                    result.put("Content", nullToEmpty(text.getStr("content")));
                } else {
                    result.put("Content", "");
                }
            } else if ("image".equalsIgnoreCase(msgType)) {
                // 图片消息原始JSON诊断日志（DEBUG级别）
                log.debug("企微 aibot 图片消息原始JSON: {}", json.length() > 3000 ? json.substring(0, 3000) : json);
                result.put("Content", "");
                extractAibotImageFields(obj, result, json);
            } else {
                result.put("Content", "");
            }

            log.debug("企微 aibot JSON 消息解析完成: msgId={}, msgType={}, from={}",
                    result.get("MsgId"), result.get("MsgType"), result.get("FromUserName"));
            return result;
        } catch (Exception ex) {
            log.error("企微 aibot JSON 消息解析失败，内容（前500字符）: {}",
                    json.length() > 500 ? json.substring(0, 500) : json, ex);
            throw BusinessException.of(ErrorCode.WECOM_MSG_PARSE_FAILED, "企微aibot JSON消息解析失败");
        }
    }

    /**
     * 提取企微 AI bot 图片消息字段，兼容多种 JSON 结构：
     * 1. 标准格式：image.media_id / image.pic_url / image.download_url / image.aes_key
     * 2. 备用格式（部分版本）：image.Media_Id / image.Pic_Url / image.Download_Url / image.Aes_Key
     * 3. 根节点平铺：media_id / pic_url / download_url / aes_key
     */
    private static void extractAibotImageFields(JSONObject obj, Map<String, String> result, String rawJson) {
        String mediaId = "";
        String picUrl = "";
        String downloadUrl = "";
        String aesKey = "";
        boolean foundInSubObj = false;

        // 尝试从 "image" 或 "Image" 子对象提取
        JSONObject imageObj = getNestedObject(obj, "image", "Image");
        if (imageObj != null) {
            foundInSubObj = true;
            mediaId = firstNonEmpty(imageObj.getStr("media_id"), imageObj.getStr("MediaId"), imageObj.getStr("Media_Id"));
            picUrl = firstNonEmpty(imageObj.getStr("pic_url"), imageObj.getStr("PicUrl"), imageObj.getStr("Pic_Url"));
            downloadUrl = firstNonEmpty(imageObj.getStr("download_url"), imageObj.getStr("DownloadUrl"), imageObj.getStr("Download_Url"));
            aesKey = firstNonEmpty(imageObj.getStr("aes_key"), imageObj.getStr("AesKey"), imageObj.getStr("Aes_Key"), imageObj.getStr("AES_Key"));
        }

        // 降级：若子对象为空或所有字段均为空，尝试从根节点提取
        if (!foundInSubObj || (mediaId.isEmpty() && picUrl.isEmpty() && downloadUrl.isEmpty())) {
            String rootMediaId = firstNonEmpty(obj.getStr("media_id"), obj.getStr("MediaId"));
            String rootPicUrl = firstNonEmpty(obj.getStr("pic_url"), obj.getStr("PicUrl"));
            String rootDownloadUrl = firstNonEmpty(obj.getStr("download_url"), obj.getStr("DownloadUrl"));
            String rootAesKey = firstNonEmpty(obj.getStr("aes_key"), obj.getStr("AesKey"), obj.getStr("AES_Key"));

            if (!rootMediaId.isEmpty() || !rootPicUrl.isEmpty() || !rootDownloadUrl.isEmpty()) {
                mediaId = rootMediaId;
                picUrl = rootPicUrl;
                downloadUrl = rootDownloadUrl;
                aesKey = rootAesKey;
                log.info("企微图片消息字段从根节点提取: hasMediaId={}, hasPicUrl={}, hasDownloadUrl={}, hasAesKey={}",
                        !mediaId.isEmpty(), !picUrl.isEmpty(), !downloadUrl.isEmpty(), !aesKey.isEmpty());
            }
        }

        result.put("MediaId", nullToEmpty(mediaId));
        result.put("PicUrl", nullToEmpty(picUrl));
        result.put("DownloadUrl", nullToEmpty(downloadUrl));
        result.put("AesKey", nullToEmpty(aesKey));

        if (mediaId.isEmpty() && picUrl.isEmpty() && downloadUrl.isEmpty()) {
            log.warn("企微图片消息所有下载字段均为空（imageSubObj={}），原始JSON片段: {}",
                    foundInSubObj,
                    rawJson.length() > 500 ? rawJson.substring(0, 500) : rawJson);
        } else {
            log.info("企微图片消息字段解析成功: imageSubObj={}, hasMediaId={}, hasPicUrl={}, hasDownloadUrl={}, hasAesKey={}",
                    foundInSubObj, !mediaId.isEmpty(), !picUrl.isEmpty(), !downloadUrl.isEmpty(), !aesKey.isEmpty());
        }
    }

    /**
     * 按优先级从 JSONObject 中获取第一个非空非null的嵌套对象
     */
    private static JSONObject getNestedObject(JSONObject parent, String... keys) {
        for (String key : keys) {
            Object val = parent.get(key);
            if (val instanceof JSONObject) {
                return (JSONObject) val;
            }
            if (val != null && !(val instanceof String)) {
                // 尝试转换
                try {
                    return JSONUtil.parseObj(val.toString());
                } catch (Exception ignored) {
                    // 转换失败，尝试下一个key
                }
            }
        }
        return null;
    }

    /**
     * 返回第一个非空非null的字符串值
     */
    private static String firstNonEmpty(String... values) {
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) {
                return v.trim();
            }
        }
        return "";
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
