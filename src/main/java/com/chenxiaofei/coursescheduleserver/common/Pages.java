package com.chenxiaofei.coursescheduleserver.common;

import java.util.List;
import java.util.function.Supplier;

/**
 * 分页查询工具：封装「count 总数 + 分页取数」的样板逻辑。
 *
 * <p>典型用法：
 * <pre>{@code
 * return Pages.of(req,
 *         () -> mapper.countByUser(userId, req.getName()),
 *         (offset, limit) -> mapper.pageByUser(userId, req.getName(), offset, limit));
 * }</pre>
 */
public final class Pages {

    private static final int DEFAULT_NUM = 1;
    private static final int DEFAULT_SIZE = 20;

    private Pages() {
    }

    /**
     * 执行分页查询：先 count 总数，再按 offset/limit 取本页数据。
     * 自动兜底 pageNum/pageSize 的空值与非法值（&lt;1）。
     *
     * @param req     分页参数（提供 pageNum/pageSize）
     * @param counter 总数查询，如 {@code () -> mapper.countXxx(filters)}
     * @param pager   分页数据查询，入参为 offset 与 limit，如 {@code (offset, limit) -> mapper.pageXxx(filters, offset, limit)}
     * @return 分页结果
     */
    public static <T> PageResult<T> of(PageRequest req, Supplier<Long> counter, Pager<T> pager) {
        int size = clampSize(req.getPageSize());
        long offset = (long) (clampNum(req.getPageNum()) - 1) * size;
        long total = counter.get();
        List<T> list = pager.apply(offset, size);
        return PageResult.of(total, list);
    }

    /** 页码兜底：空或 &lt;1 取默认 1 */
    public static int clampNum(Integer pageNum) {
        return (pageNum == null || pageNum < 1) ? DEFAULT_NUM : pageNum;
    }

    /** 每页条数兜底：空或 &lt;1 取默认 20 */
    public static int clampSize(Integer pageSize) {
        return (pageSize == null || pageSize < 1) ? DEFAULT_SIZE : pageSize;
    }

    /** 分页数据查询函数：offset 偏移量（从 0 开始），limit 本页条数 */
    @FunctionalInterface
    public interface Pager<T> {
        List<T> apply(long offset, int limit);
    }
}
