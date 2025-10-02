package com.util;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * AI接口交互工具类，专注于药膳推荐的AI请求与响应处理
 */
@Component
public class DeepSeekClient {
    private static final Logger log = LoggerFactory.getLogger(DeepSeekClient.class);

    private final String API_KEY;
    private final String API_URL;
    private final HttpClient httpClient;
    private final Gson gson = new Gson();

    // 常量定义
    private static final String MODEL = "deepseek-chat";
    private static final double TEMPERATURE = 0.5;
    private static final int MAX_TOKENS = 1500;
    private static final int HTTP_TIMEOUT_SECONDS = 30;  // 超时时间30秒

    // 构造方法初始化（替代静态初始化）
    public DeepSeekClient() {
        // 加载配置
        Properties properties = new Properties();
        try (InputStream is = DeepSeekClient.class.getResourceAsStream("/config.properties")) {
            if (is == null) {
                throw new RuntimeException("未找到配置文件: /config.properties");
            }
            properties.load(is);

            // 配置校验
            API_KEY = properties.getProperty("key");
            API_URL = properties.getProperty("url");

            if (API_KEY == null || API_KEY.trim().isEmpty()) {
                throw new RuntimeException("配置文件中未设置有效的API_KEY");
            }
            if (API_URL == null || API_URL.trim().isEmpty()) {
                throw new RuntimeException("配置文件中未设置有效的API_URL");
            }
        } catch (Exception e) {
            throw new RuntimeException("加载AI配置失败", e);
        }

        // 初始化HttpClient（单例复用，带超时设置）
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS))
                .build();
    }

    /**
     * 根据用户健康信息生成药膳推荐
     * @param userInfo 用户健康信息封装
     * @return 解析后的药膳对象
     */
    public MedicinalDiet generateDietRecommendation(UserHealthInfo userInfo) {
        // 验证用户信息
        validateUserHealthInfo(userInfo);

        // 构建提示词
        String prompt = buildPrompt(userInfo);

        // 构建AI请求
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("user", prompt));
        ChatRequest requestBody = new ChatRequest(
                MODEL,
                messages,
                TEMPERATURE,
                MAX_TOKENS
        );

        // 发送请求并解析响应
        String response = sendRequest(requestBody);
        return parseDietResponse(response);
    }

    /**
     * 验证用户健康信息
     */
    private void validateUserHealthInfo(UserHealthInfo userInfo) {
        if (userInfo == null) {
            throw new IllegalArgumentException("用户健康信息不能为空");
        }
        if (userInfo.getAge() < 0 || userInfo.getAge() > 150) {
            throw new IllegalArgumentException("无效的年龄值: " + userInfo.getAge());
        }
        if (userInfo.getGender() == null || (!"男".equals(userInfo.getGender()) && !"女".equals(userInfo.getGender()))) {
            throw new IllegalArgumentException("无效的性别值: " + userInfo.getGender());
        }
    }

    /**
     * 构建AI提示词
     */
    // 在DeepSeekClient的buildPrompt方法中更新提示词
    private String buildPrompt(UserHealthInfo userInfo) {
        return String.format("""
        请根据以下用户健康信息，推荐1款适合的药膳：
        症状：%s
        性别：%s
        年龄：%s
        其他状况：%s

        要求：
        1. 必须返回纯JSON格式数据（无任何前置/后置文本），包含以下字段：
           - name: 药膳名称（字符串）
           - ingredients: 制作成分（数组，如["芹菜200g", "红枣5颗"]）
           - steps: 制作步骤（数组，如["步骤1...", "步骤2..."]）
           - reason: 适合原因（字符串，说明与症状的关联，对应功效）
           - taboo: 禁忌说明（字符串，如"孕妇慎用"）
           - suitableTime: 适宜食用时间（字符串，如"早餐"、"晚餐"）
           - tags: 标签列表（数组，如["健脾", "益气"]）
        2. 所有字段不可为null，内容简洁准确，用中文描述。
        3. 禁止返回任何非JSON内容（如解释、备注）。
        """,
                userInfo.getSymptom(),
                userInfo.getGender(),
                userInfo.getAge() > 0 ? userInfo.getAge() : "未提供",
                userInfo.getOtherConditions()
        );
    }

    /**
     * 解析AI响应为MedicinalDiet对象
     */
    private MedicinalDiet parseDietResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            throw new RuntimeException("AI返回空响应");
        }

        try {
            // 更安全的JSON提取方式
            int startIdx = response.indexOf('{');
            int endIdx = response.lastIndexOf('}');

            if (startIdx == -1 || endIdx == -1 || startIdx >= endIdx) {
                throw new RuntimeException("AI响应不包含有效的JSON结构");
            }

            String jsonContent = response.substring(startIdx, endIdx + 1);
            MedicinalDiet diet = gson.fromJson(jsonContent, MedicinalDiet.class);

            // 验证解析结果
            validateMedicinalDiet(diet);

            return diet;
        } catch (Exception e) {
            log.error("解析AI响应失败, 原始响应: {}", maskSensitiveInfo(response), e);
            throw new RuntimeException("解析AI响应失败: " + e.getMessage(), e);
        }
    }

    /**
     * 验证解析后的药膳信息
     */
    private void validateMedicinalDiet(MedicinalDiet diet) {
        if (diet == null) {
            throw new RuntimeException("解析后的药膳信息为空");
        }
        if (diet.getName() == null || diet.getName().trim().isEmpty()) {
            throw new RuntimeException("药膳名称不能为空");
        }
        if (diet.getIngredients() == null || diet.getIngredients().isEmpty()) {
            throw new RuntimeException("药膳成分不能为空");
        }
        if (diet.getSteps() == null || diet.getSteps().isEmpty()) {
            throw new RuntimeException("药膳步骤不能为空");
        }
        if (diet.getReason() == null || diet.getReason().trim().isEmpty()) {
            throw new RuntimeException("适合原因不能为空");
        }
    }

    /**
     * 发送HTTP请求到AI接口
     */
    private String sendRequest(ChatRequest requestBody) {
        try {
            String requestBodyJson = gson.toJson(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + API_KEY)
                    .timeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS))  // 设置请求超时
                    .POST(BodyPublishers.ofString(requestBodyJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                ChatResponse chatResponse = gson.fromJson(response.body(), ChatResponse.class);
                if (chatResponse.getChoices() == null || chatResponse.getChoices().isEmpty()) {
                    throw new RuntimeException("AI返回空结果");
                }
                return chatResponse.getChoices().get(0).getMessage().getContent();
            } else {
                String errorMsg = String.format("AI请求失败，状态码: %d, 响应: %s",
                        response.statusCode(), maskSensitiveInfo(response.body()));
                log.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }
        } catch (Exception e) {
            log.error("发送AI请求异常", e);
            throw new RuntimeException("发送AI请求异常: " + e.getMessage(), e);
        }
    }

    /**
     * 敏感信息脱敏
     */
    private String maskSensitiveInfo(String content) {
        if (content == null) return null;
        // 简单脱敏示例，可根据实际情况扩展
        return content.replaceAll(API_KEY, "***API_KEY***");
    }

    // ------------------------------ 内部静态实体类 ------------------------------

    /**
     * 用户健康信息实体类（供外部传入参数）
     */
    public static class UserHealthInfo {
        private String symptom; // 症状
        private String gender;  // 性别（男/女）
        private int age;        // 年龄
        private String otherConditions; // 其他健康状况

        public UserHealthInfo(String symptom, String gender, int age, String otherConditions) {
            this.symptom = symptom;
            this.gender = gender;
            this.age = age;
            this.otherConditions = otherConditions;
        }

        // Getter方法
        public String getSymptom() { return symptom; }
        public String getGender() { return gender; }
        public int getAge() { return age; }
        public String getOtherConditions() { return otherConditions; }
    }

    /**
     * 药膳结果实体类（AI返回的结构化内容）
     */
    public static class MedicinalDiet {
        private String name;          // 药膳名称
        private List<String> ingredients; // 制作成分
        private List<String> steps;   // 制作步骤
        private String reason;        // 适合原因

        // 新增字段
        private String taboo;         // 禁忌说明
        private String suitableTime;  // 适宜食用时间
        private List<String> tags;    // 标签列表
        // 无参构造（Gson解析必需）
        public MedicinalDiet() {}

        // 新增字段的Getter和Setter
        public String getTaboo() { return taboo; }
        public void setTaboo(String taboo) { this.taboo = taboo; }

        public String getSuitableTime() { return suitableTime; }
        public void setSuitableTime(String suitableTime) { this.suitableTime = suitableTime; }

        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
        // Getter和Setter
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public List<String> getIngredients() { return ingredients; }
        public void setIngredients(List<String> ingredients) { this.ingredients = ingredients; }

        public List<String> getSteps() { return steps; }
        public void setSteps(List<String> steps) { this.steps = steps; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    /**
     * 内部消息实体类
     */
    private static class Message {
        private String role;
        private String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getContent() { return content; }
    }

    /**
     * AI请求体结构
     */
    private static class ChatRequest {
        private String model;
        private List<Message> messages;
        private double temperature;
        private int max_tokens;

        public ChatRequest(String model, List<Message> messages, double temperature, int max_tokens) {
            this.model = model;
            this.messages = messages;
            this.temperature = temperature;
            this.max_tokens = max_tokens;
        }
    }

    /**
     * AI响应体结构
     */
    private static class ChatResponse {
        private List<Choice> choices;

        public List<Choice> getChoices() { return choices; }

        private static class Choice {
            private Message message;

            public Message getMessage() { return message; }
        }
    }
}
