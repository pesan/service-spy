package org.github.pesan.platform.vertx;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.vertx.rxjava3.core.AbstractVerticle;
import org.github.pesan.platform.Feature;
import org.github.pesan.platform.Platform;

import java.util.Collection;

class ServerVerticle extends AbstractVerticle {

    private final Platform platform;
    private final Collection<Feature> features;

    ServerVerticle(Platform platform, Collection<Feature> features) {
        this.platform = platform;
        this.features = features;
    }

    @Override
    public Completable rxStart() {
        return Flowable.fromIterable(features)
                .flatMapCompletable(feature -> feature.initialize(platform));
    }
}