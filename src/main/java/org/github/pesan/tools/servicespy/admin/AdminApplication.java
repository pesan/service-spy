package org.github.pesan.tools.servicespy.admin;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackageClasses = AdminApplication.class)
@EnableAsync
public class AdminApplication {

}

