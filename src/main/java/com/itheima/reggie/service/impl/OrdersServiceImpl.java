package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.*;
import com.itheima.reggie.mapper.OrdersMapper;
import com.itheima.reggie.service.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


@Service
public class OrdersServiceImpl extends ServiceImpl<OrdersMapper, Orders> implements OrdersService {
    @Autowired
    private ShoppingCartService shoppingCartService;
    @Autowired
    private AddressBookService addressBookService;
    @Autowired
    private UserService userService;
    @Autowired
    private OrderDetailService orderDetailService;

    @Transactional
    @Override
    public void saveOrder(Orders orders) {
        // 设置订单号
        Long orderId = IdWorker.getId();
        orders.setNumber(String.valueOf(orderId));
        // 设置订单状态为1
        orders.setStatus(2);
        // 设置用户id
        orders.setUserId(BaseContext.getCurrentId());
        // 设订单时间、结账时间
        orders.setOrderTime(LocalDateTime.now());
        orders.setCheckoutTime(LocalDateTime.now());
        // 设置实付金额，根据用户id查询套餐
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());
        List<ShoppingCart> shops = shoppingCartService.list(queryWrapper);
        if (shops == null) throw new CustomException("没有订单");
        AtomicInteger amount = new AtomicInteger(0);

        // 根据用户地址id获取用户名、手机号、地址、收货人
        User user = userService.getById(BaseContext.getCurrentId());
        LambdaQueryWrapper<AddressBook> queryWrapper1 = new LambdaQueryWrapper<>();
        queryWrapper1.eq(AddressBook::getId, orders.getAddressBookId());
        AddressBook address = addressBookService.getOne(queryWrapper1);
        if (address == null) throw new CustomException("地址有误");
        orders.setUserName(user.getName());
        orders.setPhone(address.getPhone());
        orders.setAddress(address.getDetail());
        orders.setConsignee(address.getConsignee());

        // orders_detail表插入数据
        List<OrderDetail> orderDetails = shops.stream().map((item) -> {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(item, orderDetail, "id");
            orderDetail.setOrderId(orderId);
            amount.addAndGet(item.getAmount().multiply(BigDecimal.valueOf(item.getNumber())).intValue());
            return orderDetail;
        }).collect(Collectors.toList());
        orders.setAmount(new BigDecimal(amount.get()));
        orderDetailService.saveBatch(orderDetails);
        // orders表插入数据
        this.save(orders);
        // 删除用户购物车订单数据
        shoppingCartService.remove(queryWrapper);
    }
}
