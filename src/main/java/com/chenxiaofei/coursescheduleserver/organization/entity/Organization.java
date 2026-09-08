package com.chenxiaofei.coursescheduleserver.organization.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Organization {

    private Long id;
    private Long userId;
    private String name;
    private String contactName;
    private String contactPhone;
    private String address;
    private String color;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}