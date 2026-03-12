package com.miduo.cloud.ticket.infrastructure.external.wework;

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
