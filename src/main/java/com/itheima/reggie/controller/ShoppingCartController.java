package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.ShoppingCart;
import com.itheima.reggie.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/shoppingCart")
public class ShoppingCartController {
    @Autowired
    private ShoppingCartService shoppingCartService;

    @GetMapping("/list")
    public R<List<ShoppingCart>> list() {
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());
        queryWrapper.orderByDesc(ShoppingCart::getCreateTime);
        List<ShoppingCart> list = shoppingCartService.list(queryWrapper);
        return R.success(list);
    }

    // 添加商品到购物车
    @PostMapping("/add")
    public R<ShoppingCart> addShoppingCart(@RequestBody ShoppingCart shoppingCart) {
        // 设置用户Id
        Long currentId = BaseContext.getCurrentId();
        shoppingCart.setUserId(currentId);
        // 当前菜品/套餐是否存在
        // 根据dishId判断shoppingCart是否已经存在相同数据存在就number+1，否则新建
        Long dishId = shoppingCart.getDishId();
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, currentId);
        if (dishId != null) {
            queryWrapper.eq(ShoppingCart::getDishId, dishId);
        } else {
            queryWrapper.eq(ShoppingCart::getSetmealId, shoppingCart.getSetmealId());
        }
        ShoppingCart cartServiceOne = shoppingCartService.getOne(queryWrapper);

        if (cartServiceOne != null) {
            cartServiceOne.setNumber(cartServiceOne.getNumber() + 1);
            shoppingCartService.updateById(cartServiceOne);
        } else {
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCart.setNumber(1);
            shoppingCartService.save(shoppingCart);
            cartServiceOne = shoppingCart;
        }
        return R.success(cartServiceOne);
    }

    // 清空购物车
    @DeleteMapping("/clean")
    public R<String> deleteShoppingCart() {
        // 清空对应用户的购物车商品
        // 获取用户ID后批量删除
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());
        shoppingCartService.remove(queryWrapper);
        return R.success("成功");
    }

    // 减少购物车商品
    @PostMapping("/sub")
    public R<ShoppingCart> subShoppingCart(@RequestBody ShoppingCart shoppingCart) {
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        if (shoppingCart.getDishId() != null) {
            // 根据dishId获取对应菜品
            queryWrapper.eq(ShoppingCart::getDishId, shoppingCart.getDishId());
        } else {
            // 根据setmealId获取对应套餐
            queryWrapper.eq(ShoppingCart::getSetmealId, shoppingCart.getSetmealId());
        }
        ShoppingCart shop = shoppingCartService.getOne(queryWrapper);
        // number-1，如果数量为1则删除该商品
        shop.setNumber(shop.getNumber() - 1);
        if (shop.getNumber() <= 0) shoppingCartService.remove(queryWrapper);
        else shoppingCartService.updateById(shop);
        return R.success(shop);
    }
}
