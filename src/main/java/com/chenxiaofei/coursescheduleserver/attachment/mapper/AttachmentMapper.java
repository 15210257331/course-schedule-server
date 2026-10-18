package com.chenxiaofei.coursescheduleserver.attachment.mapper;

import com.chenxiaofei.coursescheduleserver.attachment.entity.Attachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AttachmentMapper {

    /** 某学生关联的全部课件（课件管理页 / 学生课件弹窗） */
    List<Attachment> listByStudent(@Param("userId") Long userId, @Param("studentId") Long studentId);

    /** 查询当前用户全部课件（迁移/审计用） */
    List<Attachment> listByUser(@Param("userId") Long userId);

    /** 分页统计：按学生 + 文件名关键字筛选 */
    long countByUser(@Param("userId") Long userId, @Param("studentId") Long studentId,
                     @Param("keyword") String keyword);

    /** 分页数据：按学生 + 文件名关键字筛选 */
    List<Attachment> pageByUser(@Param("userId") Long userId, @Param("studentId") Long studentId,
                                @Param("keyword") String keyword,
                                @Param("offset") long offset, @Param("limit") int limit);

    Attachment findById(@Param("id") Long id, @Param("userId") Long userId);

    int insert(Attachment a);

    /** 插入课件-学生关联（一个课件可关联多个学生） */
    int insertStudentRef(@Param("userId") Long userId,
                         @Param("attachmentId") Long attachmentId,
                         @Param("studentId") Long studentId);

    /** 课件是否已关联该学生（判重） */
    Long existsStudentRef(@Param("userId") Long userId,
                          @Param("attachmentId") Long attachmentId,
                          @Param("studentId") Long studentId);

    int delete(@Param("id") Long id, @Param("userId") Long userId);

    /** 删除课件的全部学生关联（课件删除级联用） */
    int deleteStudentRefsByAttachment(@Param("userId") Long userId, @Param("attachmentId") Long attachmentId);

    /** 删除某学生全部关联后将成为孤儿（不再关联任何学生）的课件 id 列表 */
    List<Long> findOrphanAttachmentIds(@Param("userId") Long userId, @Param("studentId") Long studentId);

    /** 删除某学生的全部课件关联（学生删除级联用） */
    int deleteStudentRefsByStudent(@Param("userId") Long userId, @Param("studentId") Long studentId);
}
