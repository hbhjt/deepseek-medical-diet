package com.mapper;

import com.pojo.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {
    @Select("select user_id, nickname, password,created_time, updated_time " +
            "from t_user " +
            "where nickname=#{nickname} and password =#{password}")
    public User getByUsernameAndPassword(User user);
}
