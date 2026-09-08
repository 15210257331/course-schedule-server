#!/usr/bin/env bash
#
# course-schedule-server 一键部署脚本
# 流程：本地 Maven 打包 -> 上传 Dockerfile + jar 到服务器 -> 服务器构建镜像 -> 重启容器
#
# 可用环境变量覆盖（也可直接改下方默认值）：
#   DEPLOY_HOST        服务器地址           默认 140.143.168.25
#   DEPLOY_USER        SSH 用户名           默认 root
#   DEPLOY_PORT        SSH 端口             默认 22
#   DEPLOY_PASS        服务器密码           默认留空（空则先试密钥，不通再提示输入密码）
#   DEPLOY_DIR         服务器部署目录        默认 /root/web/course-schedule-server/
#   DEPLOY_CONTAINER   容器名               默认 course-schedule-server
#   DEPLOY_IMAGE       镜像名               默认 course-schedule-server
#   DEPLOY_TAG         镜像 tag             默认 latest
#   DEPLOY_PORT_MAP    端口映射(宿主:容器)   默认 9999:8080

set -euo pipefail

SSH_HOST="${DEPLOY_HOST:-140.143.168.25}"
SSH_USER="${DEPLOY_USER:-root}"
SSH_PORT="${DEPLOY_PORT:-22}"
SSH_PASS="${DEPLOY_PASS:-}"
REMOTE_DIR="${DEPLOY_DIR:-/root/web/course-schedule-server/}"
CONTAINER_NAME="${DEPLOY_CONTAINER:-course-schedule-server}"
IMAGE_NAME="${DEPLOY_IMAGE:-course-schedule-server}"
IMAGE_TAG="${DEPLOY_TAG:-latest}"
IMAGE_FULL="$IMAGE_NAME:$IMAGE_TAG"
PORT_MAP="${DEPLOY_PORT_MAP:-9999:8080}"
LOCAL_DOCKERFILE="Dockerfile"
REMOTE_SCRIPT_NAME="deploy-remote.sh"

cd "$(dirname "$0")"

log() { printf '\033[1;32m[deploy]\033[0m %s\n' "$*"; }
err() { printf '\033[1;31m[error]\033[0m %s\n' "$*" >&2; exit 1; }

command -v ssh >/dev/null 2>&1 || err "未找到 ssh"
command -v scp >/dev/null 2>&1 || err "未找到 scp"

USE_EXPECT=0; command -v expect >/dev/null 2>&1 && USE_EXPECT=1
USE_SSHPASS=0; command -v sshpass >/dev/null 2>&1 && USE_SSHPASS=1

# ---------- 登录方式 ----------
if [ -n "$SSH_PASS" ]; then
    if [ "$USE_SSHPASS" = "0" ] && [ "$USE_EXPECT" = "0" ]; then
        err "已设置 DEPLOY_PASS 但系统既无 sshpass 也无 expect；请 brew install sshpass 或改用密钥登录"
    fi
    if [ "$USE_SSHPASS" = "1" ]; then USE_EXPECT=0; fi
fi

# 未提供密码时，先探测密钥免密是否可用，不通则交互式输入密码
if [ -z "$SSH_PASS" ]; then
    if ! ssh -p "$SSH_PORT" -o BatchMode=yes -o ConnectTimeout=5 -o StrictHostKeyChecking=no "$SSH_USER@$SSH_HOST" true 2>/dev/null; then
        if [ "$USE_EXPECT" = "0" ] && [ "$USE_SSHPASS" = "0" ]; then
            err "密钥免密不可用，且本机无 expect/sshpass 无法输入密码；请先配置免密或安装 expect/sshpass"
        fi
        printf '请输入服务器密码 (%s@%s): ' "$SSH_USER" "$SSH_HOST"
        read -r -s SSH_PASS
        printf '\n'
        [ -n "$SSH_PASS" ] || err "密码不能为空"
        # 优先用 sshpass（更稳），否则回退 expect
        if [ "$USE_SSHPASS" = "1" ]; then USE_EXPECT=0; fi
    fi
fi

# ---------- 本地 Maven 打包 ----------
log "本地 Maven 打包（跳过测试）..."
./mvnw -q clean package -DskipTests || err "Maven 打包失败"

JAR_FILE=$(ls -1t target/course-schedule-server-*.jar 2>/dev/null | head -n1)
[ -n "$JAR_FILE" ] || err "未在 target/ 找到可执行 jar"
log "打包产物: $JAR_FILE"

# ---------- 生成服务器端部署脚本 ----------
log "生成服务器端部署脚本..."
TMPDIR_LOCAL="target/.deploy"
mkdir -p "$TMPDIR_LOCAL"

cat > "$TMPDIR_LOCAL/$REMOTE_SCRIPT_NAME" <<EOF
#!/usr/bin/env bash
set -e
cd '$REMOTE_DIR'
echo "--- 构建镜像 $IMAGE_FULL ---"
docker build -t '$IMAGE_FULL' .
echo "--- 停止并删除旧容器 ---"
docker stop '$CONTAINER_NAME' 2>/dev/null || true
docker rm '$CONTAINER_NAME' 2>/dev/null || true
echo "--- 启动新容器 ---"
docker run -d --name '$CONTAINER_NAME' --restart unless-stopped \\
  -p '$PORT_MAP' \\
  -v '$REMOTE_DIR/uploads:/app/uploads' \\
  '$IMAGE_FULL'
echo "--- 容器状态 ---"
docker ps --filter name='$CONTAINER_NAME'
EOF

# ---------- SSH / SCP 执行封装 ----------
run_ssh_command() {
    local cmd="$1"
    if [ "$USE_SSHPASS" = "1" ]; then
        sshpass -p "$SSH_PASS" ssh -p "$SSH_PORT" -o StrictHostKeyChecking=no "$SSH_USER@$SSH_HOST" "$cmd"
    elif [ "$USE_EXPECT" = "1" ]; then
        export E_SSH_PORT="$SSH_PORT" E_SSH_USER="$SSH_USER" E_SSH_HOST="$SSH_HOST" E_DEPLOY_PASS="$SSH_PASS" E_CMD="$cmd"
        expect <<'EXPECT'
set timeout 600
spawn ssh -p $env(E_SSH_PORT) -o StrictHostKeyChecking=no $env(E_SSH_USER)@$env(E_SSH_HOST) $env(E_CMD)
expect {
    "*yes/no*"     { send "yes\r"; exp_continue }
    "*assword:*"   { send -- "$env(E_DEPLOY_PASS)\r"; exp_continue }
    eof
}
EXPECT
    else
        ssh -p "$SSH_PORT" -o StrictHostKeyChecking=no "$SSH_USER@$SSH_HOST" "$cmd"
    fi
}

scp_file() {
    local local_path="$1" remote_path="$2"
    if [ "$USE_SSHPASS" = "1" ]; then
        sshpass -p "$SSH_PASS" scp -P "$SSH_PORT" -o StrictHostKeyChecking=no "$local_path" "$SSH_USER@$SSH_HOST:$remote_path"
    elif [ "$USE_EXPECT" = "1" ]; then
        export E_SSH_PORT="$SSH_PORT" E_SSH_USER="$SSH_USER" E_SSH_HOST="$SSH_HOST" E_DEPLOY_PASS="$SSH_PASS" E_LOCAL="$local_path" E_REMOTE="$remote_path"
        expect <<'EXPECT'
set timeout 600
spawn scp -P $env(E_SSH_PORT) -o StrictHostKeyChecking=no $env(E_LOCAL) $env(E_SSH_USER)@$env(E_SSH_HOST):$env(E_REMOTE)
expect {
    "*yes/no*"     { send "yes\r"; exp_continue }
    "*assword:*"   { send -- "$env(E_DEPLOY_PASS)\r"; exp_continue }
    eof
}
EXPECT
    else
        scp -P "$SSH_PORT" -o StrictHostKeyChecking=no "$local_path" "$SSH_USER@$SSH_HOST:$remote_path"
    fi
}

# ---------- 上传 ----------
log "上传文件到 $SSH_USER@$SSH_HOST:$REMOTE_DIR ..."
run_ssh_command "mkdir -p '$REMOTE_DIR/uploads'"

scp_file "$LOCAL_DOCKERFILE" "$REMOTE_DIR$LOCAL_DOCKERFILE"
scp_file "$JAR_FILE" "$REMOTE_DIR$(basename "$JAR_FILE")"
scp_file "$TMPDIR_LOCAL/$REMOTE_SCRIPT_NAME" "$REMOTE_DIR$REMOTE_SCRIPT_NAME"

# ---------- 远程构建 + 重启容器 ----------
log "远程执行部署脚本..."
run_ssh_command "bash '$REMOTE_DIR$REMOTE_SCRIPT_NAME'"

log "部署完成 ✔  容器: $CONTAINER_NAME ($PORT_MAP)"