package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/setmeal")
public class SetmealController {
    @Autowired
    private SetmealService setmealService;
    @Autowired
    private CategoryService categoryService;

    @DeleteMapping
    public R<String> deletes(@RequestParam List<Long> ids) {
        setmealService.removeSetmealWithSetmealDish(ids);
        return R.success("删除成功");
    }

    @PutMapping
    public R<String> update(@RequestBody SetmealDto setmealDto) {
        setmealService.updateWithSetmealDish(setmealDto);
        return R.success("修改成功");
    }

    @PostMapping
    public R<String> addSetmeal(@RequestBody SetmealDto setmealDto) {
        setmealService.saveSetmealWithSetmealDish(setmealDto);
        return R.success("添加套餐成功");
    }

    @PostMapping("/status/{status}")
    public R<String> updateStatus(@PathVariable String status, String ids) {
        List<String> setmeal = Arrays.asList(ids.split(","));
        LambdaUpdateWrapper<Setmeal> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.set(Setmeal::getStatus, status).in(Setmeal::getId, setmeal);
        setmealService.update(lambdaUpdateWrapper);
        return R.success("更新状态成功");
    }

    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name) {
        // 分页
        Page<Setmeal> pageInfo = new Page<>(page, pageSize);
        Page<SetmealDto> dtoPage = new Page<>();

        // name字段查询
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(name != null, Setmeal::getName, name);
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);
        setmealService.page(pageInfo, queryWrapper);

        // 深度复制新Page
        BeanUtils.copyProperties(pageInfo, dtoPage, "records");

        // 根据Setmeal中的category_id获取Category套餐分类
        // 获取category_id对应的category的name
        List<Setmeal> records = pageInfo.getRecords();
        List<SetmealDto> dtoRecords = records.stream().map((item) -> {
            Category category = categoryService.getById(item.getCategoryId());
            SetmealDto setmealDto = new SetmealDto();
            BeanUtils.copyProperties(item, setmealDto);
            if (category != null) {
                setmealDto.setCategoryName(category.getName());
            }
            return setmealDto;
        }).collect(Collectors.toList());
        dtoPage.setRecords(dtoRecords);
        return R.success(dtoPage);
    }

    @GetMapping("/{id}")
    public R<SetmealDto> getDishById(@PathVariable Long id) {
        return R.success(setmealService.getByIdWithSetmealDish(id));
    }

    @GetMapping("/list")
    public R<List<Setmeal>> getTypeByCategoryId(Setmeal setmeal) {
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Setmeal::getCategoryId, setmeal.getCategoryId());
        queryWrapper.eq(Setmeal::getStatus, setmeal.getStatus());
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);
        List<Setmeal> list = setmealService.list(queryWrapper);
        return R.success(list);
    }
}
