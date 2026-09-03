package com.chenxiaofei.coursescheduleserver.dashboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 机构/家教明细行结清状态更新请求。
 */
@Data
public class SettlementUpdateRequest {

    @NotBlank(message = "结算月份不能为空")
    private String settleMonth;

    @NotBlank(message = "行类型不能为空")
    private String targetType;

    @NotBlank(message = "行键不能为空")
    private String targetKey;

    @NotNull(message = "结清状态不能为空")
    private Boolean settled;
}