package com.mapper;

import com.pojo.MedicinalDiet;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RecipeMapper {
    /**
     * 插入完整的药膳推荐记录
     * @param diet 包含所有字段的药膳对象
     */
    void insertRecipe(MedicinalDiet diet);
}