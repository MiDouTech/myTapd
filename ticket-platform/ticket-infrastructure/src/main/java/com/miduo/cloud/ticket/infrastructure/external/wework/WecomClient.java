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

    private static final String GET_USER_INFO_PATH = "/cgi-bin/auth/getuserinfo";
    private static final String GET_USER_DETAIL_PATH = "/cgi-bin/user/get";
    private static final String GET_DEPARTMENT_LIST_PATH = "/cgi-bin/department/list";
    private static final String GET_DEPARTMENT_USER_PATH = "/cgi-bin/user/list";
    private static final String SEND_APP_MESSAGE_PATH = "/cgi-bin/message/send";
    private static final Long ROOT_DEPARTMENT_ID = 1L;

    private final WecomTokenManager tokenManager;
    private final WeworkRuntimeConfigProvider runtimeConfigProvider;

    public WecomClient(WecomTokenManager tokenManager, WeworkRuntimeConfigProvider runtimeConfigProvider) {
        this.tokenManager = tokenManager;
        this.runtimeConfigProvider = runtimeConfigProvider;
    }

    /**
     * 用auth_code换取用户身份（UserId）
     */
    public WecomUserIdentity getUserInfoByCode(String code) {
        String accessToken = tokenManager.getAccessToken();
        String url = buildApiUrl(GET_USER_INFO_PATH) + "?access_token=" + accessToken + "&code=" + code;
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
        String url = buildApiUrl(GET_USER_DETAIL_PATH) + "?access_token=" + accessToken + "&userid=" + wecomUserId;
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
        detail.setGender(parseGender(json.getString("gender")));
        detail.setAvatar(json.getString("avatar"));
        detail.setStatus(json.getIntValue("status"));

        JSONArray deptIds = json.getJSONArray("department");
        detail.setDepartmentIds(parseLongList(deptIds));
        detail.setMainDepartment(resolveMainDepartment(json, detail.getDepartmentIds()));
        detail.setDirectLeaders(parseStringList(json.getJSONArray("direct_leader")));
        detail.setLeaderInDepartments(parseIntegerList(json.getJSONArray("is_leader_in_dept")));

        return detail;
    }

    /**
     * 获取部门列表
     */
    public List<WecomDepartment> getDepartmentList() {
        String accessToken = tokenManager.getContactAccessToken();
        String url = buildApiUrl(GET_DEPARTMENT_LIST_PATH) + "?access_token=" + accessToken;
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
                List<String> leaders = parseStringList(deptJson.getJSONArray("department_leader"));
                dept.setLeaderWecomUserids(leaders);
                dept.setPrimaryLeaderWecomUserid(leaders.isEmpty() ? null : leaders.get(0));
                departments.add(dept);
            }
        }
        return departments;
    }

    /**
     * 获取部门下的成员列表（详情）
     */
    public List<WecomUserDetail> getDepartmentUsers(Long departmentId) {
        return getDepartmentUsers(departmentId, false);
    }

    /**
     * 获取部门下的成员列表（支持递归子部门）
     */
    public List<WecomUserDetail> getDepartmentUsers(Long departmentId, boolean fetchChild) {
        String accessToken = tokenManager.getContactAccessToken();
        Long targetDepartmentId = departmentId == null ? ROOT_DEPARTMENT_ID : departmentId;
        String url = buildApiUrl(GET_DEPARTMENT_USER_PATH)
                + "?access_token=" + accessToken
                + "&department_id=" + targetDepartmentId
                + "&fetch_child=" + (fetchChild ? 1 : 0);
        String response = HttpUtil.get(url);
        JSONObject json = JSON.parseObject(response);

        if (json == null || json.getIntValue("errcode") != 0) {
            String errMsg = json != null ? json.getString("errmsg") : "response is null";
            log.error("获取部门成员失败, deptId={}, fetchChild={}, error={}", targetDepartmentId, fetchChild, errMsg);
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
                detail.setGender(parseGender(userJson.getString("gender")));
                detail.setAvatar(userJson.getString("avatar"));
                detail.setStatus(userJson.getIntValue("status"));

                JSONArray deptIds = userJson.getJSONArray("department");
                detail.setDepartmentIds(parseLongList(deptIds));
                detail.setMainDepartment(resolveMainDepartment(userJson, detail.getDepartmentIds()));
                detail.setDirectLeaders(parseStringList(userJson.getJSONArray("direct_leader")));
                detail.setLeaderInDepartments(parseIntegerList(userJson.getJSONArray("is_leader_in_dept")));
                users.add(detail);
            }
        }
        return users;
    }

    /**
     * 发送企微应用文本卡片消息
     */
    public void sendTextCardMessage(String toUser, String title, String description, String url, String btnText) {
        if (toUser == null || toUser.trim().isEmpty()) {
            log.warn("企微应用消息接收人为空，跳过发送");
            return;
        }

        int agentId;
        try {
            String configAgentId = runtimeConfigProvider.getRuntimeConfig().getAgentId();
            agentId = Integer.parseInt(configAgentId);
        } catch (Exception ex) {
            log.error("企微AgentId配置非法");
            throw BusinessException.of(ErrorCode.WECOM_API_ERROR, "企微AgentId配置非法");
        }

        String accessToken = tokenManager.getAccessToken();
        String requestUrl = buildApiUrl(SEND_APP_MESSAGE_PATH) + "?access_token=" + accessToken;

        JSONObject textCard = new JSONObject();
        textCard.put("title", title == null ? "工单通知" : title);
        textCard.put("description", description == null ? "" : description);
        textCard.put("url", url == null ? "" : url);
        textCard.put("btntxt", btnText == null ? "查看详情" : btnText);

        JSONObject payload = new JSONObject();
        payload.put("touser", toUser);
        payload.put("msgtype", "textcard");
        payload.put("agentid", agentId);
        payload.put("textcard", textCard);

        String response = HttpUtil.post(requestUrl, payload.toJSONString());
        JSONObject result = JSON.parseObject(response);
        if (result == null || result.getIntValue("errcode") != 0) {
            String errMsg = result != null ? result.getString("errmsg") : "response is null";
            log.error("发送企微应用消息失败: toUser={}, errMsg={}", toUser, errMsg);
            throw BusinessException.of(ErrorCode.WECOM_API_ERROR, "发送企微应用消息失败: " + errMsg);
        }
    }

    /**
     * 通过企微AI bot response_url直接回复消息
     * 适用于AI bot单聊场景，无需access_token
     * AI bot response_url 仅支持 markdown 类型消息
     */
    public void sendAibotReply(String responseUrl, String content) {
        if (responseUrl == null || responseUrl.trim().isEmpty()) {
            log.warn("企微AI bot response_url为空，跳过回复");
            return;
        }
        if (content == null || content.trim().isEmpty()) {
            log.warn("企微AI bot回复内容为空，跳过发送");
            return;
        }

        JSONObject markdown = new JSONObject();
        markdown.put("content", content);

        JSONObject payload = new JSONObject();
        payload.put("msgtype", "markdown");
        payload.put("markdown", markdown);

        String response = HttpUtil.post(responseUrl.trim(), payload.toJSONString());
        JSONObject result = JSON.parseObject(response);
        if (result == null || result.getIntValue("errcode") != 0) {
            String errMsg = result != null ? result.getString("errmsg") : "response is null";
            log.error("发送企微AI bot回复失败: errMsg={}", errMsg);
            throw BusinessException.of(ErrorCode.WECOM_API_ERROR, "发送企微AI bot回复失败: " + errMsg);
        }
        log.info("企微AI bot回复发送成功");
    }

    /**
     * 发送企微应用文本消息（text类型，无url字段要求）
     * 适用于bot单聊通知场景
     */
    public void sendTextMessage(String toUser, String content) {
        if (toUser == null || toUser.trim().isEmpty()) {
            log.warn("企微应用文本消息接收人为空，跳过发送");
            return;
        }

        int agentId;
        try {
            String configAgentId = runtimeConfigProvider.getRuntimeConfig().getAgentId();
            agentId = Integer.parseInt(configAgentId);
        } catch (Exception ex) {
            log.error("企微AgentId配置非法");
            throw BusinessException.of(ErrorCode.WECOM_API_ERROR, "企微AgentId配置非法");
        }

        String accessToken = tokenManager.getAccessToken();
        String requestUrl = buildApiUrl(SEND_APP_MESSAGE_PATH) + "?access_token=" + accessToken;

        JSONObject text = new JSONObject();
        text.put("content", content == null ? "" : content);

        JSONObject payload = new JSONObject();
        payload.put("touser", toUser);
        payload.put("msgtype", "text");
        payload.put("agentid", agentId);
        payload.put("text", text);

        String response = HttpUtil.post(requestUrl, payload.toJSONString());
        JSONObject result = JSON.parseObject(response);
        if (result == null || result.getIntValue("errcode") != 0) {
            String errMsg = result != null ? result.getString("errmsg") : "response is null";
            log.error("发送企微应用文本消息失败: toUser={}, errMsg={}", toUser, errMsg);
            throw BusinessException.of(ErrorCode.WECOM_API_ERROR, "发送企微应用文本消息失败: " + errMsg);
        }
    }

    /**
     * 发送企微群机器人Markdown消息
     */
    public void sendGroupWebhookMarkdown(String webhookUrl, String markdownContent) {
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            log.warn("企微群Webhook地址为空，跳过发送");
            return;
        }

        JSONObject markdown = new JSONObject();
        markdown.put("content", markdownContent == null ? "" : markdownContent);

        JSONObject payload = new JSONObject();
        payload.put("msgtype", "markdown");
        payload.put("markdown", markdown);

        String response = HttpUtil.post(webhookUrl, payload.toJSONString());
        JSONObject result = JSON.parseObject(response);
        if (result == null || result.getIntValue("errcode") != 0) {
            String errMsg = result != null ? result.getString("errmsg") : "response is null";
            log.error("发送企微群Webhook失败: webhookUrl={}, errMsg={}", webhookUrl, errMsg);
            throw BusinessException.of(ErrorCode.WECOM_API_ERROR, "发送企微群Webhook失败: " + errMsg);
        }
    }

    private String buildApiUrl(String path) {
        String baseUrl = runtimeConfigProvider.getRuntimeConfig().getApiBaseUrl();
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + path;
        }
        return baseUrl + path;
    }

    private Integer parseGender(String gender) {
        if (gender == null || gender.trim().isEmpty()) {
            return 0;
        }
        try {
            int value = Integer.parseInt(gender.trim());
            if (value == 1 || value == 2) {
                return value;
            }
        } catch (NumberFormatException ignored) {
            // 按未知性别处理，避免解析异常影响同步流程
        }
        return 0;
    }

    private Long resolveMainDepartment(JSONObject userJson, List<Long> departmentIds) {
        Long mainDepartment = userJson.getLong("main_department");
        if (mainDepartment != null) {
            return mainDepartment;
        }
        if (departmentIds == null || departmentIds.isEmpty()) {
            return null;
        }
        return departmentIds.get(0);
    }

    private List<Long> parseLongList(JSONArray array) {
        if (array == null || array.isEmpty()) {
            return new ArrayList<>();
        }
        return array.toJavaList(Long.class);
    }

    private List<Integer> parseIntegerList(JSONArray array) {
        if (array == null || array.isEmpty()) {
            return new ArrayList<>();
        }
        return array.toJavaList(Integer.class);
    }

    private List<String> parseStringList(JSONArray array) {
        if (array == null || array.isEmpty()) {
            return new ArrayList<>();
        }
        return array.toJavaList(String.class);
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
        private Integer gender;
        private String avatar;
        private Integer status;
        private List<Long> departmentIds;
        private Long mainDepartment;
        private List<String> directLeaders;
        private List<Integer> leaderInDepartments;
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
        private List<String> leaderWecomUserids;
        private String primaryLeaderWecomUserid;
    }
}
