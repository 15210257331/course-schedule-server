# TeacherOS 后端（course-schedule-server）需求说明书

> 项目名称：TeacherOS · 兼职教师工作台（后端）
> 技术栈：Spring Boot + MyBatis + MySQL + JWT + BCrypt + Lombok

---

## 一、项目概述

TeacherOS 后端为「兼职教师工作台」提供 REST API 服务，支撑教师排课、课程/学生/机构/模板管理、收入统计、课程提醒、用户与偏好设置等业务。

采用 JWT 无状态认证 + `UserContext` 线程级用户上下文，所有资源按用户隔离；通过定时任务完成课程状态自动结算与到点提醒生成。

---

## 二、运行环境与启动

| 项 | 说明 |
|---|---|
| JDK | 17（本机编译可用 `JAVA_HOME=~/.sdkman/candidates/java/21-tem ./mvnw compile`） |
| 构建工具 | Maven Wrapper（`./mvnw`） |
| 数据库 | MySQL（库名 `course_schedule`），`utf8mb4` |
| 服务端口 | 8080 |
| 配置 | `application.yaml`（总配置）+ `application-dev.yaml`（开发环境） |

```bash
./mvnw compile       # 编译
./mvnw spring-boot:run  # 启动（默认激活 dev profile）
```

---

## 三、技术要点

- **认证鉴权**：`JwtAuthInterceptor` 拦截除白名单外的请求，校验 `Authorization: Bearer <token>`，解析后写入 `UserContext`（`ThreadLocal`）。
- **密码安全**：`BCrypt` 加盐哈希存储。
- **统一返回**：`Result<T>`（`{ code, message, data }`，`code=0` 成功）。
- **分页**：`PageResult<T>`（`{ total, list }`）。
- **异常处理**：`GlobalExceptionHandler` 统一捕获 `BusinessException` 与校验错误，转换为 `Result`。
- **统一响应时间**：见过期课程结算与提醒生成为 `@Scheduled` 定时任务。

---

## 四、模块与功能

### 4.1 认证（auth）
- 登录：校验用户名密码，签发 JWT，返回用户信息（含任教学科 `subjects`）。
- 注册：用户名唯一校验，密码 BCrypt 加密，默认角色 `TEACHER`。
- 个人资料：查询（脱敏密码）、更新（昵称 / 头像 / 邮箱 / 手机号 / 任教学科）。
- 修改密码：原密码校验 + 新密码加密。

### 4.2 课程（course）
- 按时间范围查询（可带标题模糊过滤）。
- 分页查询（标题模糊 + 时间范围，按 `start_time` 倒序）。
- 详情、新增、编辑、删除。
- **新增 / 编辑**：
  - 校验起止时间（结束晚于开始）。
  - 冲突校验（同用户同时间段已有课则拒绝）。
  - 学生解析：按姓名查学生表，存在则绑定并补齐机构/科目/学段，不存在则自动建档。
- **移动课程**：调整起止时间（含冲突校验）。
- **复制**：复制到目标时间段。
- **复制本周到下周**：整周复制，跳过已结束课程与重复系列源，目标时段冲突则跳过该节。
- **重复排课**：`repeat_type`（daily/weekly）+ `repeat_end_date`，按期生成系列课程（`parent_id` 关联），逐节冲突校验；删除主课程连带删除整个系列。

### 4.3 课程模板（coursetemplate）
- 列表 / 分页 / 详情 / 新增 / 编辑 / 删除。
- **科目限制**：科目必须是当前用户任教学科之一（未设置任教学科则不限制）。
- **学生唯一**：一个学生只能有一个模板。
- 学生字段解析同课程（绑定或自动建档，同步机构/科目/学段）。

### 4.4 学生（student）
> **注**：学生管理功能已简化，学生信息通过课程/模板中的姓名字段自动关联，不再单独维护学生实体。

- 学生以「姓名」为去重键，课程/模板录入学生姓名时自动绑定或自动建档。
- 课程/模板保存时会同步学生的机构、科目、学段信息。

### 4.5 机构（organization）
- 列表 / 分页 / 详情 / 新增 / 编辑 / 删除。
- **删除保护**：机构下存在课程时禁止删除。

### 4.6 工作台统计（dashboard）
- **汇总** `summary`：今日收入、上月收入、本月收入、本年收入。
- **今日课程** `today-courses`。
- **收入报告** `income-report`：支持 `days`（近 N 天）与 `start/end`（指定日期范围）两种入参，返回趋势、机构/学生/学段占比、学生/机构费用明细、总额、总时长、总节数。
- **结算状态更新** `settlement`：标记课程费用是否已结清，用于收入统计口径区分。
- **口径统一**：收入与课时统计均以 `end_time < NOW()`（课程已结束）为准；课程去重按用户隔离。

### 4.7 提醒（notification）
- 提醒列表、未读提醒、标记已读 / 全部已读、删除。
- **到点提醒生成**（`ReminderScheduler`，每分钟）：根据课程的提醒偏移（默认 30 分钟，可被全局设置覆盖）在开课前窗口内为 scheduled 课程生成提醒，每节课只生成一次（按 `course_id` 去重）。

### 4.8 系统设置（setting）
- 设置读取与保存（`user_id + setting_key` 唯一，`ON DUPLICATE KEY UPDATE` 幂等 upsert）。
- 支持键：
  - `defaultDuration`：默认课程时长（分钟），新建课程/模板时自动填充
  - `reminderOffset`：默认课程提醒（分钟），创建课程时若未指定提醒时间则使用此值
  - `defaultFee`：默认课时费（元/小时），新建课程/模板时自动填充
  - `browserNotify`：浏览器通知开关，控制是否弹出系统通知
  - `defaultView`：默认视图（day/week/month），课程日历默认展示
  - `idbCache`：本地缓存开关，控制是否使用 IndexedDB 缓存

### 4.9 数据备份（backup）
- **导出**：将当前用户的所有业务数据（课程/模板/机构/学生/设置）导出为 JSON 附件下载。
- **导入**：全量替换当前用户数据（先清空后插入），用于数据迁移或恢复。

### 4.10 课程状态自动结算
- `CourseStatusScheduler`（每分钟）：把 `status = scheduled` 且 `end_time` 已过期的课程批量置为 `completed`，保证收入/课时统计与课程状态一致。

---

## 五、数据表（`schema.sql`）

| 表 | 说明 | 关键字段 |
|---|---|---|
| `user` | 用户 | username（唯一）、password（BCrypt）、nickname、avatar、email、phone、subjects（任教学科，逗号分隔）、role |
| `organization` | 机构 | user_id、name、contact_name、contact_phone、address、default_fee、color、remark |
| `student` | 学生 | user_id、name、gender、grade、subject、organization_id、phone、parent_phone、fee、remark |
| `course` | 课程 | user_id、title、student_id、student_name、organization_id、subject、stage、course_type、start_time、end_time、fee、fee_manual、location、note、status、color、reminder_offset_minutes、repeat_type、repeat_end_date、parent_id |
| `course_template` | 课程模板 | user_id、title、student_id、student_name、organization_id、subject、stage、course_type、duration_minutes、fee、fee_manual、location、note、color、repeat_type |
| `notification` | 提醒 | user_id、type、title、content、course_id、remind_at、is_read |
| `setting` | 用户设置 | user_id、setting_key、setting_value（联合唯一） |

> 表结构通过 `application.yaml` 中的 `schema-locations` 或本仓库 `schema.sql` 初始化，均幂等（`CREATE TABLE IF NOT EXISTS`）；存量表列的补齐见 `SchemaMigration`。

---

## 六、安全配置（密钥与密码管理）

生产环境部署时，以下敏感配置**不要**硬编码进仓库，应通过环境变量注入：

| 敏感项 | 位置 | 建议环境变量 | 说明 |
|---|---|---|---|
| JWT 签名密钥 | `application.yaml` → `jwt.secret` | `JWT_SECRET` | 需 >= 32 字节的随机串；泄露后所有登录态可被伪造 |
| 数据库密码 | `application-*.yaml` → `spring.datasource.password` | `SPRING_DATASOURCE_PASSWORD` | 现仓库内 `application-dev.yaml` 为开发占位值 |
| 数据库账号 | `spring.datasource.username` | `SPRING_DATASOURCE_USERNAME` | 生产建议使用只读/最小权限账号 |
| 邮箱 SMTP 凭据 | `spring.mail.*` | `SPRING_MAIL_USERNAME` / `SPRING_MAIL_PASSWORD` | 启用真实邮件（`mail.enabled=true`）时使用 |

Spring Boot 允许用环境变量覆盖配置，例如：

```bash
export JWT_SECRET="$(openssl rand -base64 48)"
export SPRING_DATASOURCE_PASSWORD="你的生产密码"
export SPRING_MAIL_HOST=smtp.exmail.qq.com
export SPRING_MAIL_USERNAME=no-reply@example.com
export SPRING_MAIL_PASSWORD="你的授权码"

./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

**当前状态说明**：`application-dev.yaml` 中的数据库密码与 `application.yaml` 中的 JWT 密钥均为开发占位值，仅用于本地联调；正式部署前务必改用环境变量并更新密钥。

---

## 七、目录结构

```
src/main/java/com/chenxiaofei/coursescheduleserver/
├── CourseScheduleServerApplication.java  # 启动类（启用定时任务）
├── auth/            # 认证：登录/注册/资料/密码
├── backup/          # 数据备份：导出/导入
├── common/          # Result / PageResult / BusinessException / 全局异常
├── config/          # Web 配置、JWT 属性、启动迁移、数据初始化
├── course/          # 课程：服务 / 控制器 / 状态结算定时任务
├── coursetemplate/  # 课程模板
├── dashboard/       # 工作台与收入统计
├── notification/    # 提醒 / 到点提醒定时任务
├── organization/    # 机构
├── security/        # JWT 工具 / 拦截器 / UserContext
├── setting/         # 系统设置
├── upload/          # 文件上传
└── student/         # 学生（已简化，通过课程/模板关联）
```

---

## 八、接口列表（`/api` 前缀，需 JWT，登录/注册/重置密码/上传除外）

> **注意**：自 v2.0 起，所有查询接口（原 GET）统一改为 POST 请求，参数通过 RequestBody 传递。

### 认证（auth）
| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/auth/login` | 登录 |
| POST | `/auth/register` | 注册 |
| POST | `/auth/profile` | 查询个人资料 |
| PUT | `/auth/profile` | 更新个人资料 |
| PUT | `/auth/password` | 修改密码 |
| POST | `/auth/reset-code` | 发送密码重置验证码 |
| POST | `/auth/reset-password` | 校验验证码并重置密码 |

### 课程（courses）
| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/courses/list` | 按时间范围查课程（body: start, end, title） |
| POST | `/courses/page` | 课程分页 |
| POST | `/courses/detail` | 课程详情（body: id） |
| POST | `/courses` | 新增课程 |
| PUT | `/courses/{id}` | 编辑课程 |
| DELETE | `/courses/{id}` | 删除课程 |
| PUT | `/courses/{id}/move` | 移动课程时间 |
| POST | `/courses/copy-week` | 复制本周到下周（body: week） |

### 课程模板（course-templates）
| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/course-templates/list` | 模板列表（body: name 可选） |
| POST | `/course-templates/page` | 模板分页 |
| POST | `/course-templates/detail` | 模板详情（body: id） |
| POST | `/course-templates` | 新增模板 |
| PUT | `/course-templates/{id}` | 编辑模板 |
| DELETE | `/course-templates/{id}` | 删除模板 |

### 机构（organizations）
| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/organizations/list` | 机构列表（body: name 可选） |
| POST | `/organizations/page` | 机构分页 |
| POST | `/organizations/detail` | 机构详情（body: id） |
| POST | `/organizations` | 新增机构 |
| PUT | `/organizations/{id}` | 编辑机构 |
| DELETE | `/organizations/{id}` | 删除机构 |

### 工作台统计（dashboard）
| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/dashboard/summary` | 汇总统计 |
| POST | `/dashboard/today-courses` | 今日课程 |
| POST | `/dashboard/income-report` | 收入报告（body: days 或 start/end） |
| PUT | `/dashboard/settlement` | 更新结算状态 |

### 系统设置（settings）
| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/settings/list` | 读取设置 |
| PUT | `/settings` | 保存设置 |

### 提醒（notifications）
| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/notifications/list` | 提醒列表（body: limit 可选，默认 20） |
| POST | `/notifications/due` | 未读到期提醒 |
| POST | `/notifications/{id}/read` | 标记已读 |
| POST | `/notifications/read-all` | 全部已读 |
| DELETE | `/notifications/{id}` | 删除提醒 |

### 数据备份（backup）
| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/backup/export` | 导出数据（JSON 附件） |
| POST | `/backup/import` | 导入数据（全量替换） |

### 上传（upload）
| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/upload/avatar` | 上传头像（multipart/form-data） |

---

## 九、关键约定

- **接口规范**：自 v2.0 起，所有查询接口（原 GET）统一改为 POST 请求，参数通过 RequestBody 传递；路径采用 `/list`、`/page`、`/detail` 等语义化后缀。
- 所有业务数据按 `user_id` 隔离，接口通过 `UserContext.getUserId()` 获取当前用户。
- 收入 / 课时统计口径统一：`end_time < NOW()` 视为「已结束」，跨所有统计一致。
- 课程完成状态由定时任务按时间自动结算，不依赖手动操作。
- 学生以「姓名」为去重键，课程 / 模板录入学生姓名时自动绑定或自动建档。
- 课程标题由科目 + 类型自动生成（前端），后端对标题做必填校验。
- **默认配置贯通**：系统设置中的 `defaultDuration`（默认时长）、`defaultFee`（默认课时费）、`reminderOffset`（默认提醒）会在新建课程/模板时自动填充，创建课程时若未指定提醒时间则使用 `reminderOffset` 配置值。