package com.chenxiaofei.coursescheduleserver.salaryrule.service;

import com.chenxiaofei.coursescheduleserver.common.BusinessException;
import com.chenxiaofei.coursescheduleserver.salaryrule.dto.SalaryRuleRequest;
import com.chenxiaofei.coursescheduleserver.salaryrule.entity.SalaryRule;
import com.chenxiaofei.coursescheduleserver.salaryrule.mapper.SalaryRuleMapper;
import com.chenxiaofei.coursescheduleserver.security.UserContext;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SalaryRuleService {

    private final SalaryRuleMapper mapper;

    public SalaryRuleService(SalaryRuleMapper mapper) {
        this.mapper = mapper;
    }

    public List<SalaryRule> list() {
        return mapper.listByUser(UserContext.getUserId());
    }

    public SalaryRule create(SalaryRuleRequest request) {
        SalaryRule r = new SalaryRule();
        r.setUserId(UserContext.getUserId());
        apply(r, request);
        mapper.insert(r);
        return mapper.listByUser(UserContext.getUserId()).stream()
                .filter(x -> x.getId().equals(r.getId())).findFirst().orElse(r);
    }

    public SalaryRule update(Long id, SalaryRuleRequest request) {
        SalaryRule r = new SalaryRule();
        r.setId(id);
        r.setUserId(UserContext.getUserId());
        apply(r, request);
        mapper.update(r);
        return mapper.listByUser(UserContext.getUserId()).stream()
                .filter(x -> x.getId().equals(id)).findFirst().orElse(r);
    }

    public void delete(Long id) {
        mapper.delete(id, UserContext.getUserId());
    }

    private void apply(SalaryRule r, SalaryRuleRequest request) {
        r.setOrganizationId(request.getOrganizationId());
        r.setGrade(request.getGrade());
        r.setSubject(request.getSubject());
        r.setHourlyFee(request.getHourlyFee());
        r.setRemark(request.getRemark());
    }
}