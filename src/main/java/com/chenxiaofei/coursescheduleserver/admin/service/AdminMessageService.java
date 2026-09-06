package com.chenxiaofei.coursescheduleserver.admin.service;

import com.chenxiaofei.coursescheduleserver.common.BusinessException;
import com.chenxiaofei.coursescheduleserver.common.PageResult;
import com.chenxiaofei.coursescheduleserver.admin.dto.AdminMessagePageRequest;
import com.chenxiaofei.coursescheduleserver.admin.dto.AdminMessageRequest;
import com.chenxiaofei.coursescheduleserver.admin.entity.AdminMessage;
import com.chenxiaofei.coursescheduleserver.admin.mapper.AdminMessageMapper;
import com.chenxiaofei.coursescheduleserver.auth.entity.User;
import com.chenxiaofei.coursescheduleserver.auth.mapper.UserMapper;
import com.chenxiaofei.coursescheduleserver.security.UserContext;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminMessageService {

    private final AdminMessageMapper messageMapper;
    private final UserMapper userMapper;

    public AdminMessageService(AdminMessageMapper messageMapper, UserMapper userMapper) {
        this.messageMapper = messageMapper;
        this.userMapper = userMapper;
    }

    /** 管理端分页 */
    public PageResult<AdminMessage> page(AdminMessagePageRequest req) {
        int pageNum = req.getPageNum() == null || req.getPageNum() < 1 ? 1 : req.getPageNum();
        int pageSize = req.getPageSize() == null || req.getPageSize() < 1 ? 20 : req.getPageSize();
        long offset = (long) (pageNum - 1) * pageSize;
        long total = messageMapper.countMessages(req.getType(), req.getStatus(), req.getKeyword());
        List<AdminMessage> list = messageMapper.pageMessages(req.getType(), req.getStatus(), req.getKeyword(), offset, pageSize);
        // 填充目标人数
        long teacherTotal = userMapper.countTeachers(null, null);
        for (AdminMessage m : list) {
            m.setTargetCount(resolveTargetCount(m, teacherTotal));
        }
        return PageResult.of(total, list);
    }

    public AdminMessage detail(Long id) {
        AdminMessage m = messageMapper.findById(id);
        if (m == null) {
            throw new BusinessException(404, "消息不存在");
        }
        m.setTargetCount(resolveTargetCount(m, userMapper.countTeachers(null, null)));
        return m;
    }

    public AdminMessage create(AdminMessageRequest req) {
        validate(req);
        AdminMessage m = new AdminMessage();
        apply(m, req);
        m.setStatus("published");
        m.setCreatedBy(UserContext.getUserId());
        messageMapper.insert(m);
        return messageMapper.findById(m.getId());
    }

    public AdminMessage update(Long id, AdminMessageRequest req) {
        detail(id);
        validate(req);
        AdminMessage m = new AdminMessage();
        m.setId(id);
        apply(m, req);
        messageMapper.update(m);
        return messageMapper.findById(id);
    }

    public void revoke(Long id) {
        detail(id);
        messageMapper.updateStatus(id, "revoked");
    }

    public void publish(Long id) {
        detail(id);
        messageMapper.updateStatus(id, "published");
    }

    public void delete(Long id) {
        detail(id);
        messageMapper.delete(id);
    }

    /** 教师端：可见消息列表 */
    public List<AdminMessage> listForTeacher(int limit) {
        Long userId = UserContext.getUserId();
        int l = Math.min(Math.max(limit, 1), 100);
        return messageMapper.listForTeacher(userId, l);
    }

    /** 教师端：标记已读 */
    public void markRead(Long id) {
        AdminMessage m = messageMapper.findById(id);
        if (m == null) {
            throw new BusinessException(404, "消息不存在");
        }
        messageMapper.insertRead(id, UserContext.getUserId());
    }

    /** 教师端：全部标记已读 */
    public void markAllRead() {
        messageMapper.insertReadAll(UserContext.getUserId());
    }

    // ----------

    private void validate(AdminMessageRequest req) {
        if (req.getTitle() == null || req.getTitle().isBlank()) {
            throw new BusinessException("消息标题不能为空");
        }
        if (req.getContent() == null || req.getContent().isBlank()) {
            throw new BusinessException("消息内容不能为空");
        }
        if ("specific".equals(req.getTargetType())
                && (req.getTargetIds() == null || req.getTargetIds().isEmpty())) {
            throw new BusinessException("请选择目标教师");
        }
    }

    private void apply(AdminMessage m, AdminMessageRequest req) {
        m.setTitle(req.getTitle().trim());
        m.setContent(req.getContent());
        m.setType(req.getType() == null || req.getType().isBlank() ? "announcement" : req.getType());
        String targetType = "specific".equals(req.getTargetType()) ? "specific" : "all";
        m.setTargetType(targetType);
        if ("specific".equals(targetType)) {
            // 去重后逗号拼接
            Set<Long> ids = new HashSet<>(req.getTargetIds());
            m.setTargetIds(ids.stream().map(String::valueOf).collect(Collectors.joining(",")));
        } else {
            m.setTargetIds(null);
        }
    }

    private int resolveTargetCount(AdminMessage m, long teacherTotal) {
        if ("all".equals(m.getTargetType())) {
            return (int) teacherTotal;
        }
        if (m.getTargetIds() == null || m.getTargetIds().isBlank()) {
            return 0;
        }
        return m.getTargetIds().split(",").length;
    }

    /** 管理端：简要教师列表（选择器用） */
    public List<User> teacherOptions(String keyword) {
        List<User> list = userMapper.listTeachers(keyword);
        for (User u : list) {
            u.setPassword(null);
        }
        return list;
    }
}
