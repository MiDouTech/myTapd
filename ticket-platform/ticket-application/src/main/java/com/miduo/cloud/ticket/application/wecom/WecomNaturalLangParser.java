package com.miduo.cloud.ticket.application.wecom;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.common.constants.RedisKeyConstants;
import com.miduo.cloud.ticket.entity.dto.wecom.NlpAnalyzeResult;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.wecom.mapper.WecomNlpKeywordMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.wecom.po.WecomNlpKeywordPO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 企微自然语言消息解析器（基于规则引擎）
 * 接口编号：内部组件，非对外 API
 * Task023：企微文本消息自动创建工单
 */
@Component
public class WecomNaturalLangParser {

    private static final int MATCH_TYPE_CATEGORY = 1;
    private static final int MATCH_TYPE_PRIORITY = 2;

    private static final int CONFIDENCE_THRESHOLD = 70;

    private static final Pattern MERCHANT_NO_PATTERN = Pattern.compile("\\b(\\d{8,12})\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b(1[3-9]\\d{9})\\b");

    private final WecomNlpKeywordMapper nlpKeywordMapper;
    private final StringRedisTemplate redisTemplate;

    public WecomNaturalLangParser(WecomNlpKeywordMapper nlpKeywordMapper,
                                  StringRedisTemplate redisTemplate) {
        this.nlpKeywordMapper = nlpKeywordMapper;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 分析自然语言文本，提取分类、优先级和实体
     *
     * @param text                原始文本（已去除@提及前缀）
     * @param defaultCategoryPath 群默认分类路径（可空）
     * @return 解析结果
     */
    public NlpAnalyzeResult analyze(String text, String defaultCategoryPath) {
        NlpAnalyzeResult result = new NlpAnalyzeResult();
        result.setRawText(text);

        String title = text.length() > 50 ? text.substring(0, 50) : text;
        result.setTitle(title);

        List<WecomNlpKeywordPO> keywords = loadKeywords();

        List<WecomNlpKeywordPO> categoryKeywords = keywords.stream()
                .filter(k -> MATCH_TYPE_CATEGORY == k.getMatchType() && k.getIsActive() != null && k.getIsActive() == 1)
                .collect(Collectors.toList());

        List<WecomNlpKeywordPO> priorityKeywords = keywords.stream()
                .filter(k -> MATCH_TYPE_PRIORITY == k.getMatchType() && k.getIsActive() != null && k.getIsActive() == 1)
                .collect(Collectors.toList());

        String resolvedCategory = matchCategory(text, categoryKeywords, defaultCategoryPath);
        result.setCategoryPath(resolvedCategory);

        String resolvedPriority = matchPriority(text, priorityKeywords);
        result.setPriority(resolvedPriority != null ? resolvedPriority : "medium");

        Map<String, String> entities = extractEntities(text);
        result.setEntities(entities);

        int confidence = calculateConfidence(resolvedCategory, resolvedPriority, categoryKeywords, text);
        result.setConfidence(confidence);

        return result;
    }

    private String matchCategory(String text, List<WecomNlpKeywordPO> categoryKeywords, String defaultCategoryPath) {
        String bestCategory = null;
        int bestConfidence = 0;

        for (WecomNlpKeywordPO keyword : categoryKeywords) {
            if (keyword.getKeyword() == null || keyword.getKeyword().trim().isEmpty()) {
                continue;
            }
            if (text.contains(keyword.getKeyword())) {
                int conf = keyword.getConfidence() != null ? keyword.getConfidence() : 0;
                if (conf > bestConfidence) {
                    bestConfidence = conf;
                    bestCategory = keyword.getTargetValue();
                }
            }
        }

        if (bestConfidence >= CONFIDENCE_THRESHOLD) {
            return bestCategory;
        }
        return defaultCategoryPath;
    }

    private String matchPriority(String text, List<WecomNlpKeywordPO> priorityKeywords) {
        for (WecomNlpKeywordPO keyword : priorityKeywords) {
            if (keyword.getKeyword() == null || keyword.getKeyword().trim().isEmpty()) {
                continue;
            }
            if (text.contains(keyword.getKeyword())) {
                return keyword.getTargetValue();
            }
        }
        return null;
    }

    private Map<String, String> extractEntities(String text) {
        Map<String, String> entities = new HashMap<>();
        Matcher merchantMatcher = MERCHANT_NO_PATTERN.matcher(text);
        List<String> merchantNos = new ArrayList<>();
        while (merchantMatcher.find()) {
            merchantNos.add(merchantMatcher.group(1));
        }
        if (!merchantNos.isEmpty()) {
            entities.put("merchantNo", String.join(",", merchantNos));
        }

        Matcher phoneMatcher = PHONE_PATTERN.matcher(text);
        List<String> phones = new ArrayList<>();
        while (phoneMatcher.find()) {
            phones.add(phoneMatcher.group(1));
        }
        if (!phones.isEmpty()) {
            entities.put("phone", String.join(",", phones));
        }
        return entities;
    }

    private int calculateConfidence(String resolvedCategory, String resolvedPriority,
                                    List<WecomNlpKeywordPO> categoryKeywords, String text) {
        if (resolvedCategory == null) {
            return 30;
        }
        int maxConf = 0;
        for (WecomNlpKeywordPO keyword : categoryKeywords) {
            if (keyword.getKeyword() != null && text.contains(keyword.getKeyword())) {
                int conf = keyword.getConfidence() != null ? keyword.getConfidence() : 0;
                if (conf > maxConf) {
                    maxConf = conf;
                }
            }
        }
        if (resolvedPriority != null) {
            maxConf = Math.min(100, maxConf + 5);
        }
        return maxConf;
    }

    private List<WecomNlpKeywordPO> loadKeywords() {
        String cacheKey = RedisKeyConstants.WECOM_NLP_KEYWORDS_CACHE;
        Boolean hasKey = redisTemplate.hasKey(cacheKey);
        if (Boolean.TRUE.equals(hasKey)) {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null && !cached.isEmpty()) {
                try {
                    return com.alibaba.fastjson2.JSON.parseArray(cached, WecomNlpKeywordPO.class);
                } catch (Exception ignored) {
                }
            }
        }

        List<WecomNlpKeywordPO> keywords = nlpKeywordMapper.selectList(
                new LambdaQueryWrapper<WecomNlpKeywordPO>()
                        .eq(WecomNlpKeywordPO::getIsActive, 1)
                        .orderByDesc(WecomNlpKeywordPO::getSortOrder)
        );
        if (keywords == null) {
            keywords = new ArrayList<>();
        }

        redisTemplate.opsForValue().set(cacheKey,
                com.alibaba.fastjson2.JSON.toJSONString(keywords),
                5, TimeUnit.MINUTES);
        return keywords;
    }

    /**
     * 清除NLP关键词缓存
     */
    public void evictKeywordsCache() {
        redisTemplate.delete(RedisKeyConstants.WECOM_NLP_KEYWORDS_CACHE);
    }
}
