package com.miduo.cloud.ticket.application.dashboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.common.constants.DashboardLayoutConstants;
import com.miduo.cloud.ticket.common.enums.DashboardRowGroupEnum;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.security.SecurityUtil;
import com.miduo.cloud.ticket.entity.dto.dashboard.DashboardLayoutItemOutput;
import com.miduo.cloud.ticket.entity.dto.dashboard.DashboardLayoutSaveInput;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.dashboard.mapper.DashboardUserLayoutMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.dashboard.po.DashboardUserLayoutPO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 仪表盘个人布局业务服务
 * 提供用户个人仪表盘行组排序配置的读写逻辑
 */
@Service
public class DashboardLayoutApplicationService extends BaseApplicationService {

    private final DashboardUserLayoutMapper layoutMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public DashboardLayoutApplicationService(DashboardUserLayoutMapper layoutMapper,
                                             StringRedisTemplate redisTemplate,
                                             ObjectMapper objectMapper) {
        this.layoutMapper = layoutMapper;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 获取用户个人布局配置
     * 优先从 Redis 缓存获取，未命中则查数据库，空记录返回系统默认布局
     *
     * @param userId 用户ID
     * @return 布局配置列表（按 sortOrder 升序）
     */
    public List<DashboardLayoutItemOutput> getLayout(Long userId) {
        if (userId == null) {
            return buildDefaultLayout();
        }
        String cacheKey = DashboardLayoutConstants.LAYOUT_CACHE_KEY_PREFIX + userId;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                List<DashboardLayoutItemOutput> result = objectMapper.readValue(cached,
                        new TypeReference<List<DashboardLayoutItemOutput>>() {});
                if (!CollectionUtils.isEmpty(result)) {
                    return result;
                }
            } catch (JsonProcessingException e) {
                log.warn("仪表盘布局缓存反序列化失败，将重新查询DB: userId={}", userId);
            }
        }

        List<DashboardUserLayoutPO> poList = layoutMapper.selectByUserId(userId);
        List<DashboardLayoutItemOutput> result;
        if (CollectionUtils.isEmpty(poList)) {
            result = buildDefaultLayout();
        } else {
            result = convertToOutputList(poList);
        }

        cacheLayout(cacheKey, result);
        return result;
    }

    /**
     * 保存用户个人布局配置
     * 事务内：软删旧记录 → 批量插入新记录；事务后：清除 Redis 缓存
     *
     * @param userId 用户ID
     * @param input  保存请求
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveLayout(Long userId, DashboardLayoutSaveInput input) {
        if (userId == null) {
            throw BusinessException.of(ErrorCode.UNAUTHORIZED);
        }
        if (input == null || CollectionUtils.isEmpty(input.getLayouts())) {
            throw BusinessException.of(ErrorCode.BAD_REQUEST, "布局列表不能为空");
        }

        for (DashboardLayoutSaveInput.LayoutItem item : input.getLayouts()) {
            DashboardRowGroupEnum.fromKey(item.getRowGroupKey());
        }

        String currentUser = SecurityUtil.getCurrentUsername();

        layoutMapper.softDeleteByUserId(userId, currentUser);

        List<DashboardUserLayoutPO> newRecords = new ArrayList<>();
        for (DashboardLayoutSaveInput.LayoutItem item : input.getLayouts()) {
            DashboardRowGroupEnum rowGroup = DashboardRowGroupEnum.fromKey(item.getRowGroupKey());
            DashboardUserLayoutPO po = new DashboardUserLayoutPO();
            po.setUserId(userId);
            po.setRowGroupKey(rowGroup.getKey());
            po.setIsFixed(rowGroup.isFixed() ? 1 : 0);
            if (rowGroup == DashboardRowGroupEnum.OVERVIEW) {
                po.setSortOrder(0);
            } else {
                po.setSortOrder(item.getSortOrder());
            }
            po.setCreateBy(currentUser);
            po.setUpdateBy(currentUser);
            newRecords.add(po);
        }

        layoutMapper.batchInsert(newRecords);

        String cacheKey = DashboardLayoutConstants.LAYOUT_CACHE_KEY_PREFIX + userId;
        redisTemplate.delete(cacheKey);
        log.info("仪表盘布局已保存: userId={}, count={}", userId, newRecords.size());
    }

    /**
     * 恢复用户仪表盘为系统默认布局
     * 软删除现有记录，清除 Redis 缓存
     *
     * @param userId 用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void resetLayout(Long userId) {
        if (userId == null) {
            throw BusinessException.of(ErrorCode.UNAUTHORIZED);
        }
        String currentUser = SecurityUtil.getCurrentUsername();
        layoutMapper.softDeleteByUserId(userId, currentUser);

        String cacheKey = DashboardLayoutConstants.LAYOUT_CACHE_KEY_PREFIX + userId;
        redisTemplate.delete(cacheKey);
        log.info("仪表盘布局已恢复默认: userId={}", userId);
    }

    private List<DashboardLayoutItemOutput> buildDefaultLayout() {
        List<DashboardLayoutItemOutput> result = new ArrayList<>();
        for (DashboardLayoutConstants.DashboardRowGroupEntry entry : DashboardLayoutConstants.DEFAULT_LAYOUT) {
            DashboardLayoutItemOutput item = new DashboardLayoutItemOutput();
            item.setRowGroupKey(entry.getRowGroupKey());
            item.setSortOrder(entry.getSortOrder());
            item.setIsFixed(entry.isFixed());
            result.add(item);
        }
        return result;
    }

    private List<DashboardLayoutItemOutput> convertToOutputList(List<DashboardUserLayoutPO> poList) {
        List<DashboardLayoutItemOutput> result = new ArrayList<>();
        for (DashboardUserLayoutPO po : poList) {
            DashboardLayoutItemOutput item = new DashboardLayoutItemOutput();
            item.setRowGroupKey(po.getRowGroupKey());
            item.setSortOrder(po.getSortOrder());
            item.setIsFixed(po.getIsFixed() != null && po.getIsFixed() == 1);
            result.add(item);
        }
        result.sort(Comparator.comparingInt(DashboardLayoutItemOutput::getSortOrder));
        return result;
    }

    private void cacheLayout(String cacheKey, List<DashboardLayoutItemOutput> result) {
        try {
            String json = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(cacheKey, json,
                    DashboardLayoutConstants.LAYOUT_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            log.warn("仪表盘布局缓存写入失败: {}", e.getMessage());
        }
    }
}
