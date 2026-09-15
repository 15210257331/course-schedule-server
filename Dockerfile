FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY course-schedule-server-*.jar app.jar

# 运行期写入目录：上传文件 + 本地备份/COS 临时文件。相对路径 ./uploads ./backup 解析到 /app 下，
# 必须预先创建，否则首次备份/上传因父目录不存在抛 NoSuchFileException。
RUN mkdir -p /app/uploads /app/backup
VOLUME /app/uploads
VOLUME /app/backup

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
