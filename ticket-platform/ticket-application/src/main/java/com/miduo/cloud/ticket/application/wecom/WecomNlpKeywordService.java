package com.miduo.cloud.ticket.application.wecom;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.common.enums.NlpMatchType;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.entity.dto.wecom.NlpKeywordCreateInput;
import com.miduo.cloud.ticket.entity.dto.wecom.NlpKeywordListOutput;
import com.miduo.cloud.ticket.entity.dto.wecom.NlpKeywordUpdateInput;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.wecom.mapper.WecomNlpKeywordMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.wecom.po.WecomNlpKeywordPO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 企微NLP关键词配置服务
 * Task023：企微文本消息自动创建工单 - 关键词管理
 */
@Service
public class WecomNlpKeywordService {

    private final WecomNlpKeywordMapper nlpKeywordMapper;
    private final WecomNaturalLangParser naturalLangParser;

    public WecomNlpKeywordService(WecomNlpKeywordMapper nlpKeywordMapper,
                                   WecomNaturalLangParser naturalLangParser) {
        this.nlpKeywordMapper = nlpKeywordMapper;
        this.naturalLangParser = naturalLangParser;
    }

    /**
     * 查询NLP关键词列表（API000432）
     *
     * @param matchType 匹配类型（可为空）
     * @return 关键词列表
     */
    public List<NlpKeywordListOutput> listKeywords(Integer matchType) {
        LambdaQueryWrapper<WecomNlpKeywordPO> wrapper = new LambdaQueryWrapper<>();
        if (matchType != null) {
            wrapper.eq(WecomNlpKeywordPO::getMatchType, matchType);
        }
        wrapper.orderByDesc(WecomNlpKeywordPO::getSortOrder)
                .orderByAsc(WecomNlpKeywordPO::getId);
        List<WecomNlpKeywordPO> list = nlpKeywordMapper.selectList(wrapper);
        return list.stream().map(this::toOutput).collect(Collectors.toList());
    }

    /**
     * 创建NLP关键词（API000433）
     *
     * @param input 创建请求
     * @return 新创建的关键词ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createKeyword(NlpKeywordCreateInput input) {
        validateMatchType(input.getMatchType());
        WecomNlpKeywordPO po = new WecomNlpKeywordPO();
        po.setKeyword(input.getKeyword().trim());
        po.setMatchType(input.getMatchType());
        po.setTargetValue(input.getTargetValue().trim());
        po.setConfidence(input.getConfidence());
        po.setSortOrder(input.getSortOrder());
        po.setIsActive(input.getIsActive());
        nlpKeywordMapper.insert(po);
        naturalLangParser.evictKeywordsCache();
        return po.getId();
    }

    /**
     * 更新NLP关键词（API000434）
     *
     * @param id    关键词ID
     * @param input 更新请求
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateKeyword(Long id, NlpKeywordUpdateInput input) {
        WecomNlpKeywordPO existing = nlpKeywordMapper.selectById(id);
        if (existing == null) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "关键词不存在：id=" + id);
        }
        validateMatchType(input.getMatchType());
        existing.setKeyword(input.getKeyword().trim());
        existing.setMatchType(input.getMatchType());
        existing.setTargetValue(input.getTargetValue().trim());
        existing.setConfidence(input.getConfidence());
        existing.setSortOrder(input.getSortOrder());
        existing.setIsActive(input.getIsActive());
        nlpKeywordMapper.updateById(existing);
        naturalLangParser.evictKeywordsCache();
    }

    /**
     * 删除NLP关键词（逻辑删除）（API000435）
     *
     * @param id 关键词ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteKeyword(Long id) {
        WecomNlpKeywordPO existing = nlpKeywordMapper.selectById(id);
        if (existing == null) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "关键词不存在：id=" + id);
        }
        nlpKeywordMapper.deleteById(id);
        naturalLangParser.evictKeywordsCache();
    }

    private NlpKeywordListOutput toOutput(WecomNlpKeywordPO po) {
        NlpKeywordListOutput output = new NlpKeywordListOutput();
        output.setId(po.getId());
        output.setKeyword(po.getKeyword());
        output.setMatchType(po.getMatchType());
        output.setMatchTypeLabel(resolveMatchTypeLabel(po.getMatchType()));
        output.setTargetValue(po.getTargetValue());
        output.setConfidence(po.getConfidence());
        output.setSortOrder(po.getSortOrder());
        output.setIsActive(po.getIsActive());
        output.setCreateTime(po.getCreateTime());
        output.setUpdateTime(po.getUpdateTime());
        return output;
    }

    private String resolveMatchTypeLabel(Integer matchType) {
        if (matchType == null) {
            return "-";
        }
        NlpMatchType type = NlpMatchType.fromCode(matchType);
        return type != null ? type.getLabel() : String.valueOf(matchType);
    }

    private void validateMatchType(Integer matchType) {
        if (matchType == null) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "匹配类型不能为空");
        }
        if (NlpMatchType.fromCode(matchType) == null) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "无效的匹配类型：" + matchType);
        }
    }
}
