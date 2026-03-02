package com.miduo.cloud.ticket.application.template;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.entity.dto.template.TemplateListOutput;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketCategoryMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketTemplateMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketCategoryPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketTemplatePO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TemplateApplicationService {

    @Resource
    private TicketTemplateMapper templateMapper;

    @Resource
    private TicketCategoryMapper categoryMapper;

    public List<TemplateListOutput> getTemplateList(Long categoryId) {
        LambdaQueryWrapper<TicketTemplatePO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TicketTemplatePO::getIsActive, 1);
        if (categoryId != null) {
            queryWrapper.eq(TicketTemplatePO::getCategoryId, categoryId);
        }
        queryWrapper.orderByDesc(TicketTemplatePO::getCreateTime);

        List<TicketTemplatePO> templates = templateMapper.selectList(queryWrapper);
        if (templates == null || templates.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> categoryIds = templates.stream()
                .map(TicketTemplatePO::getCategoryId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        Map<Long, String> categoryNameMap = Collections.emptyMap();
        if (!categoryIds.isEmpty()) {
            List<TicketCategoryPO> categories = categoryMapper.selectBatchIds(categoryIds);
            categoryNameMap = categories.stream()
                    .collect(Collectors.toMap(TicketCategoryPO::getId, TicketCategoryPO::getName));
        }

        Map<Long, String> finalCategoryNameMap = categoryNameMap;
        return templates.stream().map(po -> {
            TemplateListOutput output = new TemplateListOutput();
            output.setId(po.getId());
            output.setName(po.getName());
            output.setCategoryId(po.getCategoryId());
            output.setCategoryName(finalCategoryNameMap.get(po.getCategoryId()));
            output.setFieldsConfig(po.getFieldsConfig());
            output.setDescription(po.getDescription());
            output.setIsActive(po.getIsActive());
            output.setCreateTime(po.getCreateTime());
            return output;
        }).collect(Collectors.toList());
    }
}
