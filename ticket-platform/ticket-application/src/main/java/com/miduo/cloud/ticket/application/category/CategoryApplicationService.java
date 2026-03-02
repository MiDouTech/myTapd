package com.miduo.cloud.ticket.application.category;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.miduo.cloud.ticket.common.constants.AppConstants;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.entity.dto.category.CategoryCreateInput;
import com.miduo.cloud.ticket.entity.dto.category.CategoryDetailOutput;
import com.miduo.cloud.ticket.entity.dto.category.CategoryTreeOutput;
import com.miduo.cloud.ticket.entity.dto.category.CategoryUpdateInput;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.HandlerGroupMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketCategoryMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketTemplateMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.HandlerGroupPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketCategoryPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketTemplatePO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.mapper.SlaPolicyMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.mapper.WorkflowMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.po.SlaPolicyPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.po.WorkflowPO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CategoryApplicationService {

    @Resource
    private TicketCategoryMapper categoryMapper;

    @Resource
    private TicketTemplateMapper templateMapper;

    @Resource
    private WorkflowMapper workflowMapper;

    @Resource
    private SlaPolicyMapper slaPolicyMapper;

    @Resource
    private HandlerGroupMapper handlerGroupMapper;

    public List<CategoryTreeOutput> getCategoryTree() {
        List<TicketCategoryPO> allCategories = categoryMapper.selectList(
                new LambdaQueryWrapper<TicketCategoryPO>()
                        .orderByAsc(TicketCategoryPO::getLevel)
                        .orderByAsc(TicketCategoryPO::getSortOrder)
        );
        if (allCategories == null || allCategories.isEmpty()) {
            return Collections.emptyList();
        }
        List<CategoryTreeOutput> outputs = allCategories.stream()
                .map(this::convertToTreeOutput)
                .collect(Collectors.toList());
        return buildTree(outputs);
    }

    public CategoryDetailOutput getCategoryDetail(Long id) {
        TicketCategoryPO category = categoryMapper.selectById(id);
        if (category == null) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "分类不存在");
        }
        return convertToDetailOutput(category);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createCategory(CategoryCreateInput input) {
        validateCategoryLevel(input.getLevel(), input.getParentId());
        validateParentExists(input.getParentId());

        TicketCategoryPO po = new TicketCategoryPO();
        po.setName(input.getName());
        po.setParentId(input.getParentId());
        po.setLevel(input.getLevel());
        po.setTemplateId(input.getTemplateId());
        po.setWorkflowId(input.getWorkflowId());
        po.setSlaPolicyId(input.getSlaPolicyId());
        po.setDefaultGroupId(input.getDefaultGroupId());
        po.setSortOrder(input.getSortOrder() != null ? input.getSortOrder() : 0);
        po.setIsActive(1);

        categoryMapper.insert(po);

        String path = buildCategoryPath(po.getId(), input.getParentId());
        po.setPath(path);
        categoryMapper.updateById(po);

        return po.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateCategory(Long id, CategoryUpdateInput input) {
        TicketCategoryPO existing = categoryMapper.selectById(id);
        if (existing == null) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "分类不存在");
        }

        if (StringUtils.isNotBlank(input.getName())) {
            existing.setName(input.getName());
        }
        if (input.getTemplateId() != null) {
            existing.setTemplateId(input.getTemplateId());
        }
        if (input.getWorkflowId() != null) {
            existing.setWorkflowId(input.getWorkflowId());
        }
        if (input.getSlaPolicyId() != null) {
            existing.setSlaPolicyId(input.getSlaPolicyId());
        }
        if (input.getDefaultGroupId() != null) {
            existing.setDefaultGroupId(input.getDefaultGroupId());
        }
        if (input.getSortOrder() != null) {
            existing.setSortOrder(input.getSortOrder());
        }
        if (input.getIsActive() != null) {
            existing.setIsActive(input.getIsActive());
        }

        categoryMapper.updateById(existing);
    }

    private void validateCategoryLevel(Integer level, Long parentId) {
        if (level == null || level < 1 || level > AppConstants.MAX_CATEGORY_LEVEL) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR,
                    "分类层级必须在1到" + AppConstants.MAX_CATEGORY_LEVEL + "之间");
        }
        if (level == 1 && parentId != null) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "一级分类不能有父分类");
        }
        if (level > 1 && parentId == null) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "二级及以上分类必须指定父分类");
        }
    }

    private void validateParentExists(Long parentId) {
        if (parentId != null) {
            TicketCategoryPO parent = categoryMapper.selectById(parentId);
            if (parent == null) {
                throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "父分类不存在");
            }
        }
    }

    private String buildCategoryPath(Long id, Long parentId) {
        if (parentId == null) {
            return "/" + id + "/";
        }
        TicketCategoryPO parent = categoryMapper.selectById(parentId);
        if (parent == null || parent.getPath() == null) {
            return "/" + id + "/";
        }
        return parent.getPath() + id + "/";
    }

    private CategoryTreeOutput convertToTreeOutput(TicketCategoryPO po) {
        CategoryTreeOutput output = new CategoryTreeOutput();
        output.setId(po.getId());
        output.setName(po.getName());
        output.setParentId(po.getParentId());
        output.setLevel(po.getLevel());
        output.setPath(po.getPath());
        output.setTemplateId(po.getTemplateId());
        output.setWorkflowId(po.getWorkflowId());
        output.setSlaPolicyId(po.getSlaPolicyId());
        output.setDefaultGroupId(po.getDefaultGroupId());
        output.setSortOrder(po.getSortOrder());
        output.setIsActive(po.getIsActive());
        output.setChildren(new ArrayList<>());
        return output;
    }

    private List<CategoryTreeOutput> buildTree(List<CategoryTreeOutput> allNodes) {
        Map<Long, CategoryTreeOutput> nodeMap = allNodes.stream()
                .collect(Collectors.toMap(CategoryTreeOutput::getId, n -> n));

        List<CategoryTreeOutput> roots = new ArrayList<>();
        for (CategoryTreeOutput node : allNodes) {
            if (node.getParentId() == null) {
                roots.add(node);
            } else {
                CategoryTreeOutput parent = nodeMap.get(node.getParentId());
                if (parent != null) {
                    parent.getChildren().add(node);
                }
            }
        }
        return roots;
    }

    private CategoryDetailOutput convertToDetailOutput(TicketCategoryPO po) {
        CategoryDetailOutput output = new CategoryDetailOutput();
        output.setId(po.getId());
        output.setName(po.getName());
        output.setParentId(po.getParentId());
        output.setLevel(po.getLevel());
        output.setPath(po.getPath());
        output.setTemplateId(po.getTemplateId());
        output.setWorkflowId(po.getWorkflowId());
        output.setSlaPolicyId(po.getSlaPolicyId());
        output.setDefaultGroupId(po.getDefaultGroupId());
        output.setSortOrder(po.getSortOrder());
        output.setIsActive(po.getIsActive());
        output.setCreateTime(po.getCreateTime());
        output.setUpdateTime(po.getUpdateTime());

        if (po.getParentId() != null) {
            TicketCategoryPO parent = categoryMapper.selectById(po.getParentId());
            if (parent != null) {
                output.setParentName(parent.getName());
            }
        }
        if (po.getTemplateId() != null) {
            TicketTemplatePO template = templateMapper.selectById(po.getTemplateId());
            if (template != null) {
                output.setTemplateName(template.getName());
            }
        }
        if (po.getWorkflowId() != null) {
            WorkflowPO workflow = workflowMapper.selectById(po.getWorkflowId());
            if (workflow != null) {
                output.setWorkflowName(workflow.getName());
            }
        }
        if (po.getSlaPolicyId() != null) {
            SlaPolicyPO slaPolicy = slaPolicyMapper.selectById(po.getSlaPolicyId());
            if (slaPolicy != null) {
                output.setSlaPolicyName(slaPolicy.getName());
            }
        }
        if (po.getDefaultGroupId() != null) {
            HandlerGroupPO group = handlerGroupMapper.selectById(po.getDefaultGroupId());
            if (group != null) {
                output.setDefaultGroupName(group.getName());
            }
        }

        output.setFullPathName(buildFullPathName(po));

        return output;
    }

    private String buildFullPathName(TicketCategoryPO category) {
        List<String> names = new ArrayList<>();
        TicketCategoryPO current = category;
        while (current != null) {
            names.add(0, current.getName());
            if (current.getParentId() != null) {
                current = categoryMapper.selectById(current.getParentId());
            } else {
                current = null;
            }
        }
        return String.join(" > ", names);
    }
}
