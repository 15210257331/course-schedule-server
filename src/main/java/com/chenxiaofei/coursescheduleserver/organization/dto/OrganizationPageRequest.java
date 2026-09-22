package com.chenxiaofei.coursescheduleserver.organization.dto;

import com.chenxiaofei.coursescheduleserver.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 机构分页查询参数（POST body）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrganizationPageRequest extends PageRequest {

    /** 机构名称模糊查询 */
    private String name;
}