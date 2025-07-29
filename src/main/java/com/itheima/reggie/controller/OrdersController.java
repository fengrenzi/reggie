package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.OrdersDto;
import com.itheima.reggie.entity.OrderDetail;
import com.itheima.reggie.entity.Orders;
import com.itheima.reggie.service.OrderDetailService;
import com.itheima.reggie.service.OrdersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@RestController
@RequestMapping("/order")
public class OrdersController {
    @Autowired
    private OrdersService ordersService;
    @Autowired
    private OrderDetailService orderDetailService;


    @PostMapping("/submit")
    public R<String> submitOrder(@RequestBody Orders orders) {
        ordersService.saveOrder(orders);
        return R.success("提交成功");
    }

    @GetMapping("/userPage")
    public R<Page<OrdersDto>> page(int page, int pageSize) {
        // 获取用户所有Orders
        Page<Orders> ordersPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<Orders> OrdersQueryWrapper = new LambdaQueryWrapper<>();
        OrdersQueryWrapper.eq(Orders::getUserId, BaseContext.getCurrentId());
        OrdersQueryWrapper.orderByDesc(Orders::getCheckoutTime);
        ordersService.page(ordersPage, OrdersQueryWrapper);
        // 结合OrderDetail生成新OrderDto
        Page<OrdersDto> ordersDtoPage = new Page<>();
        BeanUtils.copyProperties(ordersPage, ordersDtoPage, "records");
        List<Orders> records = ordersPage.getRecords();
        List<OrdersDto> ordersRecords = records.stream().map((item) -> {
            OrdersDto ordersDto = new OrdersDto();
            BeanUtils.copyProperties(item, ordersDto);
            LambdaQueryWrapper<OrderDetail> orderDetailQueryWrapper = new LambdaQueryWrapper<>();
            orderDetailQueryWrapper.eq(OrderDetail::getOrderId, item.getNumber());
            List<OrderDetail> orderDetails = orderDetailService.list(orderDetailQueryWrapper);
            ordersDto.setOrderDetails(orderDetails);
            return ordersDto;
        }).collect(Collectors.toList());
        ordersDtoPage.setRecords(ordersRecords);
        return R.success(ordersDtoPage);
    }
}
