package com.miduo.cloud.ticket.application.wecom;

import com.miduo.cloud.ticket.entity.dto.wecom.WecomMessageParseOutput;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 企微客服消息字段解析器（自然语言 → 客服信息字段）
 * 接口编号：API000504 内部组件
 * <p>
 * 解析策略（按优先级）：
 * 1. 标签-值对匹配：识别"商户编号：XXXXX"等结构化格式
 * 2. 自然语言语义提取：识别自然语句中内嵌的字段信息
 * 3. 正则提取：自动识别商户编号格式（6-12位数字）
 * 4. 图片URL提取：识别消息中的HTTP图片链接
 * 5. 上下文推断：无标签时将主体文本作为问题描述
 */
@Component
public class WecomMessageFieldParser {

    // 商户编号：6-12位纯数字，不紧邻其他数字
    private static final Pattern MERCHANT_NO_PATTERN = Pattern.compile("(?<![\\d])([1-9]\\d{5,11})(?![\\d])");
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s，,；;。！!？?\"']+");
    private static final Pattern IMAGE_URL_PATTERN = Pattern.compile(
            "https?://[^\\s，,；;。！!？?\"']+\\.(?:jpg|jpeg|png|gif|webp|bmp)(\\?[^\\s，,；;。！!？?]*)?",
            Pattern.CASE_INSENSITIVE
    );

    // 自然语言中提及商户编号的模式
    private static final Pattern NL_MERCHANT_NO_PATTERN = Pattern.compile(
            "(?:商户(?:编号|号|id|ID)?|mch_?id|merchant_?(?:no|id)|mchid)[^\u4e00-\u9fa50-9]*([1-9]\\d{5,11})"
    );

    // 自然语言中提及公司名称的模式（识别"XXX公司"、"XXX有限公司"等）
    private static final Pattern NL_COMPANY_PATTERN = Pattern.compile(
            "(?:公司|商户|商家|企业|客户)(?:名称|叫|是|为)?[\\s:：]*([^，,。！!？?\\n\\r]{2,30}(?:公司|集团|有限|科技|网络|信息|技术|贸易|实业|商贸|投资))"
    );

    // 自然语言中内嵌的公司名称（如"来自XX公司的商户反馈"）
    private static final Pattern NL_COMPANY_INLINE_PATTERN = Pattern.compile(
            "([^\\s，,。！!？?（(]{2,20}(?:公司|集团|有限公司|科技|网络|信息技术|贸易|实业|商贸))"
    );

    // 自然语言中提及商户账号的模式
    private static final Pattern NL_MERCHANT_ACCOUNT_PATTERN = Pattern.compile(
            "(?:账号|账户|登录账号|用户名|login)[^\u4e00-\u9fa50-9a-zA-Z@.]*([a-zA-Z0-9@._\\-]{3,50})"
    );

    // 自然语言中提及场景码的模式（字母数字混合）
    private static final Pattern NL_SCENE_CODE_PATTERN = Pattern.compile(
            "(?:场景码?|scene(?:_code)?|业务场景)[^\\w]*([A-Za-z0-9_\\-]{2,30})"
    );

    // 问题描述的自然语言触发词
    private static final List<String> PROBLEM_TRIGGER_WORDS = Arrays.asList(
            "反馈", "提示", "报错", "出现", "遇到", "发现", "无法", "不能", "失败", "错误",
            "问题", "故障", "异常", "显示", "提示", "点击", "跳转", "白屏", "闪退", "卡顿",
            "报：", "提示：", "说", "表示", "称", "描述"
    );

    // 预期结果的自然语言触发词
    private static final List<String> EXPECTED_TRIGGER_WORDS = Arrays.asList(
            "预期", "期望", "应该", "应当", "正常应", "正确应", "期望结果", "希望",
            "正常情况", "按道理", "本应", "理应"
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
        map.put("merchant_id", "merchantNo");
        map.put("商家编号", "merchantNo");
        map.put("商户id", "merchantNo");

        map.put("公司名称", "companyName");
        map.put("公司", "companyName");
        map.put("商户名称", "companyName");
        map.put("商户名", "companyName");
        map.put("商家名称", "companyName");
        map.put("商家名", "companyName");
        map.put("企业名称", "companyName");
        map.put("企业名", "companyName");
        map.put("客户名称", "companyName");
        map.put("客户名", "companyName");

        map.put("商户账号", "merchantAccount");
        map.put("账号", "merchantAccount");
        map.put("账户", "merchantAccount");
        map.put("登录账号", "merchantAccount");
        map.put("登录账户", "merchantAccount");
        map.put("用户名", "merchantAccount");
        map.put("登录名", "merchantAccount");

        map.put("场景码", "sceneCode");
        map.put("场景", "sceneCode");
        map.put("scene", "sceneCode");
        map.put("scene_code", "sceneCode");
        map.put("业务场景", "sceneCode");
        map.put("场景编码", "sceneCode");

        map.put("问题描述", "problemDesc");
        map.put("问题", "problemDesc");
        map.put("描述", "problemDesc");
        map.put("故障描述", "problemDesc");
        map.put("bug描述", "problemDesc");
        map.put("现象", "problemDesc");
        map.put("现象描述", "problemDesc");
        map.put("反馈内容", "problemDesc");
        map.put("反馈", "problemDesc");
        map.put("故障", "problemDesc");
        map.put("异常描述", "problemDesc");
        map.put("错误描述", "problemDesc");
        map.put("异常", "problemDesc");

        map.put("预期结果", "expectedResult");
        map.put("期望结果", "expectedResult");
        map.put("预期", "expectedResult");
        map.put("期望", "expectedResult");
        map.put("期望行为", "expectedResult");
        map.put("正确行为", "expectedResult");
        map.put("正常行为", "expectedResult");
        map.put("预期行为", "expectedResult");

        map.put("截图", "problemScreenshot");
        map.put("问题截图", "problemScreenshot");
        map.put("图片", "problemScreenshot");
        map.put("附图", "problemScreenshot");
        map.put("截屏", "problemScreenshot");
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

        // Step3: 自然语言提取（针对结构化标签未覆盖的字段）
        extractFromNaturalLanguage(text, fieldValueMap, labelValueMap);

        // Step4: 为没有明确标签的字段尝试正则提取商户编号
        if (!fieldValueMap.containsKey("merchantNo")) {
            String merchantNo = extractMerchantNo(text, labelValueMap);
            if (merchantNo != null) {
                fieldValueMap.put("merchantNo", merchantNo);
            }
        }

        // Step5: 提取图片URL
        if (!fieldValueMap.containsKey("problemScreenshot")) {
            String screenshots = extractImageUrls(text);
            if (screenshots != null) {
                fieldValueMap.put("problemScreenshot", screenshots);
            }
        }

        // Step6: 若无明确问题描述标签，将正文（去除已提取的结构化行）作为问题描述
        if (!fieldValueMap.containsKey("problemDesc")) {
            String inferredDesc = inferProblemDesc(text, labelValueMap);
            if (StringUtils.hasText(inferredDesc)) {
                fieldValueMap.put("problemDesc", inferredDesc);
            }
        }

        // Step7: 赋值到输出对象
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
        output.setConfidence(calculateConfidence(matchedFields, labelValueMap.size(), text));
        return output;
    }

    /**
     * 自然语言提取：从自由文本中识别各字段信息
     * 对于未通过结构化标签匹配到的字段，尝试用语义模式提取
     */
    private void extractFromNaturalLanguage(String text, Map<String, String> fieldValueMap,
                                             Map<String, String> labelValueMap) {
        // 提取商户编号（带上下文语义）
        if (!fieldValueMap.containsKey("merchantNo")) {
            String merchantNo = extractNlMerchantNo(text);
            if (merchantNo != null) {
                fieldValueMap.put("merchantNo", merchantNo);
            }
        }

        // 提取公司名称（带上下文语义）
        if (!fieldValueMap.containsKey("companyName")) {
            String companyName = extractNlCompanyName(text);
            if (companyName != null) {
                fieldValueMap.put("companyName", companyName);
            }
        }

        // 提取商户账号（带上下文语义）
        if (!fieldValueMap.containsKey("merchantAccount")) {
            String account = extractNlMerchantAccount(text);
            if (account != null) {
                fieldValueMap.put("merchantAccount", account);
            }
        }

        // 提取场景码（带上下文语义）
        if (!fieldValueMap.containsKey("sceneCode")) {
            String sceneCode = extractNlSceneCode(text);
            if (sceneCode != null) {
                fieldValueMap.put("sceneCode", sceneCode);
            }
        }

        // 提取问题描述（从自然语言句子中识别问题描述段落）
        if (!fieldValueMap.containsKey("problemDesc")) {
            String problemDesc = extractNlProblemDesc(text, labelValueMap);
            if (StringUtils.hasText(problemDesc)) {
                fieldValueMap.put("problemDesc", problemDesc);
            }
        }

        // 提取预期结果（从自然语言句子中识别预期结果段落）
        if (!fieldValueMap.containsKey("expectedResult")) {
            String expectedResult = extractNlExpectedResult(text, labelValueMap);
            if (StringUtils.hasText(expectedResult)) {
                fieldValueMap.put("expectedResult", expectedResult);
            }
        }
    }

    /**
     * 从自然语言文本中提取商户编号（带语义上下文）
     */
    private String extractNlMerchantNo(String text) {
        Matcher matcher = NL_MERCHANT_NO_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 从自然语言文本中提取公司名称
     */
    private String extractNlCompanyName(String text) {
        // 优先匹配"公司名称是XXX"等显式模式
        Matcher matcher = NL_COMPANY_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        // 其次匹配内嵌的公司名称（如"来自山东英贝健生物技术有限公司的商户"）
        Matcher inlineMatcher = NL_COMPANY_INLINE_PATTERN.matcher(text);
        List<String> candidates = new ArrayList<>();
        while (inlineMatcher.find()) {
            String candidate = inlineMatcher.group(1).trim();
            // 过滤掉太短或者是通用词的候选
            if (candidate.length() >= 4 && !isGenericCompanyWord(candidate)) {
                candidates.add(candidate);
            }
        }
        // 优先选取最长的公司名（更具体）
        return candidates.stream()
                .max(Comparator.comparingInt(String::length))
                .orElse(null);
    }

    /**
     * 判断是否是通用公司词（不应单独作为公司名）
     */
    private boolean isGenericCompanyWord(String word) {
        Set<String> generic = new HashSet<>(Arrays.asList(
                "有限公司", "股份公司", "集团公司", "科技公司", "网络公司"
        ));
        return generic.contains(word);
    }

    /**
     * 从自然语言文本中提取商户账号
     */
    private String extractNlMerchantAccount(String text) {
        Matcher matcher = NL_MERCHANT_ACCOUNT_PATTERN.matcher(text);
        if (matcher.find()) {
            String account = matcher.group(1).trim();
            // 账号不应该是纯数字（避免与商户编号混淆），除非较短
            if (!account.matches("\\d{6,}")) {
                return account;
            }
        }
        return null;
    }

    /**
     * 从自然语言文本中提取场景码
     */
    private String extractNlSceneCode(String text) {
        Matcher matcher = NL_SCENE_CODE_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * 从自然语言文本中提取问题描述
     * 识别触发词后的句子作为问题描述
     */
    private String extractNlProblemDesc(String text, Map<String, String> labelValueMap) {
        // 已有结构化标签的文本不需要自然语言提取
        if (!labelValueMap.isEmpty() && labelValueMap.size() >= 3) {
            return null;
        }

        String[] sentences = splitIntoSentences(text);
        List<String> problemSentences = new ArrayList<>();

        for (String sentence : sentences) {
            if (sentence.trim().isEmpty()) {
                continue;
            }
            // 跳过已被标签匹配的行
            if (isStructuredLine(sentence)) {
                continue;
            }
            // 检查是否包含问题触发词
            boolean hasProblemTrigger = PROBLEM_TRIGGER_WORDS.stream()
                    .anyMatch(sentence::contains);
            boolean hasExpectedTrigger = EXPECTED_TRIGGER_WORDS.stream()
                    .anyMatch(sentence::contains);

            // 问题描述行：包含问题触发词但不包含预期触发词
            if (hasProblemTrigger && !hasExpectedTrigger) {
                // 去除URL
                String cleaned = URL_PATTERN.matcher(sentence.trim()).replaceAll("").trim();
                if (!cleaned.isEmpty()) {
                    problemSentences.add(cleaned);
                }
            }
        }

        if (problemSentences.isEmpty()) {
            return null;
        }
        return String.join("；", problemSentences);
    }

    /**
     * 从自然语言文本中提取预期结果
     */
    private String extractNlExpectedResult(String text, Map<String, String> labelValueMap) {
        String[] sentences = splitIntoSentences(text);
        List<String> expectedSentences = new ArrayList<>();

        for (String sentence : sentences) {
            if (sentence.trim().isEmpty()) {
                continue;
            }
            if (isStructuredLine(sentence)) {
                continue;
            }
            boolean hasExpectedTrigger = EXPECTED_TRIGGER_WORDS.stream()
                    .anyMatch(sentence::contains);
            if (hasExpectedTrigger) {
                String cleaned = URL_PATTERN.matcher(sentence.trim()).replaceAll("").trim();
                if (!cleaned.isEmpty()) {
                    expectedSentences.add(cleaned);
                }
            }
        }

        if (expectedSentences.isEmpty()) {
            return null;
        }
        return String.join("；", expectedSentences);
    }

    /**
     * 将文本切分成句子（按中英文标点和换行）
     */
    private String[] splitIntoSentences(String text) {
        return text.split("[\\r\\n。！!？?；;]+");
    }

    /**
     * 判断一行是否是结构化标签行（"字段名：字段值" 格式）
     */
    private boolean isStructuredLine(String line) {
        int colonIdx = findFirstColon(line);
        if (colonIdx > 0 && colonIdx < line.length() - 1) {
            String key = line.substring(0, colonIdx).trim();
            return key.length() <= 20 && !key.contains(" ") && !key.startsWith("http");
        }
        return false;
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
            if (!isStructuredLine(line)) {
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
    private int calculateConfidence(List<String> matchedFields, int structuredLineCount, String text) {
        if (matchedFields.isEmpty()) {
            return 10;
        }
        // 有结构化行说明消息格式规整，置信度高
        int base = structuredLineCount > 0 ? 60 : 30;
        int bonus = matchedFields.size() * 8;
        // 匹配了关键字段（商户编号、公司名称）额外加分
        if (matchedFields.contains("merchantNo")) {
            bonus += 5;
        }
        if (matchedFields.contains("companyName")) {
            bonus += 3;
        }
        if (matchedFields.contains("problemDesc")) {
            bonus += 3;
        }
        return Math.min(100, base + bonus);
    }
}
