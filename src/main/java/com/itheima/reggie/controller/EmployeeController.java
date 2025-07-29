package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Employee;
import com.itheima.reggie.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/employee")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    // 请求参数是json格式
    @PostMapping
    public R<String> addEmployee(@RequestBody Employee employee) {
        employee.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes()));
        // 通过 MyMetaObjectHandler 公共字段自动填充
        // employee.setCreateTime(LocalDateTime.now());
        // employee.setUpdateTime(LocalDateTime.now());
        // Long empId = (Long) request.getSession().getAttribute("employee");
        // employee.setCreateUser(empId);
        // employee.setUpdateUser(empId);
        employeeService.save(employee);
        return R.success("添加成功");
    }

    @PutMapping
    // 由于id为Long类型，导致后端(19位)传前端(16位后的四舍五入)精度缺失，
    // 使用JacksonObjectMapper对象映射器，在后端传送时将java对象转为json，前端接受到的就是字符串形式的id
    public R<String> update(@RequestBody Employee employee) {
        // 公共字段自动填充
        // employee.setUpdateTime(LocalDateTime.now());
        // employee.setUpdateUser((Long) request.getSession().getAttribute("employee"));
        employeeService.updateById(employee);
        return R.success("修改状态成功");
    }

    // get路径变量查询 - /example/131231312
    @GetMapping("/{id}")
    public R<Employee> updateEmployeeInfo(@PathVariable Long id) {
        Employee employee = employeeService.getById(id);
        if (employee != null) return R.success(employee);
        return R.error("没有查询到");
    }

    // 请求参数是json格式
    @PostMapping("/login")
    public R<Employee> login(HttpServletRequest request, @RequestBody Employee employee) {
        // 对密码进行md5加密
        String password = employee.getPassword();
        password = DigestUtils.md5DigestAsHex(password.getBytes());

        // 查询数据库
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Employee::getUsername, employee.getUsername());
        Employee emp = employeeService.getOne(queryWrapper);

        // 没有查询到，返回失败
        if (emp == null) return R.error("未查询到用户");

        // 密码错误，返回失败
        if (!emp.getPassword().equals(password)) return R.error("密码错误");

        // 员工状态禁用，返回账号已禁用
        if (emp.getStatus() == 0) return R.error("账号已禁用");

        // 登录成功，id存入Session，返回成功结果
        request.getSession().setAttribute("employee", emp.getId());
        return R.success(emp);
    }

    @PostMapping("/logout")
    public R<String> logout(HttpServletRequest request) {
        request.getSession().removeAttribute("employee");
        return R.success("成功退出");
    }

    // get查询参数查询 - /example?page=xx&name=xx
    @GetMapping("/page")
    public R<Page<Employee>> page(int page, int pageSize, String name) {
        // 构造分页构造器
        Page<Employee> pageInfo = new Page<>(page, pageSize);

        // 构造条件构造器
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        // 添加条件过滤
        queryWrapper.like(StringUtils.isNotEmpty(name), Employee::getName, name);
        // 添加排序条件
        queryWrapper.orderByDesc(Employee::getUpdateTime);

        // 执行查询
        employeeService.page(pageInfo, queryWrapper);
        return R.success(pageInfo);
    }
}
