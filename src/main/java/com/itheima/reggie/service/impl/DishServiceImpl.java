package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.mapper.DishMapper;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {
    @Autowired
    private DishFlavorService dishFlavorService;

    @Transactional // 多表联查，开始事务
    public void savaWithFlavor(DishDto dishDto) {
        this.save(dishDto);

        Long dishId = dishDto.getId();
        List<DishFlavor> flavors = dishDto.getFlavors();
        flavors = flavors.stream().map((item) -> {
            item.setDishId(dishId);
            return item;
        }).collect(Collectors.toList());

        dishFlavorService.saveBatch(flavors);
    }

    @Transactional
    public DishDto getByIdWithFlavor(Long id) {
        DishDto dishDto = new DishDto();
        BeanUtils.copyProperties(this.getById(id), dishDto);

        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId, id);
        List<DishFlavor> dishFlavors = dishFlavorService.list(queryWrapper);
        dishDto.setFlavors(dishFlavors);
        return dishDto;
    }

    @Transactional
    public void updateWithFlavor(DishDto dishDto) {
        // 更新dish表
        this.updateById(dishDto);

        // 更新dish_flavor表
        // 1.清除原来的数据
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId, dishDto.getId());
        dishFlavorService.remove(queryWrapper);
        // 2.重新赋值
        List<DishFlavor> flavors = dishDto.getFlavors(); // 不包含categoryName对应的categoryID
        flavors = flavors.stream().map((item) -> {
            item.setDishId(dishDto.getId());
            return item;
        }).collect(Collectors.toList());
        dishFlavorService.saveBatch(flavors);
    }

    @Transactional
    public void removeDishWithFlavor(List<Long> ids) {
        // 菜品在售卖中不可删除
        LambdaQueryWrapper<Dish> queryWrapper1 = new LambdaQueryWrapper<>();
        queryWrapper1.in(Dish::getId, ids);
        queryWrapper1.eq(Dish::getStatus, 1);
        int count = this.count(queryWrapper1);
        if (count > 0) throw new CustomException("菜品在售卖中，不可删除");
        // 根据dish_id批量删除dishFlavor
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(DishFlavor::getDishId, ids);
        dishFlavorService.remove(queryWrapper);
        // 根据dish_id删除Setmeal
        this.removeByIds(ids);
    }
}
