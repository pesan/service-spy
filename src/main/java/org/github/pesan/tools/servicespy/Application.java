package org.github.pesan.tools.servicespy;

import org.github.pesan.tools.servicespy.admin.AdminApplication;
import org.github.pesan.tools.servicespy.common.CommonApplication;
import org.github.pesan.tools.servicespy.proxy.ProxyApplication;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class Application {
    public static void main(String... args) {
        SpringApplicationBuilder builder = new SpringApplicationBuilder(CommonApplication.class);
        builder.web(false).run(args);
        builder.child(AdminApplication.class).properties("server.port=${admin.server.port}").bannerMode(Banner.Mode.OFF).run(args);
        builder.child(ProxyApplication.class).properties("server.port=${proxy.server.port}").bannerMode(Banner.Mode.OFF).run(args);
    }
}
