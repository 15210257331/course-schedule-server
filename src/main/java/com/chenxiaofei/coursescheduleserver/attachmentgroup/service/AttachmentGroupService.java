package com.chenxiaofei.coursescheduleserver.attachmentgroup.service;

import com.chenxiaofei.coursescheduleserver.attachment.service.AttachmentService;
import com.chenxiaofei.coursescheduleserver.attachmentgroup.dto.AttachmentGroupRequest;
import com.chenxiaofei.coursescheduleserver.attachmentgroup.entity.AttachmentGroup;
import com.chenxiaofei.coursescheduleserver.attachmentgroup.mapper.AttachmentGroupMapper;
import com.chenxiaofei.coursescheduleserver.common.BusinessException;
import com.chenxiaofei.coursescheduleserver.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 附件分组：用户自定义的附件归类容器，支持增删改。
 */
@Service
public class AttachmentGroupService {

    private final AttachmentGroupMapper mapper;
    private final AttachmentService attachmentService;

    public AttachmentGroupService(AttachmentGroupMapper mapper, AttachmentService attachmentService) {
        this.mapper = mapper;
        this.attachmentService = attachmentService;
    }

    public List<AttachmentGroup> list() {
        return mapper.listByUser(UserContext.getUserId());
    }

    public AttachmentGroup get(Long id) {
        AttachmentGroup group = mapper.findById(id, UserContext.getUserId());
        if (group == null) {
            throw new BusinessException(404, "分组不存在");
        }
        return group;
    }

    public AttachmentGroup create(AttachmentGroupRequest request) {
        Long userId = UserContext.getUserId();
        String name = request.getName().trim();
        if (mapper.countByName(userId, name, null) > 0) {
            throw new BusinessException(400, "分组名称已存在");
        }
        AttachmentGroup group = new AttachmentGroup();
        group.setUserId(userId);
        group.setName(name);
        mapper.insert(group);
        return get(group.getId());
    }

    public AttachmentGroup update(Long id, AttachmentGroupRequest request) {
        get(id);
        Long userId = UserContext.getUserId();
        String name = request.getName().trim();
        if (mapper.countByName(userId, name, id) > 0) {
            throw new BusinessException(400, "分组名称已存在");
        }
        AttachmentGroup group = new AttachmentGroup();
        group.setId(id);
        group.setUserId(userId);
        group.setName(name);
        mapper.update(group);
        return get(id);
    }

    /** 删除分组：其下附件一并删除（用户已确认） */
    @Transactional
    public void delete(Long id) {
        get(id);
        Long userId = UserContext.getUserId();
        attachmentService.deleteByGroup(id);
        mapper.delete(id, userId);
    }
}