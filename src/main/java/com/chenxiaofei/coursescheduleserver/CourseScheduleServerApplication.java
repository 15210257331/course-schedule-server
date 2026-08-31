package com.chenxiaofei.coursescheduleserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CourseScheduleServerApplication {

    private static final Logger log = LoggerFactory.getLogger(CourseScheduleServerApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(CourseScheduleServerApplication.class, args);
    }
    //  @Bean：把这个方法返回的对象交给 Spring
    //  容器管理。Spring 启动时看到返回类型是
    //  CommandLineRunner，就会在应用启动完成后自动执行它。
    //  - 方法参数 Environment env：Spring
    //  自动注入的环境对象，用来读取配置（端口、profile 等）
    @Bean
    CommandLineRunner startupLogger(Environment env) {
        return args -> {
            String port = env.getProperty("server.port", "8080");
            String[] activeProfiles = env.getActiveProfiles();
            String profile = activeProfiles.length == 0 ? "default" : String.join(",", activeProfiles);
            log.info("course-schedule-server 启动成功！！！！端口: {}, 激活的 profile: {}", port, profile);
        };
    }

}
