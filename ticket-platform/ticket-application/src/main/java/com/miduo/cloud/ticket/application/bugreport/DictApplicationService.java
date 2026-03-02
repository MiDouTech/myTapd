package com.miduo.cloud.ticket.application.bugreport;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.entity.dto.bugreport.DefectCategoryOutput;
import com.miduo.cloud.ticket.entity.dto.bugreport.LogicCauseTreeOutput;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.bugreport.mapper.DictDefectCategoryMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.bugreport.mapper.DictLogicCauseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.bugreport.po.DictDefectCategoryPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.bugreport.po.DictLogicCausePO;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Bug简报字典服务
 */
@Service
public class DictApplicationService extends BaseApplicationService {

    private final DictLogicCauseMapper dictLogicCauseMapper;
    private final DictDefectCategoryMapper dictDefectCategoryMapper;

    public DictApplicationService(DictLogicCauseMapper dictLogicCauseMapper,
                                  DictDefectCategoryMapper dictDefectCategoryMapper) {
        this.dictLogicCauseMapper = dictLogicCauseMapper;
        this.dictDefectCategoryMapper = dictDefectCategoryMapper;
    }

    public List<LogicCauseTreeOutput> getLogicCauseTree() {
        List<DictLogicCausePO> list = dictLogicCauseMapper.selectList(
                new LambdaQueryWrapper<DictLogicCausePO>()
                        .eq(DictLogicCausePO::getIsActive, 1)
                        .orderByAsc(DictLogicCausePO::getLevel)
                        .orderByAsc(DictLogicCausePO::getSortOrder)
                        .orderByAsc(DictLogicCausePO::getId)
        );
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }

        Map<Long, List<DictLogicCausePO>> childrenMap = list.stream()
                .filter(item -> item.getParentId() != null)
                .collect(Collectors.groupingBy(DictLogicCausePO::getParentId));

        List<LogicCauseTreeOutput> roots = new ArrayList<>();
        for (DictLogicCausePO node : list) {
            if (node.getLevel() == null || node.getLevel() != 1) {
                continue;
            }
            LogicCauseTreeOutput root = new LogicCauseTreeOutput();
            root.setId(node.getId());
            root.setName(node.getName());
            List<DictLogicCausePO> secondLevel = childrenMap.get(node.getId());
            if (!CollectionUtils.isEmpty(secondLevel)) {
                root.setChildren(secondLevel.stream().map(child -> {
                    LogicCauseTreeOutput sub = new LogicCauseTreeOutput();
                    sub.setId(child.getId());
                    sub.setName(child.getName());
                    sub.setChildren(Collections.emptyList());
                    return sub;
                }).collect(Collectors.toList()));
            } else {
                root.setChildren(Collections.emptyList());
            }
            roots.add(root);
        }
        return roots;
    }

    public List<DefectCategoryOutput> getDefectCategories() {
        List<DictDefectCategoryPO> list = dictDefectCategoryMapper.selectList(
                new LambdaQueryWrapper<DictDefectCategoryPO>()
                        .eq(DictDefectCategoryPO::getIsActive, 1)
                        .orderByAsc(DictDefectCategoryPO::getSortOrder)
                        .orderByAsc(DictDefectCategoryPO::getId)
        );
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        return list.stream().map(item -> {
            DefectCategoryOutput output = new DefectCategoryOutput();
            output.setId(item.getId());
            output.setName(item.getName());
            output.setDescription(item.getDescription());
            return output;
        }).collect(Collectors.toList());
    }
}
