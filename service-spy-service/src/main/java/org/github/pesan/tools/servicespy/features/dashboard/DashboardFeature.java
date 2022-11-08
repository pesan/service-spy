package org.github.pesan.tools.servicespy.features.dashboard;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import org.github.pesan.platform.Feature;
import org.github.pesan.platform.Platform;
import org.github.pesan.platform.web.RequestPath;
import org.github.pesan.platform.web.Routes;
import org.github.pesan.tools.servicespy.features.dashboard.config.ConfigPlugin;
import org.github.pesan.tools.servicespy.features.dashboard.traffic.TrafficPlugin;

import java.util.Optional;

@SuppressWarnings("unused")
public class DashboardFeature implements Feature {

    private final DashboardConfig config;

    @SuppressWarnings("unused")
    public DashboardFeature(DashboardConfig config) {
        this.config = config;
    }

    @Override
    public Completable initialize(Platform platform) {
        int port = config.port();

        return Single.zip(
                        new TrafficPlugin(platform).router(),
                        new ConfigPlugin(platform).router(),
                        config.webroot()
                                .map(webroot -> new StaticContentPlugin(platform, webroot).router())
                                .orElse(Single.just(Routes.empty())),
                        DashboardFeature::assembleRouter)
                .flatMap(routes -> platform.web().webServer(port, routes))
                .ignoreElement();
    }

    private static Routes assembleRouter(Routes trafficRouter, Routes configRouter, Routes staticContentRouter) {
        return trafficRouter.mountedAt(RequestPath.fromComponents("api", "traffic"))
                .and(configRouter.mountedAt(RequestPath.fromComponents("api", "config")))
                .and(staticContentRouter);
    }

    private record DashboardConfig(Optional<String> webroot, int port) {
    }
}