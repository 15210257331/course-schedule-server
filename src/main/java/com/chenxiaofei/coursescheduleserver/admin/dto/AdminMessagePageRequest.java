package com.chenxiaofei.coursescheduleserver.admin.dto;

import com.chenxiaofei.coursescheduleserver.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 消息分页查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AdminMessagePageRequest extends PageRequest {

    /** 类型筛选：announcement/activity/notice，空为全部 */
    private String type;

    /** 状态筛选：published/revoked，空为全部 */
    private String status;

    /** 标题关键词 */
    private String keyword;
}
