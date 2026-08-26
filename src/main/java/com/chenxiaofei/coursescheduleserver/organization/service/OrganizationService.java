package com.chenxiaofei.coursescheduleserver.organization.service;

import com.chenxiaofei.coursescheduleserver.common.BusinessException;
import com.chenxiaofei.coursescheduleserver.organization.dto.OrganizationRequest;
import com.chenxiaofei.coursescheduleserver.organization.entity.Organization;
import com.chenxiaofei.coursescheduleserver.organization.mapper.OrganizationMapper;
import com.chenxiaofei.coursescheduleserver.security.UserContext;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrganizationService {

    private final OrganizationMapper mapper;

    public OrganizationService(OrganizationMapper mapper) {
        this.mapper = mapper;
    }

    public List<Organization> list() {
        return mapper.listByUser(UserContext.getUserId());
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
        mapper.delete(id, UserContext.getUserId());
    }

    private void apply(Organization org, OrganizationRequest request) {
        org.setName(request.getName());
        org.setContactName(request.getContactName());
        org.setContactPhone(request.getContactPhone());
        org.setAddress(request.getAddress());
        org.setDefaultFee(request.getDefaultFee());
        org.setRemark(request.getRemark());
    }
}