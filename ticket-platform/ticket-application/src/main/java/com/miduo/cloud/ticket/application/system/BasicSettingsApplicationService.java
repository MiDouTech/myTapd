package com.miduo.cloud.ticket.application.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.entity.dto.system.BasicSettingsOutput;
import com.miduo.cloud.ticket.entity.dto.system.BasicSettingsUpdateInput;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.system.mapper.SystemConfigMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.system.po.SystemConfigPO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基础参数配置应用服务
 */
@Service
public class BasicSettingsApplicationService {

    private static final String CONFIG_GROUP_BASIC = "BASIC";
    private static final String CONFIG_GROUP_WORKING_TIME = "WORKING_TIME";

    private static final String KEY_SYSTEM_NAME = "system_name";
    private static final String KEY_TIMEZONE = "timezone";
    private static final String KEY_DEFAULT_PAGE_SIZE = "default_page_size";
    private static final String KEY_WORKING_TIME_START = "working_time_start";
    private static final String KEY_WORKING_TIME_END = "working_time_end";

    private final SystemConfigMapper systemConfigMapper;

    public BasicSettingsApplicationService(SystemConfigMapper systemConfigMapper) {
        this.systemConfigMapper = systemConfigMapper;
    }

    public BasicSettingsOutput getBasicSettings() {
        Map<String, String> basicMap = loadConfigByGroup(CONFIG_GROUP_BASIC);
        Map<String, String> workTimeMap = loadConfigByGroup(CONFIG_GROUP_WORKING_TIME);

        BasicSettingsOutput output = new BasicSettingsOutput();
        output.setSystemName(basicMap.getOrDefault(KEY_SYSTEM_NAME, "米多工单系统"));
        output.setTimezone(basicMap.getOrDefault(KEY_TIMEZONE, "Asia/Shanghai"));
        output.setDefaultPageSize(parseInteger(basicMap.get(KEY_DEFAULT_PAGE_SIZE), 20));
        output.setWorkTimeStart(workTimeMap.getOrDefault(KEY_WORKING_TIME_START, "09:00"));
        output.setWorkTimeEnd(workTimeMap.getOrDefault(KEY_WORKING_TIME_END, "18:00"));
        return output;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateBasicSettings(BasicSettingsUpdateInput input) {
        upsertConfig(KEY_SYSTEM_NAME, input.getSystemName(), CONFIG_GROUP_BASIC, "系统名称");
        upsertConfig(KEY_TIMEZONE, input.getTimezone(), CONFIG_GROUP_BASIC, "默认时区");
        upsertConfig(KEY_DEFAULT_PAGE_SIZE, String.valueOf(input.getDefaultPageSize()), CONFIG_GROUP_BASIC, "默认分页条数");
        upsertConfig(KEY_WORKING_TIME_START, input.getWorkTimeStart(), CONFIG_GROUP_WORKING_TIME, "工作时间开始（HH:mm）");
        upsertConfig(KEY_WORKING_TIME_END, input.getWorkTimeEnd(), CONFIG_GROUP_WORKING_TIME, "工作时间结束（HH:mm）");
    }

    private Map<String, String> loadConfigByGroup(String group) {
        LambdaQueryWrapper<SystemConfigPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfigPO::getConfigGroup, group)
                .eq(SystemConfigPO::getDeleted, 0);
        List<SystemConfigPO> configs = systemConfigMapper.selectList(wrapper);
        Map<String, String> map = new HashMap<>();
        if (configs != null) {
            for (SystemConfigPO config : configs) {
                if (config.getConfigKey() != null && config.getConfigValue() != null) {
                    map.put(config.getConfigKey(), config.getConfigValue());
                }
            }
        }
        return map;
    }

    private void upsertConfig(String key, String value, String group, String description) {
        LambdaQueryWrapper<SystemConfigPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfigPO::getConfigKey, key)
                .eq(SystemConfigPO::getDeleted, 0);
        SystemConfigPO existing = systemConfigMapper.selectOne(wrapper);
        if (existing != null) {
            existing.setConfigValue(value);
            existing.setUpdateBy("system");
            existing.setUpdateTime(new Date());
            systemConfigMapper.updateById(existing);
        } else {
            SystemConfigPO newConfig = new SystemConfigPO();
            newConfig.setConfigKey(key);
            newConfig.setConfigValue(value);
            newConfig.setConfigGroup(group);
            newConfig.setDescription(description);
            newConfig.setCreateBy("system");
            newConfig.setUpdateBy("system");
            systemConfigMapper.insert(newConfig);
        }
    }

    private Integer parseInteger(String value, int defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
