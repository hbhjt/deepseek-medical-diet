package com.service;

import com.pojo.User;

public interface UserService {
    /**
     * 用户登录
     * @param user
     * @return
     */
    public User login(User user);
}