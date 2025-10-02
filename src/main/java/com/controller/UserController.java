package com.controller;


import com.pojo.Result;
import com.pojo.User;
import com.service.UserService;
import com.util.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public Result login(@RequestBody User user){
        //调用业务层：登录功能
        User loginUser = userService.login(user);

        //判断：登录用户是否存在
        if(loginUser !=null ){
            //自定义信息
            Map<String , Object> claims = new HashMap<>();
            claims.put("user_id", loginUser.getUserId());
            claims.put("nickname",loginUser.getNickname());

            //使用JWT工具类，生成身份令牌
            String token = JwtUtils.generateJwt(claims);
            return Result.success(token);
        }
        return Result.error("用户名或密码错误");
    }
}
