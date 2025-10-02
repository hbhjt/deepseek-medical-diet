package com.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;


@Slf4j
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthProfile {
    private Long profileId;       // 健康画像ID
    private Long userId;          // 用户ID
    private Integer age;          // 年龄
    private Integer gender;       // 性别 1=男 0=女
    private Integer bloodPressure;// 血压 -1=低 0=正常 1=高
    private Integer bloodSugar;   // 血糖 -1=低 0=正常 1=高
    private String symptoms;      // JSON字符串，症状标签
    private String diseases;      // JSON字符串，疾病史
    private LocalDateTime createdTime; // 填写时间
}