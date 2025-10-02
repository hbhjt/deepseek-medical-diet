package com.mapper;

import com.pojo.HealthProfile;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HealthProfileMapper {
    /**
     * 插入用户健康画像
     * @param profile 健康画像对象
     */
    void insertHealthProfile(HealthProfile profile);

}