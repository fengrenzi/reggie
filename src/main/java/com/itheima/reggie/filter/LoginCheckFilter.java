package com.itheima.reggie.filter;


import com.alibaba.fastjson.JSON;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;

/*
 * 检测是否登录
 * */
@WebFilter(filterName = "loginCheckFilter", urlPatterns = "/*")
@Slf4j
public class LoginCheckFilter implements Filter {
    // 路径匹配器，可使用通配符
    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    public boolean check(String[] urls, String requestURL) {
        for (String url : urls) {
            boolean match = PATH_MATCHER.match(url, requestURL);
            if (match) {
                return true;
            }
        }
        return false;
    }

    public String getFirstPathSegment(String path) {
        if (path == null || path.isEmpty()) return "";

        // 找到第一个斜杠的位置
        int firstSlashIndex = path.startsWith("/") ? 1 : 0;
        int nextSlashIndex = path.indexOf("/", firstSlashIndex);

        // 如果没有后续斜杠，返回整个字符串（去除开头的斜杠）
        if (nextSlashIndex == -1) {
            return path.startsWith("/") ? path.substring(1) : path;
        }

        // 截取第一个斜杠后的部分
        return path.substring(firstSlashIndex, nextSlashIndex);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        // 获取请求的url
        String requestURL = request.getRequestURI();
        // 过滤不需要处理的请求路径
        String[] urls = new String[]{
                "/employee/login",
                "/employee/logout",
                "/backend/**", // **通配符匹配搭配上面 AntPathMatcher 使用，匹配所有以/backend/开头的路径
                "/front/**",
                "/common/**",
                "/user/sendMsg",
                "/user/login"
        };

        // 判断请求是否是需要处理
        boolean check = check(urls, requestURL);
        if (check) {
            filterChain.doFilter(request, response);
            return;
        }
        if (request.getSession().getAttribute("employee") != null) {
            Long empId = (Long) request.getSession().getAttribute("employee");
            BaseContext.setCurrentId(empId);
            filterChain.doFilter(request, response);
            return;
        }
        if (request.getSession().getAttribute("user") != null) {
            Long userId = (Long) request.getSession().getAttribute("user");
            BaseContext.setCurrentId(userId);
            filterChain.doFilter(request, response);
            return;
        }
        // 没有登录,通过输出流返回数据
        response.getWriter().write(JSON.toJSONString(R.error("NOTLOGIN")));
        return;
    }
}
