package com.mapper;

import com.pojo.MedicinalDiet;
import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface RecipeMapper {
    /**
     * 插入药膳推荐记录
     * @param diet 药膳对象
     */
    void insertRecipe(MedicinalDiet diet);


}