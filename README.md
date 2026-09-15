# TeacherOS 后端（course-schedule-server）

TeacherOS · 兼职教师工作台的后端服务，技术栈：Spring Boot + MyBatis + MySQL + JWT + BCrypt + Lombok。

> 功能需求与接口说明见前端仓库 `docs/` 目录下的《项目功能需求说明书》。

## 运行环境

| 项 | 说明 |
|---|---|
| JDK | 17（本机编译可用 `JAVA_HOME=~/.sdkman/candidates/java/21-tem ./mvnw compile`） |
| 构建工具 | Maven Wrapper（`./mvnw`） |
| 数据库 | MySQL（库名 `course_schedule`），`utf8mb4` |
| 服务端口 | 8080 |
| 配置 | `application.yaml`（总配置）+ `application-dev.yaml`（开发环境） |

## 本地启动

```bash
./mvnw compile            # 编译
./mvnw spring-boot:run    # 启动（默认激活 dev profile）
```

## 生产部署（Docker）

构建脚本：将 Dockerfile 与 jar 包上传到服务器后构建镜像并启动容器。

```bash
docker build -t course-schedule-server:latest .

docker run -d --name course-schedule-server --restart unless-stopped -p 9999:8080 -v /root/web/course-schedule-server/uploads:/app/uploads -v /root/web/course-schedule-server/backup:/app/backup course-schedule-server:latest
```

## 敏感配置

生产部署时，以下配置请通过环境变量注入，不要硬编码进仓库：

| 敏感项 | 位置 | 建议环境变量 |
|---|---|---|
| JWT 签名密钥 | `application.yaml` → `jwt.secret` | `JWT_SECRET` |
| 数据库账号/密码 | `application-*.yaml` → `spring.datasource.*` | `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` |
| 邮箱 SMTP 凭据 | `spring.mail.*` | `SPRING_MAIL_USERNAME` / `SPRING_MAIL_PASSWORD` |

```bash
export JWT_SECRET="$(openssl rand -base64 48)"
export SPRING_DATASOURCE_PASSWORD="你的生产密码"
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```
