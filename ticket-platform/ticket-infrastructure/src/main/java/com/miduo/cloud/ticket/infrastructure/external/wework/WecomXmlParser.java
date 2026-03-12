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
}
