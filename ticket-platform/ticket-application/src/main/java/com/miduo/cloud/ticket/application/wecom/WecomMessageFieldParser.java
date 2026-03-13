package com.miduo.cloud.ticket.application.wecom;

import com.miduo.cloud.ticket.entity.dto.wecom.WecomMessageParseOutput;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 企微客服消息字段解析器（自然语言 → 客服信息字段）
 * 接口编号：API000504 内部组件
 * <p>
 * 解析策略（按优先级）：
 * 1. 标签-值对匹配：识别"商户编号：XXXXX"等结构化格式
 * 2. 正则提取：自动识别商户编号格式（6-12位数字）
 * 3. 图片URL提取：识别消息中的HTTP图片链接
 * 4. 上下文推断：无标签时将主体文本作为问题描述
 */
@Component
public class WecomMessageFieldParser {

    private static final Pattern MERCHANT_NO_PATTERN = Pattern.compile("(?<![\\d])([1-9]\\d{5,11})(?![\\d])");
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s，,；;。！!？?\"']+");
    private static final Pattern IMAGE_URL_PATTERN = Pattern.compile(
            "https?://[^\\s，,；;。！!？?\"']+\\.(?:jpg|jpeg|png|gif|webp|bmp)(\\?[^\\s，,；;。！!？?]*)?",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 字段别名映射：同义词 → 标准字段名
     */
    private static final Map<String, String> FIELD_ALIAS_MAP = buildFieldAliasMap();

    private static Map<String, String> buildFieldAliasMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("商户编号", "merchantNo");
        map.put("商户号", "merchantNo");
        map.put("mchid", "merchantNo");
        map.put("mch_id", "merchantNo");
        map.put("merchant_no", "merchantNo");
        map.put("商家编号", "merchantNo");

        map.put("公司名称", "companyName");
        map.put("公司", "companyName");
        map.put("商户名称", "companyName");
        map.put("商户名", "companyName");
        map.put("商家名称", "companyName");
        map.put("商家名", "companyName");
        map.put("企业名称", "companyName");

        map.put("商户账号", "merchantAccount");
        map.put("账号", "merchantAccount");
        map.put("账户", "merchantAccount");
        map.put("登录账号", "merchantAccount");
        map.put("登录账户", "merchantAccount");
        map.put("用户名", "merchantAccount");

        map.put("场景码", "sceneCode");
        map.put("场景", "sceneCode");
        map.put("scene", "sceneCode");
        map.put("scene_code", "sceneCode");
        map.put("业务场景", "sceneCode");

        map.put("问题描述", "problemDesc");
        map.put("问题", "problemDesc");
        map.put("描述", "problemDesc");
        map.put("故障描述", "problemDesc");
        map.put("bug描述", "problemDesc");
        map.put("现象", "problemDesc");
        map.put("现象描述", "problemDesc");
        map.put("反馈内容", "problemDesc");
        map.put("反馈", "problemDesc");

        map.put("预期结果", "expectedResult");
        map.put("期望结果", "expectedResult");
        map.put("预期", "expectedResult");
        map.put("期望", "expectedResult");
        map.put("期望行为", "expectedResult");
        map.put("正确行为", "expectedResult");

        map.put("截图", "problemScreenshot");
        map.put("问题截图", "problemScreenshot");
        map.put("图片", "problemScreenshot");
        map.put("附图", "problemScreenshot");
        return map;
    }

    /**
     * 解析企微消息文本，提取客服信息字段
     *
     * @param rawMessage 企微接收到的原始消息文本
     * @return 解析结果
     */
    public WecomMessageParseOutput parse(String rawMessage) {
        WecomMessageParseOutput output = new WecomMessageParseOutput();
        List<String> matchedFields = new ArrayList<>();

        if (!StringUtils.hasText(rawMessage)) {
            output.setMatchedFields(matchedFields);
            output.setConfidence(0);
            return output;
        }

        String text = rawMessage.trim();

        // Step1: 按行切分，尝试提取标签-值对
        Map<String, String> labelValueMap = extractLabelValuePairs(text);

        // Step2: 将标签-值对映射到字段
        Map<String, String> fieldValueMap = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : labelValueMap.entrySet()) {
            String label = entry.getKey().toLowerCase().trim();
            String value = entry.getValue().trim();
            for (Map.Entry<String, String> aliasEntry : FIELD_ALIAS_MAP.entrySet()) {
                if (label.equals(aliasEntry.getKey().toLowerCase())) {
                    fieldValueMap.put(aliasEntry.getValue(), value);
                    break;
                }
            }
        }

        // Step3: 为没有明确标签的字段尝试正则提取
        if (!fieldValueMap.containsKey("merchantNo")) {
            String merchantNo = extractMerchantNo(text, labelValueMap);
            if (merchantNo != null) {
                fieldValueMap.put("merchantNo", merchantNo);
            }
        }

        // Step4: 提取图片URL
        if (!fieldValueMap.containsKey("problemScreenshot")) {
            String screenshots = extractImageUrls(text);
            if (screenshots != null) {
                fieldValueMap.put("problemScreenshot", screenshots);
            }
        }

        // Step5: 若无明确问题描述标签，将正文（去除已提取的结构化行）作为问题描述
        if (!fieldValueMap.containsKey("problemDesc")) {
            String inferredDesc = inferProblemDesc(text, labelValueMap);
            if (StringUtils.hasText(inferredDesc)) {
                fieldValueMap.put("problemDesc", inferredDesc);
            }
        }

        // Step6: 赋值到输出对象
        if (fieldValueMap.containsKey("merchantNo")) {
            output.setMerchantNo(fieldValueMap.get("merchantNo"));
            matchedFields.add("merchantNo");
        }
        if (fieldValueMap.containsKey("companyName")) {
            output.setCompanyName(fieldValueMap.get("companyName"));
            matchedFields.add("companyName");
        }
        if (fieldValueMap.containsKey("merchantAccount")) {
            output.setMerchantAccount(fieldValueMap.get("merchantAccount"));
            matchedFields.add("merchantAccount");
        }
        if (fieldValueMap.containsKey("sceneCode")) {
            output.setSceneCode(fieldValueMap.get("sceneCode"));
            matchedFields.add("sceneCode");
        }
        if (fieldValueMap.containsKey("problemDesc")) {
            output.setProblemDesc(fieldValueMap.get("problemDesc"));
            matchedFields.add("problemDesc");
        }
        if (fieldValueMap.containsKey("expectedResult")) {
            output.setExpectedResult(fieldValueMap.get("expectedResult"));
            matchedFields.add("expectedResult");
        }
        if (fieldValueMap.containsKey("problemScreenshot")) {
            output.setProblemScreenshot(fieldValueMap.get("problemScreenshot"));
            matchedFields.add("problemScreenshot");
        }

        output.setMatchedFields(matchedFields);
        output.setConfidence(calculateConfidence(matchedFields, labelValueMap.size()));
        return output;
    }

    /**
     * 从文本中提取标签-值对（支持中文冒号、英文冒号）
     * 每行形如 "字段名：字段值" 或 "字段名:字段值"
     */
    private Map<String, String> extractLabelValuePairs(String text) {
        Map<String, String> result = new LinkedHashMap<>();
        String[] lines = text.split("[\\r\\n]+");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            int colonIdx = findFirstColon(line);
            if (colonIdx > 0 && colonIdx < line.length() - 1) {
                String key = line.substring(0, colonIdx).trim();
                String value = line.substring(colonIdx + 1).trim();
                // 避免把整个URL或长句子解析成标签
                if (key.length() <= 20 && !key.contains(" ") && !key.startsWith("http")) {
                    result.put(key, value);
                }
            }
        }
        return result;
    }

    /**
     * 找到行中第一个冒号（中文：或英文:）的位置
     */
    private int findFirstColon(String line) {
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '：' || c == ':') {
                return i;
            }
        }
        return -1;
    }

    /**
     * 从文本中用正则提取商户编号
     * 仅在没有通过标签匹配到 merchantNo 的情况下使用
     * 排除已经作为标签值出现的数字串（避免重复）
     */
    private String extractMerchantNo(String text, Map<String, String> labelValueMap) {
        Matcher matcher = MERCHANT_NO_PATTERN.matcher(text);
        List<String> candidates = new ArrayList<>();
        while (matcher.find()) {
            String num = matcher.group(1);
            // 排除已被标签-值提取的数字
            boolean alreadyUsed = false;
            for (String v : labelValueMap.values()) {
                if (v.contains(num)) {
                    alreadyUsed = true;
                    break;
                }
            }
            if (!alreadyUsed) {
                candidates.add(num);
            }
        }
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    /**
     * 从文本中提取图片URL（http/https链接）
     */
    private String extractImageUrls(String text) {
        Matcher matcher = IMAGE_URL_PATTERN.matcher(text);
        List<String> urls = new ArrayList<>();
        while (matcher.find()) {
            urls.add(matcher.group(0));
        }
        if (urls.isEmpty()) {
            // fallback: 任意HTTP链接也可能是截图
            Matcher urlMatcher = URL_PATTERN.matcher(text);
            while (urlMatcher.find()) {
                String url = urlMatcher.group(0);
                if (!urls.contains(url)) {
                    urls.add(url);
                }
            }
        }
        return urls.isEmpty() ? null : String.join(",", urls);
    }

    /**
     * 推断问题描述：将消息中不属于结构化字段的正文内容作为问题描述
     */
    private String inferProblemDesc(String text, Map<String, String> labelValueMap) {
        if (labelValueMap.isEmpty()) {
            // 无任何结构化字段，整个文本作为问题描述
            String cleaned = URL_PATTERN.matcher(text).replaceAll("").trim();
            return cleaned.isEmpty() ? text.trim() : cleaned;
        }
        // 有结构化字段，但未找到"问题描述"标签，收集未被识别的行
        String[] lines = text.split("[\\r\\n]+");
        List<String> freeLinesBuffer = new ArrayList<>();
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            int colonIdx = findFirstColon(line);
            boolean isStructuredLine = false;
            if (colonIdx > 0 && colonIdx < line.length() - 1) {
                String key = line.substring(0, colonIdx).trim();
                if (key.length() <= 20 && !key.contains(" ") && !key.startsWith("http")) {
                    isStructuredLine = true;
                }
            }
            if (!isStructuredLine) {
                freeLinesBuffer.add(line);
            }
        }
        if (freeLinesBuffer.isEmpty()) {
            return null;
        }
        String freeText = String.join("\n", freeLinesBuffer).trim();
        // 去除URL
        freeText = URL_PATTERN.matcher(freeText).replaceAll("").trim();
        return freeText.isEmpty() ? null : freeText;
    }

    /**
     * 计算解析置信度
     */
    private int calculateConfidence(List<String> matchedFields, int structuredLineCount) {
        if (matchedFields.isEmpty()) {
            return 10;
        }
        // 有结构化行说明消息格式规整，置信度高
        int base = structuredLineCount > 0 ? 60 : 30;
        int bonus = matchedFields.size() * 8;
        return Math.min(100, base + bonus);
    }
}
