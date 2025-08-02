package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.User;
import com.itheima.reggie.service.UserService;
import com.itheima.reggie.utils.SMSUtils;
import com.itheima.reggie.utils.ValidateCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpRequest;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping("/sendMsg")
    public R<String> sendMsg(@RequestBody User user, HttpSession session) {
        String phone = user.getPhone();
        if(StringUtils.isNotEmpty(phone)) {
            String code = ValidateCodeUtils.generateValidateCode(4).toString();
            // SMSUtils.sendMessage("瑞吉外卖", "", phone, code);
//            session.setAttribute(phone, code);
            // 验证码存入redis中，节约资源
            redisTemplate.opsForValue().set(phone, code, 5, TimeUnit.MINUTES);
            System.out.println(redisTemplate.opsForValue().get(phone));
            return R.success(code);
        }
        return R.error("失败");
    }

    @PostMapping("/login")
    public R<User> login(HttpServletRequest request, @RequestBody Map map, HttpSession session) {
        // 获取手机号
        String phone = map.get("phone").toString();
        // 获取验证码
        String code = map.get("code").toString();
        // 从session中获取验证码
//        Object codeInSession = session.getAttribute(phone);
        // 从redis中获取验证码
        Object codeInSession = redisTemplate.opsForValue().get(phone);
        // 验证码比对
        if (codeInSession != null && codeInSession.equals(code)) {
            // 验证码正确判断该手机号是否已注册
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getPhone, phone);
            User user = userService.getOne(queryWrapper);
            if (user == null) {
                user = new User();
                user.setPhone(phone);
                user.setStatus(1);
                userService.save(user);
            }
            if (user.getStatus() == 0) return R.error("账号已禁用");
            request.getSession().setAttribute("user", user.getId());
            // 用户登录成功，删除验证码
            redisTemplate.delete(phone);
            return R.success(user);
        }
        return R.error("验证码错误");
    }
}
