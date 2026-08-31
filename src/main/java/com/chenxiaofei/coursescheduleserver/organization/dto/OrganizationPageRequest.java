package com.chenxiaofei.coursescheduleserver.organization.dto;

import lombok.Data;

/**
 * 机构分页查询参数（POST body）
 */
@Data
public class OrganizationPageRequest {

    private Integer pageNum = 1;
    private Integer pageSize = 20;

    /** 机构名称模糊查询 */
    private String name;
}