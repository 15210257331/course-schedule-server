package com.chenxiaofei.coursescheduleserver.dashboard.mapper;

import com.chenxiaofei.coursescheduleserver.dashboard.entity.Settlement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 机构/家教明细行结清状态存取。
 */
@Mapper
public interface SettlementMapper {

    List<Settlement> listByMonth(@Param("userId") Long userId, @Param("settleMonth") String settleMonth);

    /** 插入或更新结清状态（幂等 upsert） */
    void upsert(Settlement settlement);
}