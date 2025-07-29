package com.itheima.reggie.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

/**
 * 用户信息
 */
@Data
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    private String phone;
    //性别 0 女 1 男
    private String sex;
    private String idNumber;
    private String avatar;
    //状态 0:禁用，1:正常
    private Integer status;
}
