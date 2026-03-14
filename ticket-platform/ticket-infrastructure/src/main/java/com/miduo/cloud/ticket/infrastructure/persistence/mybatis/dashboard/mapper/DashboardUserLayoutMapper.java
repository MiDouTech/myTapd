package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.dashboard.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.dashboard.po.DashboardUserLayoutPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 仪表盘个人布局Mapper接口
 */
@Mapper
public interface DashboardUserLayoutMapper extends BaseMapper<DashboardUserLayoutPO> {

    /**
     * 查询指定用户的布局列表，按 sort_order 升序，过滤 deleted=0
     *
     * @param userId 用户ID
     * @return 布局列表
     */
    List<DashboardUserLayoutPO> selectByUserId(@Param("userId") Long userId);

    /**
     * 批量插入布局记录
     *
     * @param list 布局PO列表
     */
    void batchInsert(@Param("list") List<DashboardUserLayoutPO> list);

    /**
     * 软删除指定用户的所有布局记录（设置 deleted=1）
     *
     * @param userId   用户ID
     * @param updateBy 操作人
     */
    void softDeleteByUserId(@Param("userId") Long userId, @Param("updateBy") String updateBy);
}
