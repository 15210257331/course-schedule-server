package com.chenxiaofei.coursescheduleserver.organization.mapper;

import com.chenxiaofei.coursescheduleserver.organization.entity.Organization;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrganizationMapper {

    List<Organization> listByUser(@Param("userId") Long userId);

    Organization findById(@Param("id") Long id, @Param("userId") Long userId);

    int insert(Organization org);

    int update(Organization org);

    int delete(@Param("id") Long id, @Param("userId") Long userId);
}
