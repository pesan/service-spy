package org.github.pesan.platform;

import io.reactivex.rxjava3.core.Completable;

public interface Feature {
    Completable initialize(Platform platform);
}
