package com.service.impl;

import com.mapper.UserMapper;
import com.pojo.User;
import com.service.UserService;

import lombok.extern.slf4j.Slf4j;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;



@Service
@Slf4j
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;

    @Override
    public User login(User user) {
        //调用dao层功能：登录
        User loginUser = userMapper.getByUsernameAndPassword(user);

        //返回查询结果给Controller
        return loginUser;
    }
}