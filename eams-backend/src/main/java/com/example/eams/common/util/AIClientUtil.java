package com.example.eams.common.util;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.eams.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

/**
 * DeepSeek 大模型 API 调用工具类
 * <p>
 * 封装 DeepSeek Chat Completion API，包含:
 * <ul>
 *   <li>单次调用 15秒超时</li>
 *   <li>最大3次重试</li>
 *   <li>连续失败自动触发降级标记</li>
 * </ul>
 */
@Slf4j
public class AIClientUtil {

    /** DeepSeek API 地址 */
    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";

    /** 默认模型 */
    private static final String MODEL = "deepseek-chat";

    /** 单次调用超时（毫秒） */
    private static final int TIMEOUT_MS = 15_000;

    /** 最大重试次数 */
    private static final int MAX_RETRIES = 3;

    /** API Key（由 application.yml 注入） */
    private static String API_KEY;

    /** 降级标记 Redis Key */
    private static final String DEGRADE_KEY = "eams:ai:degraded";

    /** 降级标记过期时间（秒） */
    private static final int DEGRADE_TTL_SEC = 300;

    /**
     * Spring 容器启动时注入 API Key
     */
    public static void setApiKey(String apiKey) {
        API_KEY = apiKey;
    }

    // ==================== 核心调用 ====================

    /**
     * 调用 DeepSeek Chat API
     *
     * @param systemPrompt 系统提示词（定义 AI 角色和行为）
     * @param userQuery    用户自然语言查询
     * @return AI 返回的 content 文本（JSON格式的查询条件 + 自然语言总结）
     * @throws BusinessException 调用失败时抛出（503-降级, 500-服务异常）
     */
    public static String query(String systemPrompt, String userQuery) {
        // 1. 检查降级标记
        if (RedisUtil.exists(DEGRADE_KEY)) {
            throw new BusinessException(503,
                    "AI智能查询服务暂时不可用，请使用传统筛选查询");
        }

        // 2. 限流检查（由 Controller 层 AOP 完成，此处做二次兜底）
        // 3. 重试调用
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                String content = doQuery(systemPrompt, userQuery);
                // 调用成功，清除失败计数
                return content;
            } catch (BusinessException e) {
                // 非重试异常直接抛出
                if (e.getCode() == 429 || e.getCode() == 503) {
                    throw e;
                }
                // 最后一次重试仍失败 → 触发降级
                if (attempt == MAX_RETRIES) {
                    triggerDegrade();
                    throw new BusinessException(503,
                            "AI智能查询服务暂时不可用，请使用传统筛选查询");
                }
                log.warn("DeepSeek调用失败，第{}次重试: {}", attempt, e.getMessage());
            }
        }
        return null; // unreachable
    }

    /**
     * 探测 DeepSeek API 是否恢复
     *
     * @return true-已恢复, false-仍不可用
     */
    public static boolean probe() {
        try {
            doQuery("你只需要回复 OK", "ping");
            // 探测成功，清除降级标记
            RedisUtil.del(DEGRADE_KEY);
            log.info("DeepSeek API 已恢复，降级标记已清除");
            return true;
        } catch (Exception e) {
            // 探测失败，续期降级标记
            RedisUtil.set(DEGRADE_KEY, "1", DEGRADE_TTL_SEC);
            return false;
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 单次 API 调用
     */
    @SuppressWarnings("unchecked")
    private static String doQuery(String systemPrompt, String userQuery) {
        // 构建请求体
        JSONObject body = JSONUtil.createObj();
        body.set("model", MODEL);
        body.set("temperature", 0.1);  // 低随机性，保证输出稳定

        JSONObject sysMsg = JSONUtil.createObj()
                .set("role", "system")
                .set("content", systemPrompt);
        JSONObject userMsg = JSONUtil.createObj()
                .set("role", "user")
                .set("content", userQuery);
        body.set("messages", new JSONObject[]{sysMsg, userMsg});

        // 发送请求
        try (HttpResponse response = HttpRequest.post(API_URL)
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .body(body.toString())
                .timeout(TIMEOUT_MS)
                .execute()) {

            int status = response.getStatus();
            String respBody = response.body();

            if (status == 200) {
                JSONObject respJson = JSONUtil.parseObj(respBody);
                List<JSONObject> choices = (List<JSONObject>) respJson.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    JSONObject message = (JSONObject) choices.get(0).get("message");
                    return (String) message.get("content");
                }
                throw new BusinessException(500, "AI查询返回为空");
            } else if (status == 429) {
                throw new BusinessException(429, "AI服务繁忙，请稍后重试");
            } else {
                log.error("DeepSeek API错误: status={}, body={}", status, respBody);
                throw new BusinessException(500,
                        "AI查询失败: HTTP " + status);
            }
        }
    }

    /**
     * 触发降级
     */
    private static void triggerDegrade() {
        RedisUtil.set(DEGRADE_KEY, "1", DEGRADE_TTL_SEC);
        log.warn("DeepSeek API 连续{}次调用失败，已触发降级，{}秒后自动探测恢复",
                MAX_RETRIES, DEGRADE_TTL_SEC);
    }
}
