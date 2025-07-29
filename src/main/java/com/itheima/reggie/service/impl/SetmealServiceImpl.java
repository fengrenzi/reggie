package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.entity.SetmealDish;
import com.itheima.reggie.mapper.SetmealMapper;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {
    @Autowired
    private SetmealDishService setmealDishService;

    @Override
    @Transactional
    public void saveSetmealWithSetmealDish(SetmealDto setmealDto) {
        //增加Setmeal
        this.save(setmealDto);
        //增加SetmealDish
        //1.获取SetmealDishs
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        //2.SetmealDishs批量添加setmealid
        List<SetmealDish> collect = setmealDishes.stream().map((item) -> {
            item.setSetmealId(setmealDto.getId());
            return item;
        }).collect(Collectors.toList());
        setmealDishService.saveBatch(collect);
    }

    @Override
    @Transactional
    public SetmealDto getByIdWithSetmealDish(Long id) {
        SetmealDto setmealDto = new SetmealDto();
        // 获取setmeal
        BeanUtils.copyProperties(this.getById(id), setmealDto);
        // 获取setmealDish
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId, id);
        List<SetmealDish> setmealDishs = setmealDishService.list(queryWrapper);
        setmealDto.setSetmealDishes(setmealDishs);
        return setmealDto;
    }

    @Override
    @Transactional
    public void updateWithSetmealDish(SetmealDto setmealDto) {
        // 更新Setmeal
        this.updateById(setmealDto);
        // 更新SetmealDish
        // 1.删除原关联Setmeal-id的SetmealDish
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId, setmealDto.getId());
        setmealDishService.remove(queryWrapper);
        // 2.获取SetmealDish
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        // 3.插入新的SetmealDish
        setmealDishes = setmealDishes.stream().map((item) -> {
            item.setSetmealId(setmealDto.getId());
            return item;
        }).collect(Collectors.toList());
        setmealDishService.saveBatch(setmealDishes);
    }

    @Override
    @Transactional
    public void removeSetmealWithSetmealDish(List<Long> ids) {
        // 判断是否在售卖中，售卖的不可删除
        LambdaQueryWrapper<Setmeal> queryWrapper1 = new LambdaQueryWrapper<>();
        queryWrapper1.in(Setmeal::getId, ids);
        queryWrapper1.eq(Setmeal::getStatus, 1);
        int count = this.count(queryWrapper1);
        if (count > 0) throw new CustomException("套餐在售卖中，不可删除");
        // 根据setmeal_id批量删除SetmealDish
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(SetmealDish::getSetmealId, ids);
        setmealDishService.remove(queryWrapper);
        // 根据setmeal_id删除Setmeal
        this.removeByIds(ids);
    }
}
