package com.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mapper.HealthProfileMapper;
import com.mapper.RecipeMapper;
import com.pojo.HealthProfile;
import com.pojo.MedicinalDiet;
import com.util.DeepSeekClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class MedicinalDietService {
    private static final Logger log = LoggerFactory.getLogger(MedicinalDietService.class);

    @Autowired
    private HealthProfileMapper healthProfileMapper;

    @Autowired
    private RecipeMapper recipeMapper;

    @Autowired
    private DeepSeekClient deepSeekClient;

    @Autowired
    private ObjectMapper objectMapper;

    // 最大步骤长度限制
    private static final int MAX_STEPS_LENGTH = 500;

    /**
     * 接收用户健康画像，生成并保存药膳推荐（完整流程）
     */
    @Transactional(rollbackFor = Exception.class)
    public MedicinalDiet recommendAndSave(HealthProfile profile) {
        try {
            // 1. 保存用户健康画像到数据库
            healthProfileMapper.insertHealthProfile(profile);
            log.info("健康画像保存成功，ID:{}", profile.getUserId());

            // 2. 转换健康画像为AI所需的参数格式
            DeepSeekClient.UserHealthInfo userInfo = convertToUserHealthInfo(profile);

            // 3. 调用AI生成药膳推荐
            DeepSeekClient.MedicinalDiet aiDietResult = deepSeekClient.generateDietRecommendation(userInfo);
            if (aiDietResult == null) {
                throw new RuntimeException("AI返回空结果");
            }

            // 4. 验证并转换AI返回的MedicinalDiet
            MedicinalDiet businessDiet = convertAiDietToBusinessDiet(aiDietResult);

            // 5. 补充业务层MedicinalDiet的额外字段
            businessDiet.setType(0);
            businessDiet.setCreateTime(LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
            businessDiet.setIntro("根据您的健康状况智能推荐的药膳");
            businessDiet.setIngredients(String.join("、", aiDietResult.getIngredients()));

            // 6. 保存药膳到数据库
            recipeMapper.insertRecipe(businessDiet);
            log.info("药膳推荐保存成功，ID:{}", businessDiet.getId());

            return businessDiet;
        } catch (Exception e) {
            log.error("药膳推荐生成失败，健康画像ID:{}", profile.getUserId(), e);
            throw new RuntimeException("药膳推荐生成失败：" + e.getMessage(), e);
        }
    }

    /**
     * 转换：业务层HealthProfile → AI工具类的UserHealthInfo
     */
    private DeepSeekClient.UserHealthInfo convertToUserHealthInfo(HealthProfile profile) {
        // 1. 解析症状
        String symptoms = parseJsonTags(profile.getSymptoms());

        // 2. 转换性别
        String gender = profile.getGender() == 1 ? "男" : "女";

        // 3. 构建其他健康状况
        String otherConditions = buildOtherConditions(profile);

        return new DeepSeekClient.UserHealthInfo(
                symptoms,
                gender,
                profile.getAge(),
                otherConditions
        );
    }

    /**
     * 转换：AI返回的MedicinalDiet → 业务层的MedicinalDiet
     */
    private MedicinalDiet convertAiDietToBusinessDiet(DeepSeekClient.MedicinalDiet aiDiet) {
        // 验证AI返回结果的关键信息
        if (aiDiet == null || !StringUtils.hasText(aiDiet.getName()) ||
                aiDiet.getSteps() == null || aiDiet.getSteps().isEmpty()) {
            throw new IllegalArgumentException("AI返回的药膳信息不完整");
        }

        MedicinalDiet businessDiet = new MedicinalDiet();
        businessDiet.setName(aiDiet.getName());
        businessDiet.setEffect(aiDiet.getReason());

        // 处理步骤，限制长度
        String steps = String.join("\n", aiDiet.getSteps());
        if (steps.length() > MAX_STEPS_LENGTH) {
            steps = steps.substring(0, MAX_STEPS_LENGTH) + "...";
        }
        businessDiet.setMethod(steps);

        return businessDiet;
    }

    /**
     * 辅助：解析JSON格式的标签（使用Jackson库）
     */
    private String parseJsonTags(String json) {
        if (!StringUtils.hasText(json)) {
            return "无";
        }
        try {
            List<String> tags = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            return (tags == null || tags.isEmpty()) ? "无" : String.join("、", tags);
        } catch (Exception e) {
            log.error("解析JSON标签失败，原始JSON:{}", json, e);
            return "无（格式异常）";
        }
    }

    /**
     * 辅助：构建"其他健康状况"描述
     */
    private String buildOtherConditions(HealthProfile profile) {
        List<String> conditions = new ArrayList<>();

        // 血压
        if (profile.getBloodPressure() != null && profile.getBloodPressure() == 1) {
            conditions.add("高血压");
        }
        // 血糖
        if (profile.getBloodSugar() != null && profile.getBloodSugar() == 1) {
            conditions.add("高血糖");
        }
        // 疾病史
        String diseases = parseJsonTags(profile.getDiseases());
        if (!"无".equals(diseases) && !"无（格式异常）".equals(diseases)) {
            conditions.add(diseases);
        }

        return conditions.isEmpty() ? "无" : String.join("、", conditions);
    }
}
