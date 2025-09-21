import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import com.google.gson.Gson;
import com.util.TypewriterEffect;
import org.apache.hc.core5.http.Message;


public class DeepSeekClient {

    private static  String API_KEY;
    private static  String API_URL;

    static {
        Properties properties = new Properties();
        try {

            InputStream is = DeepSeekClient.class.getResourceAsStream("/config.properties");
            properties.load(is);
            API_KEY = properties.getProperty("key");
            API_URL = properties.getProperty("url");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("*** 欢迎使用AI医药药膳助手 ***");
        System.out.println("请提供以下信息，我将为您推荐适合的药膳：");

        // 分步收集用户信息
        System.out.print("1. 主要症状（如：高血压、失眠）：");
        String symptom = scanner.nextLine();

        System.out.print("2. 性别（男/女）：");
        String gender = scanner.nextLine();

        System.out.print("3. 年龄（可选，直接回车跳过）：");
        int age = 0;
        String ageInput = scanner.nextLine();
        if (!ageInput.isEmpty()) {
            age = Integer.parseInt(ageInput); // 实际使用需加异常处理
        }

        System.out.print("4. 其他健康状况（可选，如：高血脂）：");
        String otherConditions = scanner.nextLine();

        // 封装用户信息
        UserHealthInfo userInfo = new UserHealthInfo(symptom, gender, age, otherConditions);
        // 生成药膳推荐
        recommendDiet(userInfo);

        scanner.close();
    }
//    public static void main(String[] args) {
//        Scanner scanner = new Scanner(System.in);
//        String input = "";
//        System.out.println("*** 我是 你爹 ，很高兴见到你 ****");
//        while(true){
//            System.out.println("---请说您问题：---");
//            String question = scanner.next();
//            if("bye".equals(question)){
//                break;
//            }
//            ask(question);
//            System.out.println();
//        }
//        System.out.println("拜拜，欢迎下次使用！");
//    }

    /**
     * 用户健康信息实体类，收集个性化参数
     */
    static class UserHealthInfo {
        private String symptom; // 症状（如：高血压、糖尿病）
        private String gender;  // 性别（男/女）
        private int age;        // 年龄（可选）
        private String otherConditions; // 其他健康状况（可选，如：高血脂）

        // 构造方法、getter/setter
        public UserHealthInfo(String symptom, String gender, int age, String otherConditions) {
            this.symptom = symptom;
            this.gender = gender;
            this.age = age;
            this.otherConditions = otherConditions;
        }

        // getter方法（用于后续拼接提示词）
        public String getSymptom() { return symptom; }
        public String getGender() { return gender; }
        public int getAge() { return age; }
        public String getOtherConditions() { return otherConditions; }
    }
    /**
     * 药膳结果实体类，对应AI返回的结构化内容
     */
    static class MedicinalDiet {
        private String name;          // 药膳名称
        private List<String> ingredients; // 制作成分（新增）
        private List<String> steps;   // 制作步骤
        private String reason;        // 适合原因

        // 必须添加无参构造（Gson解析需要）
        public MedicinalDiet() {}

        // 全参构造（可选）
        public MedicinalDiet(String name, List<String> ingredients, List<String> steps, String reason) {
            this.name = name;
            this.ingredients = ingredients;
            this.steps = steps;
            this.reason = reason;
        }

        // getter和setter（必须，Gson需要通过setter赋值）
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
     * 根据用户健康信息推荐药膳
     */
    public static void recommendDiet(UserHealthInfo userInfo) {
        // 构建提示词（关键：明确格式要求，约束AI输出）
        // 优化后的prompt示例（重点修改要求部分）
        String prompt = String.format("""
    请根据以下用户信息，推荐1款适合的药膳：
    症状：%s
    性别：%s
    年龄：%s
    其他状况：%s

    要求：
    1. 必须返回纯JSON格式数据（无任何前置/后置文本），包含以下字段：
       - name: 药膳名称（字符串）
       - ingredients: 制作成分（数组，如["芹菜200g", "红枣5颗"]）
       - steps: 制作步骤（数组，如["步骤1...", "步骤2..."]）
       - reason: 适合原因（字符串，说明与症状的关联）
    2. 所有字段不可为null，内容简洁准确，用中文描述。
    3. 禁止返回任何非JSON内容（如解释、备注）。
    """,
                userInfo.getSymptom(),
                userInfo.getGender(),
                userInfo.getAge() > 0 ? userInfo.getAge() : "未提供",
                userInfo.getOtherConditions().isEmpty() ? "无" : userInfo.getOtherConditions()
        );

        // 构建请求（复用原ChatRequest结构，消息角色为user，内容为上述prompt）
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("user", prompt));
        ChatRequest requestBody = new ChatRequest(
                "deepseek-chat",
                messages,
                0.5,  // 降低随机性（医药场景需更严谨）
                1500  // 增加最大令牌数（确保步骤和原因完整）
        );

        System.out.println(">>>正在为您推荐适合的药膳...");
        long startTime = System.currentTimeMillis();
        String response = sendRequest(requestBody);
        long endTime = System.currentTimeMillis();
        System.out.println("推荐用时：" + (endTime - startTime) / 1000 + "秒\n");

        // 解析响应并展示
        parseAndShowDiet(response);
    }
    /**
     * 解析AI响应，提取药膳信息并展示
     */
    private static void parseAndShowDiet(String response) {
        try {
            // 移除可能的多余空格（避免JSON格式错误）
            response = response.trim();
            Gson gson = new Gson();
            // 解析JSON为MedicinalDiet对象（需先更新MedicinalDiet类）
            MedicinalDiet diet = gson.fromJson(response, MedicinalDiet.class);

            // 校验关键字段非空
            if (diet.getName() == null || diet.getSteps() == null || diet.getIngredients() == null) {
                throw new Exception("AI返回的JSON缺少必要字段");
            }

            // 格式化展示结果
            System.out.println("✅ 为您推荐的药膳：");
            System.out.println("名称：" + diet.getName());
            System.out.println("制作成分：");
            diet.getIngredients().forEach(ingredient -> System.out.println("- " + ingredient));
            System.out.println("制作步骤：");
            for (int i = 0; i < diet.getSteps().size(); i++) {
                System.out.println((i + 1) + ". " + diet.getSteps().get(i));
            }
            System.out.println("适合原因：" + diet.getReason());
        } catch (Exception e) {
            System.out.println("解析推荐结果失败：" + e.getMessage());
            System.out.println("原始响应：" + response); // 调试用
        }
    }

    // 辅助方法：提取指定段落内容
    private static String extractSection(String response, String startFlag, String endFlag) {
        int start = response.indexOf(startFlag) + startFlag.length();
        int end = response.indexOf(endFlag, start);
        if (end == -1) end = response.length();
        return response.substring(start, end).trim();
    }

    // 辅助方法：提取步骤列表
    // 辅助方法：提取步骤列表
    private static List<String> extractSteps(String response, String startFlag, String endFlag) {
        String stepsStr = extractSection(response, startFlag, endFlag);
        List<String> steps = new ArrayList<>();
        for (String step : stepsStr.split("\n")) {
            String trimmedStep = step.trim();
            // 匹配数字开头的步骤（如1. 2. 等）
            if (trimmedStep.matches("^\\d+\\. .+")) {
                steps.add(trimmedStep.replaceAll("^\\d+\\. ", ""));
            }
        }
        return steps;
    }
    public static void ask(String content){
        // 创建消息列表
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("user", content));

        // 构建请求体
        ChatRequest requestBody = new ChatRequest(
                "deepseek-chat",  // 模型名称，根据文档调整
                messages,
                0.7,  // temperature
                1000  // max_tokens
        );
        System.out.println(">>>正在提交问题...");
        long startTime = System.currentTimeMillis();
        // 发送请求
        String response = sendRequest(requestBody);
        long endTime = System.currentTimeMillis();
        System.out.println("思考用时："+(endTime-startTime)/1000+"秒");
//        System.out.println("响应内容: " + response);
        TypewriterEffect.printWord(response,20);
    }

    private static String sendRequest(ChatRequest requestBody) {
        HttpClient client = HttpClient.newHttpClient();
        Gson gson = new Gson();
        //将 ChatRequest 对象中封装的数据转为 JSON 格式
        String requestBodyJson = gson.toJson(requestBody);

//        System.out.println("请求体：");
//        System.out.println(requestBodyJson);

        try {
            //构建请求对象 并指定请求头内容格式及身份验证的key
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + API_KEY)
                    //将JSON格式的字符串封装为 BodyPublishers 对象
                    .POST(BodyPublishers.ofString(requestBodyJson))
                    .build();  //构建请求对象

            System.out.println(">>>已提交问题，正在思考中....");
            // 发送请求并获取响应对象
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            //如果响应状态码为成功 200
            if (response.statusCode() == 200) {
//                System.out.println("响应体：");
//                System.out.println(response.body());
                // 解析响应 把响应体中的json字符串转为 ChatResponse 对象
                ChatResponse chatResponse = gson.fromJson(response.body(), ChatResponse.class);
                //按 JSON 格式的方式 从自定义的ChatResponse 对象中逐级取出最终的响应对象
                return chatResponse.getChoices().get(0).getMessage().getContent();
            } else {
                return "请求失败，状态码: " + response.statusCode() + ", 响应: " + response.body();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "请求异常: " + e.getMessage();
        }
    }

    // 内部类定义请求/响应结构
    static class Message {
        private String role;
        private String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getContent() {
            return content;
        }
    }

    /**
     * 请求体的数据内部结构，用于构建请求 JSON
     */
    static class ChatRequest {
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
     * 按着响应的JSON格式来设计响应体
     * 用来封装响应体字符串
     */
    static class ChatResponse {
        private List<Choice> choices;

        public List<Choice> getChoices() {
            return choices;
        }

        static class Choice {
            private Message message;

            public Message getMessage() {
                return message;
            }
        }
    }
}