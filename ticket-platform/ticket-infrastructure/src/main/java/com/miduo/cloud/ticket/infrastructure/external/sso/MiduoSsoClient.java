package com.miduo.cloud.ticket.infrastructure.external.sso;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * 米多星球 SSO 开放接口客户端
 * 负责调用 validate-login-token / refresh-session-token / revoke-session-token
 */
@Component
public class MiduoSsoClient {

    private static final Logger log = LoggerFactory.getLogger(MiduoSsoClient.class);

    private static final String VALIDATE_PATH = "/api/open/sso/validate-login-token";
    private static final String REFRESH_PATH = "/api/open/sso/refresh-session-token";
    private static final String REVOKE_PATH = "/api/open/sso/revoke-session-token";
    private static final String BRIDGE_PATH = "/api/sso/bridge/redirect-url";

    private static final int HTTP_TIMEOUT_MS = 10000;

    private final MiduoSsoProperties ssoProperties;

    public MiduoSsoClient(MiduoSsoProperties ssoProperties) {
        this.ssoProperties = ssoProperties;
    }

    /**
     * 校验交换 token，返回用户信息和 sessionToken
     */
    public ValidateResult validateLoginToken(String exchangeToken) {
        String url = ssoProperties.getBaseUrl() + VALIDATE_PATH;
        String body = JSON.toJSONString(java.util.Collections.singletonMap("token", exchangeToken));

        String response = executeSignedPost(url, body, exchangeToken);
        JSONObject json = JSON.parseObject(response);

        if (json == null) {
            throw BusinessException.of(ErrorCode.SSO_VALIDATE_FAILED, "米多 SSO 响应为空");
        }

        int returnCode = json.getIntValue("return_code");
        if (returnCode != 0) {
            String returnMsg = json.getString("return_msg");
            log.warn("SSO validate-login-token 失败: return_code={}, return_msg={}", returnCode, returnMsg);
            throw BusinessException.of(ErrorCode.SSO_VALIDATE_FAILED, "SSO 校验失败: " + returnMsg);
        }

        JSONObject returnData = json.getJSONObject("return_data");
        if (returnData == null || !returnData.getBooleanValue("valid")) {
            log.warn("SSO validate-login-token: valid=false");
            throw BusinessException.of(ErrorCode.SSO_VALIDATE_FAILED, "exchange token 无效或已过期");
        }

        ValidateResult result = new ValidateResult();
        result.setSessionToken(returnData.getString("sessionToken"));
        result.setUserId(returnData.getString("userId"));
        result.setEmployeeNo(returnData.getString("employeeNo"));
        result.setUserName(returnData.getString("userName"));
        result.setMobile(returnData.getString("mobile"));
        result.setEmail(returnData.getString("email"));
        return result;
    }

    /**
     * 续期 sessionToken
     */
    public boolean refreshSessionToken(String sessionToken) {
        String url = ssoProperties.getBaseUrl() + REFRESH_PATH;
        String body = JSON.toJSONString(java.util.Collections.singletonMap("sessionToken", sessionToken));

        String response = executeSignedPost(url, body, sessionToken);
        JSONObject json = JSON.parseObject(response);

        if (json == null) {
            log.warn("SSO refresh-session-token 响应为空");
            return false;
        }

        int returnCode = json.getIntValue("return_code");
        if (returnCode != 0) {
            log.warn("SSO refresh-session-token 失败: return_code={}, return_msg={}",
                    returnCode, json.getString("return_msg"));
            return false;
        }

        JSONObject returnData = json.getJSONObject("return_data");
        return returnData != null && returnData.getBooleanValue("success");
    }

    /**
     * 吊销 sessionToken
     */
    public boolean revokeSessionToken(String sessionToken) {
        String url = ssoProperties.getBaseUrl() + REVOKE_PATH;
        String body = JSON.toJSONString(java.util.Collections.singletonMap("sessionToken", sessionToken));

        String response = executeSignedPost(url, body, sessionToken);
        JSONObject json = JSON.parseObject(response);

        if (json == null) {
            log.warn("SSO revoke-session-token 响应为空");
            return false;
        }

        int returnCode = json.getIntValue("return_code");
        if (returnCode != 0) {
            log.warn("SSO revoke-session-token 失败: return_code={}, return_msg={}",
                    returnCode, json.getString("return_msg"));
            return false;
        }

        JSONObject returnData = json.getJSONObject("return_data");
        return returnData != null && returnData.getBooleanValue("success");
    }

    /**
     * 构建登录桥 URL
     */
    public String buildBridgeUrl(String state) {
        return ssoProperties.getBaseUrl() + BRIDGE_PATH
                + "?appCode=" + ssoProperties.getAppCode()
                + "&redirectUri=" + encodeUrl(ssoProperties.getRedirectUri())
                + "&state=" + encodeUrl(state);
    }

    private String executeSignedPost(String url, String body, String signedValue) {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String signature = computeSignature(signedValue, timestamp, nonce);

        try {
            HttpResponse response = HttpRequest.post(url)
                    .header("Content-Type", "application/json")
                    .header("X-App-Code", ssoProperties.getAppCode())
                    .header("X-App-Timestamp", timestamp)
                    .header("X-App-Nonce", nonce)
                    .header("X-App-Signature", signature)
                    .body(body)
                    .timeout(HTTP_TIMEOUT_MS)
                    .execute();
            return response.body();
        } catch (Exception e) {
            log.error("调用米多 SSO 接口失败: url={}, error={}", url, e.getMessage());
            throw BusinessException.of(ErrorCode.SSO_API_ERROR, "调用米多 SSO 接口异常: " + e.getMessage());
        }
    }

    private String computeSignature(String signedValue, String timestamp, String nonce) {
        String canonical = ssoProperties.getAppCode() + "\n"
                + timestamp + "\n"
                + nonce + "\n"
                + signedValue;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    ssoProperties.getAppSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(raw);
        } catch (Exception e) {
            throw BusinessException.of(ErrorCode.INTERNAL_ERROR, "SSO 签名计算失败");
        }
    }

    private String encodeUrl(String value) {
        if (value == null) {
            return "";
        }
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }

    /**
     * SSO 校验结果
     */
    @lombok.Data
    public static class ValidateResult {
        private String sessionToken;
        private String userId;
        private String employeeNo;
        private String userName;
        private String mobile;
        private String email;
    }
}
