package org.github.pesan.platform.vertx;

import io.reactivex.rxjava3.core.Completable;
import io.vertx.core.DeploymentOptions;
import io.vertx.rxjava3.core.Vertx;
import org.github.pesan.platform.Feature;
import org.github.pesan.platform.Platform;
import org.github.pesan.platform.Platform.Alias;
import org.github.pesan.platform.messaging.MessageBus;
import org.github.pesan.platform.rx.RxMessageBus;
import org.github.pesan.platform.web.WebPlatform;

import java.util.Collection;

@Alias("vertx")
public class VertxPlatform implements Platform {
    private final Vertx vertx;

    private final WebPlatform webPlatform;
    private final MessageBus messagingPlatform;

    public VertxPlatform() {
        this(Vertx.vertx());
    }

    public VertxPlatform(Vertx vertx) {
        this.vertx = vertx;
        this.webPlatform = new VertxWebPlatform(vertx);
        this.messagingPlatform = new RxMessageBus();
    }

    @Override
    public Completable launch(Collection<Feature> features) {
        return vertx.deployVerticle(new ServerVerticle(this, features), new DeploymentOptions())
                .ignoreElement();
    }

    @Override
    public WebPlatform web() {
        return webPlatform;
    }

    @Override
    public MessageBus messageBus() {
        return messagingPlatform;
    }
}
