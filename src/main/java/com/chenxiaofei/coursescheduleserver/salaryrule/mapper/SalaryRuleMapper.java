package com.chenxiaofei.coursescheduleserver.salaryrule.mapper;

import com.chenxiaofei.coursescheduleserver.salaryrule.entity.SalaryRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SalaryRuleMapper {

    List<SalaryRule> listByUser(@Param("userId") Long userId);

    SalaryRule match(@Param("userId") Long userId, @Param("organizationId") Long organizationId,
                     @Param("grade") String grade, @Param("subject") String subject);

    int insert(SalaryRule r);

    int update(SalaryRule r);

    int delete(@Param("id") Long id, @Param("userId") Long userId);
}
