package com.chenxiaofei.coursescheduleserver.dashboard.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 收入/统计相关查询
 */
@Mapper
public interface StatMapper {

    BigDecimal sumIncome(@Param("userId") Long userId, @Param("start") LocalDateTime start,
                         @Param("end") LocalDateTime end);

    long sumMinutes(@Param("userId") Long userId, @Param("start") LocalDateTime start,
                    @Param("end") LocalDateTime end);

    long countCourses(@Param("userId") Long userId, @Param("start") LocalDateTime start,
                      @Param("end") LocalDateTime end);

    List<Map<String, Object>> incomeByDay(@Param("userId") Long userId, @Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);

    List<Map<String, Object>> incomeByOrganization(@Param("userId") Long userId, @Param("start") LocalDateTime start,
                                                   @Param("end") LocalDateTime end);

    List<Map<String, Object>> incomeByStudent(@Param("userId") Long userId, @Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end);

    List<Map<String, Object>> incomeByStage(@Param("userId") Long userId, @Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);

    List<Map<String, Object>> feeDetailByStudent(@Param("userId") Long userId, @Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end);

    List<Map<String, Object>> feeDetailByOrganization(@Param("userId") Long userId, @Param("start") LocalDateTime start,
                                                      @Param("end") LocalDateTime end);
}
