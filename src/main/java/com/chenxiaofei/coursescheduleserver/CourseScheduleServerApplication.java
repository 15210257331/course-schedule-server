package com.chenxiaofei.coursescheduleserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class CourseScheduleServerApplication {

    private static final Logger log = LoggerFactory.getLogger(CourseScheduleServerApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(CourseScheduleServerApplication.class, args);
    }

    @Bean
    CommandLineRunner startupLogger(Environment env) {
        return args -> {
            String port = env.getProperty("server.port", "8080");
            String[] activeProfiles = env.getActiveProfiles();
            String profile = activeProfiles.length == 0 ? "default" : String.join(",", activeProfiles);
            log.info("course-schedule-server 启动成功！端口: {}, 激活的 profile: {}", port, profile);
        };
    }

}
