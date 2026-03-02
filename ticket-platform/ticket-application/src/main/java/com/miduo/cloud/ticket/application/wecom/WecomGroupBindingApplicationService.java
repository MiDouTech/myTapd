package com.miduo.cloud.ticket.application.wecom;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.entity.dto.wecom.WecomGroupBindingCreateInput;
import com.miduo.cloud.ticket.entity.dto.wecom.WecomGroupBindingListOutput;
import com.miduo.cloud.ticket.entity.dto.wecom.WecomGroupBindingUpdateInput;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketCategoryMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketCategoryPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.wecom.mapper.WecomGroupBindingMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.wecom.po.WecomGroupBindingPO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 企微群绑定应用服务
 */
@Service
public class WecomGroupBindingApplicationService extends BaseApplicationService {

    private final WecomGroupBindingMapper wecomGroupBindingMapper;
    private final TicketCategoryMapper ticketCategoryMapper;

    public WecomGroupBindingApplicationService(WecomGroupBindingMapper wecomGroupBindingMapper,
                                               TicketCategoryMapper ticketCategoryMapper) {
        this.wecomGroupBindingMapper = wecomGroupBindingMapper;
        this.ticketCategoryMapper = ticketCategoryMapper;
    }

    /**
     * 查询群绑定配置列表
     */
    public List<WecomGroupBindingListOutput> listBindings() {
        List<WecomGroupBindingPO> bindings = wecomGroupBindingMapper.selectList(
                new LambdaQueryWrapper<WecomGroupBindingPO>()
                        .orderByDesc(WecomGroupBindingPO::getCreateTime)
        );
        if (bindings == null || bindings.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> categoryIds = bindings.stream()
                .map(WecomGroupBindingPO::getDefaultCategoryId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        Map<Long, String> categoryNameMap = Collections.emptyMap();
        if (!categoryIds.isEmpty()) {
            List<TicketCategoryPO> categories = ticketCategoryMapper.selectBatchIds(categoryIds);
            categoryNameMap = categories.stream()
                    .collect(Collectors.toMap(TicketCategoryPO::getId, TicketCategoryPO::getName));
        }

        Map<Long, String> finalCategoryNameMap = categoryNameMap;
        List<WecomGroupBindingListOutput> result = new ArrayList<>();
        for (WecomGroupBindingPO binding : bindings) {
            WecomGroupBindingListOutput output = new WecomGroupBindingListOutput();
            output.setId(binding.getId());
            output.setChatId(binding.getChatId());
            output.setChatName(binding.getChatName());
            output.setDefaultCategoryId(binding.getDefaultCategoryId());
            output.setDefaultCategoryName(finalCategoryNameMap.get(binding.getDefaultCategoryId()));
            output.setWebhookUrl(binding.getWebhookUrl());
            output.setIsActive(binding.getIsActive());
            output.setCreateTime(binding.getCreateTime());
            output.setUpdateTime(binding.getUpdateTime());
            result.add(output);
        }
        return result;
    }

    /**
     * 新增群绑定配置
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createBinding(WecomGroupBindingCreateInput input) {
        validateDefaultCategory(input.getDefaultCategoryId());
        validateChatIdUnique(input.getChatId(), null);

        WecomGroupBindingPO po = new WecomGroupBindingPO();
        po.setChatId(input.getChatId().trim());
        po.setChatName(input.getChatName());
        po.setDefaultCategoryId(input.getDefaultCategoryId());
        po.setWebhookUrl(input.getWebhookUrl());
        po.setIsActive(input.getIsActive() == null ? 1 : input.getIsActive());
        wecomGroupBindingMapper.insert(po);

        log.info("企微群绑定创建成功: id={}, chatId={}", po.getId(), po.getChatId());
        return po.getId();
    }

    /**
     * 更新群绑定配置
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateBinding(Long id, WecomGroupBindingUpdateInput input) {
        if (id == null || id <= 0) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "绑定ID不能为空");
        }

        WecomGroupBindingPO existing = wecomGroupBindingMapper.selectById(id);
        if (existing == null) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "群绑定配置不存在");
        }

        validateDefaultCategory(input.getDefaultCategoryId());

        if (input.getChatName() != null) {
            existing.setChatName(input.getChatName().trim());
        }
        if (input.getDefaultCategoryId() != null) {
            existing.setDefaultCategoryId(input.getDefaultCategoryId());
        }
        if (input.getWebhookUrl() != null) {
            existing.setWebhookUrl(input.getWebhookUrl().trim());
        }
        if (input.getIsActive() != null) {
            existing.setIsActive(input.getIsActive());
        }
        wecomGroupBindingMapper.updateById(existing);

        log.info("企微群绑定更新成功: id={}", id);
    }

    private void validateDefaultCategory(Long defaultCategoryId) {
        if (defaultCategoryId == null) {
            return;
        }
        TicketCategoryPO category = ticketCategoryMapper.selectById(defaultCategoryId);
        if (category == null) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "默认分类不存在");
        }
    }

    private void validateChatIdUnique(String chatId, Long excludeId) {
        if (chatId == null || chatId.trim().isEmpty()) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "群ChatID不能为空");
        }
        LambdaQueryWrapper<WecomGroupBindingPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WecomGroupBindingPO::getChatId, chatId.trim());
        if (excludeId != null) {
            wrapper.ne(WecomGroupBindingPO::getId, excludeId);
        }
        Long count = wecomGroupBindingMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw BusinessException.of(ErrorCode.DATA_ALREADY_EXISTS, "群ChatID已绑定");
        }
    }
}
