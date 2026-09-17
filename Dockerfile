FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY course-schedule-server-*.jar app.jar

# 时区必须固定为北京时间：alpine 基础镜像默认 UTC，会让 LocalDateTime.now() 比 MySQL（北京时间）
# 少 8 小时，导致备份定时、上课提醒、课程自动结算全部错位。
ENV TZ=Asia/Shanghai
RUN apk add --no-cache tzdata \
 && mkdir -p /app/uploads /app/backup

# 运行期写入目录：上传文件 + 本地备份/COS 临时文件。相对路径 ./uploads ./backup 解析到 /app 下，
# 必须预先创建，否则首次备份/上传因父目录不存在抛 NoSuchFileException。
VOLUME /app/uploads
VOLUME /app/backup

EXPOSE 8080

ENTRYPOINT ["java", "-Duser.timezone=Asia/Shanghai", "-jar", "app.jar", "--spring.profiles.active=prod"]
