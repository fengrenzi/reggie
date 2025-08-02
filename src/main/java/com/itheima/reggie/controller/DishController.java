package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequestMapping("/dish")
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private DishFlavorService dishFlavorService;
    @Autowired
    private RedisTemplate redisTemplate;

    @DeleteMapping
    public R<String> deletesDish(@RequestParam List<Long> ids) {
        dishService.removeDishWithFlavor(ids);
        return R.success("删除成功");
    }

    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto) {
        dishService.updateWithFlavor(dishDto);
        // 清理缓存
        String key = "dish_" + dishDto.getCategoryId() + "_" + dishDto.getStatus();
        redisTemplate.delete(key);
        return R.success("修改成功");
    }

    @PostMapping
    public R<String> addDish(@RequestBody DishDto dishDto) {
        dishService.savaWithFlavor(dishDto);
        // 清理缓存
        String key = "dish_" + dishDto.getCategoryId() + "_" + dishDto.getStatus();
        redisTemplate.delete(key);
        return R.success("添加成功");
    }

    @PostMapping("/status/{status}")
    public R<String> updateStatus(@PathVariable String status, String ids) {
        List<String> dishs = Arrays.asList(ids.split(","));
        LambdaUpdateWrapper<Dish> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.set(Dish::getStatus, status).in(Dish::getId, dishs);
        dishService.update(lambdaUpdateWrapper);
        return R.success("更新状态成功");
    }

    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name) {
        Page<Dish> pageInfo = new Page<>(page, pageSize);
        Page<DishDto> dishDtoPage = new Page<>();

        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StringUtils.isNotEmpty(name), Dish::getName, name);
        queryWrapper.orderByDesc(Dish::getUpdateTime);
        dishService.page(pageInfo, queryWrapper);

        // 复制Page除了records其它属性到新的page中，records为List<Dish>数据
        BeanUtils.copyProperties(pageInfo, dishDtoPage, "records");
        // 获取records数据用于下面增加属性
        List<Dish> records = pageInfo.getRecords();
        // 基于records添加新属性
        List<DishDto> list = records.stream().map((item) -> {
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item, dishDto);
            Long categoryId = item.getCategoryId();
            Category category = categoryService.getById(categoryId);
            dishDto.setCategoryName(category.getName());
            return dishDto;
        }).collect(Collectors.toList());
        // 将修改records后的新数据添加到新Page后返回
        dishDtoPage.setRecords(list);
        return R.success(dishDtoPage);
    }

    @GetMapping("/{id}")
    public R<DishDto> getDishById(@PathVariable Long id) {
        return R.success(dishService.getByIdWithFlavor(id));
    }

    @GetMapping("/list")
    public R<List<DishDto>> getTypeByCategoryId(DishDto dishDto) {
        List<DishDto> dishDtos = null;
        String key = "dish_" + dishDto.getCategoryId() + "_" + dishDto.getStatus();
        dishDtos = (List<DishDto>) redisTemplate.opsForValue().get(key);
        if (dishDtos == null) {
            LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Dish::getCategoryId, dishDto.getCategoryId());
            queryWrapper.eq(Dish::getStatus, 1);
            queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
            List<Dish> dishes = dishService.list(queryWrapper);

            dishDtos = dishes.stream().map((item) -> {
                DishDto dish_dto = new DishDto();
                BeanUtils.copyProperties(item, dish_dto);
                LambdaQueryWrapper<DishFlavor> queryWrapper1 = new LambdaQueryWrapper<>();
                queryWrapper1.eq(DishFlavor::getDishId, item.getId());
                List<DishFlavor> list = dishFlavorService.list(queryWrapper1);
                dish_dto.setFlavors(list);
                return dish_dto;
            }).collect(Collectors.toList());
            redisTemplate.opsForValue().set(key, dishDtos, 60, TimeUnit.MINUTES);
        }

        return R.success(dishDtos);
    }
}
