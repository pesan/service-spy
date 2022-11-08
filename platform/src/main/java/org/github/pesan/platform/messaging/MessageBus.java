package org.github.pesan.platform.messaging;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;

public interface MessageBus {

    interface Publisher<T> {
        Completable send(T t);
    }

    <T> Flowable<T> consumer(Class<T> type, String topic);

    default <T> Disposable consume(Class<T> type, String topic, Consumer<T> handler) {
        return consumer(type, topic).subscribe(handler);
    }

    default <T> MessageBus.Publisher<T> publisher(String topic) {
        return payload -> publish(topic, payload);
    }

    <T> Completable publish(String topic, T message);
}
