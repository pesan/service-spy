package org.github.pesan.tools.servicespy.features.dashboard;

import io.reactivex.rxjava3.core.Single;
import org.github.pesan.platform.Platform;
import org.github.pesan.platform.web.Routes;

public class StaticContentPlugin {
    private final Platform platform;
    private final String webroot;

    public StaticContentPlugin(Platform platform, String webroot) {
        this.platform = platform;
        this.webroot = webroot;
    }

    public Single<Routes> router() {
        return Single.just(Routes.GET("/*", platform.web().staticHandler(webroot)));
    }
}