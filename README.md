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

一键发布脚本：`./deploy.sh`（本地 Maven 打包 → 上传 Dockerfile 与 jar 到服务器 `/root/web/course-schedule-server`（覆盖旧文件）→ 删除旧容器与镜像 → 构建镜像 → 启动容器）。

```bash
./deploy.sh                 # 完整流程
./deploy.sh --skip-build    # 跳过本地打包，直接上传 target 下已有 jar
```

默认 `root@140.143.168.25`，可用环境变量覆盖：`SSH_HOST / SSH_USER / SSH_PASS / REMOTE_DIR`。
脚本通过 SSH 连接复用（ControlMaster）让密码认证只做一次：首次连接输一次密码，后续 mkdir / scp / 远程命令全部复用该连接。传了 `SSH_PASS` 且装有 `sshpass` 时可全程免交互（CI 场景）。

### 免密码发布（推荐，一次性配置）

把本机公钥追加到服务器后，发布全程不再需要输密码：

```bash
ssh-copy-id root@140.143.168.25    # 输最后一次密码，之后密钥自动认证
./deploy.sh                        # 零交互发布
```

若本机还没有密钥对，先生成：`ssh-keygen -t rsa -b 4096`。

手动流程（脚本等价于以下步骤）：

```bash
docker build -t course-schedule-server:latest .

docker run -d --name course-schedule-server --restart unless-stopped -p 9999:8080 -v /root/web/course-schedule-server/uploads:/app/uploads -v /root/web/course-schedule-server/backup:/app/backup course-schedule-server:latest
```

## 使用docker-compose启动

```bash
# 构建镜像并启动容器   `build`：自动读取当前目录 Dockerfile，构建镜像（不需要提前 docker build）
docker-compose up -d --build
# 停止服务
docker-compose down
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
