package com.chenxiaofei.coursescheduleserver.course.dto;

import lombok.Data;

/**
 * 整周复制结果：复制数 + 各类跳过数，供前端给出「已复制 N 节，跳过 M 节」提示。
 */
@Data
public class CopyWeekResult {

    /** 实际复制成功的节数 */
    private int copied;

    /** 跳过：带重复规则的课（自身会按规则顺延，不参与整周复制） */
    private int skippedRepeat;

    /** 跳过：目标时段已有课（避免整周重叠） */
    private int skippedConflict;

    public int totalSkipped() {
        return skippedRepeat + skippedConflict;
    }
}
