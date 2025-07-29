package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.mapper.CategoryMapper;
import com.itheima.reggie.mapper.DishMapper;
import com.itheima.reggie.mapper.SetmealMapper;
import com.itheima.reggie.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private DishMapper dishMapper;

    @Override
    public void remove(Long id) { // 也可返回R对象
        // 该分类是否关联菜品，关联则报错
        LambdaQueryWrapper<Dish> queryWrapper_dish = new LambdaQueryWrapper<>();
        queryWrapper_dish.eq(Dish::getCategoryId, id);
        int dish = dishMapper.selectCount(queryWrapper_dish);
        if (dish > 0) throw new CustomException("关联菜品，不可删除");
        // 该分类是否关联套餐，关联则报错
        LambdaQueryWrapper<Setmeal> queryWrapper_setmeal = new LambdaQueryWrapper<>();
        queryWrapper_setmeal.eq(Setmeal::getCategoryId, id);
        int setmeal = setmealMapper.selectCount(queryWrapper_setmeal);
        if (setmeal > 0) throw new CustomException("关联套餐，不可删除");
        // 删除分类
        super.removeById(id);
    }
}
