package com.miduo.cloud.ticket.infrastructure.external.wework;

import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 企业微信API客户端
 * 封装企微服务端API调用：用户认证、通讯录同步等
 */
@Component
public class WecomClient {

    private static final Logger log = LoggerFactory.getLogger(WecomClient.class);

    private static final String GET_USER_INFO_URL = "https://qyapi.weixin.qq.com/cgi-bin/auth/getuserinfo";
    private static final String GET_USER_DETAIL_URL = "https://qyapi.weixin.qq.com/cgi-bin/user/get";
    private static final String GET_DEPARTMENT_LIST_URL = "https://qyapi.weixin.qq.com/cgi-bin/department/list";
    private static final String GET_DEPARTMENT_USER_URL = "https://qyapi.weixin.qq.com/cgi-bin/user/list";

    private final WecomTokenManager tokenManager;

    public WecomClient(WecomTokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    /**
     * 用auth_code换取用户身份（UserId）
     */
    public WecomUserIdentity getUserInfoByCode(String code) {
        String accessToken = tokenManager.getAccessToken();
        String url = GET_USER_INFO_URL + "?access_token=" + accessToken + "&code=" + code;
        String response = HttpUtil.get(url);
        JSONObject json = JSON.parseObject(response);

        if (json == null || json.getIntValue("errcode") != 0) {
            String errMsg = json != null ? json.getString("errmsg") : "response is null";
            log.error("企微code换取用户身份失败: {}", errMsg);
            throw BusinessException.of(ErrorCode.WECOM_AUTH_FAILED, "企微认证失败: " + errMsg);
        }

        WecomUserIdentity identity = new WecomUserIdentity();
        identity.setUserId(json.getString("userid"));
        identity.setUserTicket(json.getString("user_ticket"));
        return identity;
    }

    /**
     * 获取企微用户详情
     */
    public WecomUserDetail getUserDetail(String wecomUserId) {
        String accessToken = tokenManager.getContactAccessToken();
        String url = GET_USER_DETAIL_URL + "?access_token=" + accessToken + "&userid=" + wecomUserId;
        String response = HttpUtil.get(url);
        JSONObject json = JSON.parseObject(response);

        if (json == null || json.getIntValue("errcode") != 0) {
            String errMsg = json != null ? json.getString("errmsg") : "response is null";
            log.error("获取企微用户详情失败, userId={}, error={}", wecomUserId, errMsg);
            throw BusinessException.of(ErrorCode.WECOM_API_ERROR, "获取用户详情失败: " + errMsg);
        }

        WecomUserDetail detail = new WecomUserDetail();
        detail.setUserId(json.getString("userid"));
        detail.setName(json.getString("name"));
        detail.setMobile(json.getString("mobile"));
        detail.setEmail(json.getString("email"));
        detail.setPosition(json.getString("position"));
        detail.setAvatar(json.getString("avatar"));
        detail.setStatus(json.getIntValue("status"));

        JSONArray deptIds = json.getJSONArray("department");
        if (deptIds != null && !deptIds.isEmpty()) {
            detail.setDepartmentIds(deptIds.toJavaList(Long.class));
            detail.setMainDepartment(deptIds.getLong(0));
        } else {
            detail.setDepartmentIds(new ArrayList<>());
        }

        return detail;
    }

    /**
     * 获取部门列表
     */
    public List<WecomDepartment> getDepartmentList() {
        String accessToken = tokenManager.getContactAccessToken();
        String url = GET_DEPARTMENT_LIST_URL + "?access_token=" + accessToken;
        String response = HttpUtil.get(url);
        JSONObject json = JSON.parseObject(response);

        if (json == null || json.getIntValue("errcode") != 0) {
            String errMsg = json != null ? json.getString("errmsg") : "response is null";
            log.error("获取企微部门列表失败: {}", errMsg);
            throw BusinessException.of(ErrorCode.WECOM_API_ERROR, "获取部门列表失败: " + errMsg);
        }

        JSONArray deptArray = json.getJSONArray("department");
        List<WecomDepartment> departments = new ArrayList<>();
        if (deptArray != null) {
            for (int i = 0; i < deptArray.size(); i++) {
                JSONObject deptJson = deptArray.getJSONObject(i);
                WecomDepartment dept = new WecomDepartment();
                dept.setId(deptJson.getLong("id"));
                dept.setName(deptJson.getString("name"));
                dept.setParentId(deptJson.getLong("parentid"));
                dept.setOrder(deptJson.getIntValue("order"));
                departments.add(dept);
            }
        }
        return departments;
    }

    /**
     * 获取部门下的成员列表（详情）
     */
    public List<WecomUserDetail> getDepartmentUsers(Long departmentId) {
        String accessToken = tokenManager.getContactAccessToken();
        String url = GET_DEPARTMENT_USER_URL
                + "?access_token=" + accessToken
                + "&department_id=" + departmentId;
        String response = HttpUtil.get(url);
        JSONObject json = JSON.parseObject(response);

        if (json == null || json.getIntValue("errcode") != 0) {
            String errMsg = json != null ? json.getString("errmsg") : "response is null";
            log.error("获取部门成员失败, deptId={}, error={}", departmentId, errMsg);
            throw BusinessException.of(ErrorCode.WECOM_API_ERROR, "获取部门成员失败: " + errMsg);
        }

        JSONArray userArray = json.getJSONArray("userlist");
        List<WecomUserDetail> users = new ArrayList<>();
        if (userArray != null) {
            for (int i = 0; i < userArray.size(); i++) {
                JSONObject userJson = userArray.getJSONObject(i);
                WecomUserDetail detail = new WecomUserDetail();
                detail.setUserId(userJson.getString("userid"));
                detail.setName(userJson.getString("name"));
                detail.setMobile(userJson.getString("mobile"));
                detail.setEmail(userJson.getString("email"));
                detail.setPosition(userJson.getString("position"));
                detail.setAvatar(userJson.getString("avatar"));
                detail.setStatus(userJson.getIntValue("status"));

                JSONArray deptIds = userJson.getJSONArray("department");
                if (deptIds != null && !deptIds.isEmpty()) {
                    detail.setDepartmentIds(deptIds.toJavaList(Long.class));
                    detail.setMainDepartment(deptIds.getLong(0));
                } else {
                    detail.setDepartmentIds(new ArrayList<>());
                }
                users.add(detail);
            }
        }
        return users;
    }

    /**
     * 企微用户身份（code换取后）
     */
    @lombok.Data
    public static class WecomUserIdentity {
        private String userId;
        private String userTicket;
    }

    /**
     * 企微用户详情
     */
    @lombok.Data
    public static class WecomUserDetail {
        private String userId;
        private String name;
        private String mobile;
        private String email;
        private String position;
        private String avatar;
        private Integer status;
        private List<Long> departmentIds;
        private Long mainDepartment;
    }

    /**
     * 企微部门信息
     */
    @lombok.Data
    public static class WecomDepartment {
        private Long id;
        private String name;
        private Long parentId;
        private Integer order;
    }
}
