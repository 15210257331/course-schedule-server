package com.chenxiaofei.coursescheduleserver.admin.dto;

import lombok.Data;

/**
 * 消息分页查询参数
 */
@Data
public class AdminMessagePageRequest {

    private Integer pageNum = 1;
    private Integer pageSize = 20;

    /** 类型筛选：announcement/activity/notice，空为全部 */
    private String type;

    /** 状态筛选：published/revoked，空为全部 */
    private String status;

    /** 标题关键词 */
    private String keyword;
}
