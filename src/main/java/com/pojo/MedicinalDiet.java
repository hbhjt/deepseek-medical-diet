package com.pojo;

import java.time.LocalDateTime;

/**
 * 业务层药膳实体类（与数据库表对应）
 */
public class MedicinalDiet {
    private Long id;             // 主键ID
    private String name;         // 药膳名称
    private String intro;        // 药膳简介
    private String ingredients;  // 制作成分（字符串，用、分隔）
    private String method;       // 制作方法（字符串，用换行分隔）
    private String effect;       // 功效说明
    private Integer type;        // 类型（0=药膳）
    private LocalDateTime createTime; // 创建时间
    private Integer isValid;     // 是否有效（1=有效/0=无效）

    // 无参构造
    public MedicinalDiet() {}

    // Getter和Setter（完整生成）
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIntro() { return intro; }
    public void setIntro(String intro) { this.intro = intro; }

    public String getIngredients() { return ingredients; }
    public void setIngredients(String ingredients) { this.ingredients = ingredients; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getEffect() { return effect; }
    public void setEffect(String effect) { this.effect = effect; }

    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public Integer getIsValid() { return isValid; }
    public void setIsValid(Integer isValid) { this.isValid = isValid; }
}