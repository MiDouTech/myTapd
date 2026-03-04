package com.miduo.cloud.ticket.domain.user.repository;

import com.miduo.cloud.ticket.domain.user.model.User;

import java.util.List;

/**
 * 用户仓储接口
 */
public interface UserRepository {

    /**
     * 根据ID查询用户
     */
    User findById(Long id);

    /**
     * 根据企微userid查询用户
     */
    User findByWecomUserid(String wecomUserid);

    /**
     * 查询所有用户
     */
    List<User> findAll();

    /**
     * 查询所有激活用户
     */
    List<User> findAllActive();

    /**
     * 根据部门ID查询用户列表
     */
    List<User> findByDepartmentId(Long departmentId);

    /**
     * 根据部门ID列表批量查询用户
     */
    List<User> findByDepartmentIds(List<Long> departmentIds);

    /**
     * 保存用户（新增或更新）
     */
    User save(User user);

    /**
     * 批量保存用户
     */
    void batchSave(List<User> users);

    /**
     * 更新用户状态
     */
    void updateAccountStatus(Long userId, Integer status);

    /**
     * 查询用户角色编码列表
     */
    List<String> findRoleCodes(Long userId);

    /**
     * 为用户分配角色
     */
    void assignRole(Long userId, Long roleId);
}
