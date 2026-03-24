package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.workflow.po.HandlerGroupMemberPO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 处理组成员Mapper
 */
@Mapper
public interface HandlerGroupMemberMapper extends BaseMapper<HandlerGroupMemberPO> {

    /**
     * 按处理组物理删除成员行（更新成员时需先清表，逻辑删除会保留 uk_group_user 唯一键冲突）
     */
    @Delete("DELETE FROM handler_group_member WHERE group_id = #{groupId}")
    int physicalDeleteByGroupId(@Param("groupId") Long groupId);
}
