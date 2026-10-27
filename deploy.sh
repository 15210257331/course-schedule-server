#!/usr/bin/env bash
# ============================================================
# TeacherOS 后端一键发布脚本
# 流程：mvn 打包 → 上传 Dockerfile + jar 到服务器 → 删旧容器/镜像 → 构建镜像 → 启动容器
#
# 用法：
#   ./deploy.sh                          # 交互式（提示输入 SSH 密码，全过程只输一次）
#   ./deploy.sh --skip-build             # 跳过本地打包，直接上传 target 下已有 jar
#
# 可用环境变量覆盖默认值：
#   SSH_HOST / SSH_USER / SSH_PASS / REMOTE_DIR
# ============================================================
set -euo pipefail

# ---------- 可配置项 ----------
SSH_HOST="${SSH_HOST:-140.143.168.25}"
SSH_USER="${SSH_USER:-root}"
SSH_PASS="${SSH_PASS:-}"
REMOTE_DIR="${REMOTE_DIR:-/root/web/course-schedule-server}"
LOCAL_DIR="$(cd "$(dirname "$0")" && pwd)"

SKIP_BUILD=0
[[ "${1:-}" == "--skip-build" ]] && SKIP_BUILD=1

log()  { printf '\033[36m[deploy]\033[0m %s\n' "$*"; }
err()  { printf '\033[31m[deploy]\033[0m %s\n' "$*" >&2; exit 1; }

# ---------- 1. 本地打包 ----------
if [[ $SKIP_BUILD -eq 0 ]]; then
  # 项目要求 JDK 17 编译；本机默认 JDK 可能是 8（sdkman current 指向 8），
  # 优先用 sdkman 的 17，其次 JAVA_HOME，保证 javac 支持 switch 箭头语法。
  if [[ -x "$HOME/.sdkman/candidates/java/17.0.0-tem/bin/javac" ]]; then
    export JAVA_HOME="$HOME/.sdkman/candidates/java/17.0.0-tem"
  fi
  log "使用 JDK: $("$JAVA_HOME/bin/javac" -version 2>&1 || javac -version 2>&1)"
  log "开始 Maven 打包（跳过测试）..."
  (cd "$LOCAL_DIR" && ./mvnw clean package -DskipTests)
  log "打包完成"
fi

# ---------- 2. 定位 jar ----------
JAR="$(ls -t "$LOCAL_DIR"/target/course-schedule-server-*.jar 2>/dev/null | grep -v '\.original$' | head -1 || true)"
[[ -n "$JAR" ]] || err "target 下未找到 course-schedule-server-*.jar，请先执行 ./mvnw package 或去掉 --skip-build"
log "待发布 jar: $(basename "$JAR")"

# 服务器上的 jar 统一叫 app.jar（覆盖上传，无需按版本号管理）
REMOTE_JAR="app.jar"

# ---------- 3. SSH 复用连接（密码只输一次） ----------
# 原理：开一个 SSH 主连接（ControlMaster）放到后台，后续 ssh/scp 全部复用该连接，
# 认证只在首次建立时做一次。结束/出错时统一清理 socket，避免残留后台进程。
CONTROL_DIR="$(mktemp -d)"
SSH_OPTS=(-o StrictHostKeyChecking=no -o ControlMaster=auto -o ControlPath="$CONTROL_DIR/%r@%h:%p" -o ControlPersist=no)

cleanup() { ssh "${SSH_OPTS[@]}" -O exit "$SSH_USER@$SSH_HOST" >/dev/null 2>&1 || true; rm -rf "$CONTROL_DIR"; }
trap cleanup EXIT

# 建立主连接：有 SSH_PASS + sshpass 时免交互（CI 场景）；否则提示输一次密码
if command -v sshpass >/dev/null 2>&1 && [[ -n "$SSH_PASS" ]]; then
  sshpass -p "$SSH_PASS" ssh "${SSH_OPTS[@]}" -fN "$SSH_USER@$SSH_HOST"
else
  if [[ -z "$SSH_PASS" ]] && ! ssh-add -l >/dev/null 2>&1; then
    log "需要 SSH 密码（整个过程只输这一次）..."
  fi
  ssh "${SSH_OPTS[@]}" -fN "$SSH_USER@$SSH_HOST"
fi

run_ssh() { ssh "${SSH_OPTS[@]}" "$SSH_USER@$SSH_HOST" "$@"; }
run_scp() { scp "${SSH_OPTS[@]}" "$@"; }

log "连接服务器 $SSH_USER@$SSH_HOST ..."
run_ssh "mkdir -p '$REMOTE_DIR'"

# ---------- 4. 上传（覆盖旧文件） ----------
log "上传 Dockerfile 与 jar ..."
run_scp "$LOCAL_DIR/Dockerfile" "$SSH_USER@$SSH_HOST:$REMOTE_DIR/Dockerfile"
run_scp "$JAR" "$SSH_USER@$SSH_HOST:$REMOTE_DIR/$REMOTE_JAR"
# ---------- 5. 远程：删旧容器/镜像 → 构建镜像 → 启动容器 ----------
# 注意：Dockerfile 中 COPY course-schedule-server-*.jar app.jar，
# 服务器上只有一个 app.jar 时 COPY 会匹配失败，先改名为带前缀的文件再构建。
log "远程重建容器 ..."
run_ssh "set -e
  cd '$REMOTE_DIR'
  # COPY 需要匹配 course-schedule-server-*.jar，把 app.jar 换个名字
  [[ -f app.jar ]] && mv -f app.jar course-schedule-server-app.jar
  docker rm -f course-schedule-server >/dev/null 2>&1 || true
  docker rmi -f course-schedule-server:latest >/dev/null 2>&1 || true
  docker build -t course-schedule-server:latest .
  docker run -d --name course-schedule-server --restart unless-stopped \
    -p 9999:8080 \
    -v '$REMOTE_DIR/uploads':/app/uploads \
    -v '$REMOTE_DIR/backup':/app/backup \
    course-schedule-server:latest
  docker ps --filter name=course-schedule-server
"

log "发布完成 ✅"
