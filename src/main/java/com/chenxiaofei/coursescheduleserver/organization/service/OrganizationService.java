package com.chenxiaofei.coursescheduleserver.organization.service;

import com.chenxiaofei.coursescheduleserver.common.BusinessException;
import com.chenxiaofei.coursescheduleserver.common.PageResult;
import com.chenxiaofei.coursescheduleserver.common.Pages;
import com.chenxiaofei.coursescheduleserver.course.mapper.CourseMapper;
import com.chenxiaofei.coursescheduleserver.organization.dto.OrganizationPageRequest;
import com.chenxiaofei.coursescheduleserver.organization.dto.OrganizationRequest;
import com.chenxiaofei.coursescheduleserver.organization.entity.Organization;
import com.chenxiaofei.coursescheduleserver.organization.mapper.OrganizationMapper;
import com.chenxiaofei.coursescheduleserver.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationMapper mapper;
    private final CourseMapper courseMapper;

    public List<Organization> list() {
        return list(null);
    }

    public List<Organization> list(String name) {
        return mapper.listByUser(UserContext.getUserId(), name);
    }

    /** 分页查询（name 模糊），按 id 倒序 */
    public PageResult<Organization> page(OrganizationPageRequest req) {
        Long userId = UserContext.getUserId();
        return Pages.of(req,
                () -> mapper.countByUser(userId, req.getName()),
                (offset, limit) -> mapper.pageByUser(userId, req.getName(), offset, limit));
    }

    public Organization get(Long id) {
        Organization org = mapper.findById(id, UserContext.getUserId());
        if (org == null) {
            throw new BusinessException(404, "机构不存在");
        }
        return org;
    }

    public Organization create(OrganizationRequest request) {
        Organization org = new Organization();
        org.setUserId(UserContext.getUserId());
        apply(org, request);
        mapper.insert(org);
        return get(org.getId());
    }

    public Organization update(Long id, OrganizationRequest request) {
        get(id);
        Organization org = new Organization();
        org.setId(id);
        org.setUserId(UserContext.getUserId());
        apply(org, request);
        mapper.update(org);
        return get(id);
    }

    public void delete(Long id) {
        get(id);
        Long userId = UserContext.getUserId();
        // 存在关联课程时禁止删除，避免课程机构悬空
        if (courseMapper.countByOrganization(userId, id) > 0) {
            throw new BusinessException(400, "该机构下还有课程，无法删除");
        }
        mapper.delete(id, userId);
    }

    private void apply(Organization org, OrganizationRequest request) {
        org.setName(request.getName());
        org.setContactName(request.getContactName());
        org.setContactPhone(request.getContactPhone());
        org.setAddress(request.getAddress());
        org.setColor(request.getColor());
        org.setRemark(request.getRemark());
    }
}