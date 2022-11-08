package org.github.pesan.platform.rx;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.processors.FlowableProcessor;
import io.reactivex.rxjava3.processors.ReplayProcessor;
import org.github.pesan.platform.messaging.MessageBus;

public class RxMessageBus implements MessageBus {
    private final FlowableProcessor<Envelope> bus = ReplayProcessor.<Envelope>createWithSize(256).toSerialized();

    private static final ObjectMapper PAYLOAD_MAPPER = new ObjectMapper()
            .registerModule(new Jdk8Module())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    public <T> Flowable<T> consumer(Class<T> type, String topic) {
        return bus.filter(msg -> topic.equals(msg.address()))
                .map(msg -> PAYLOAD_MAPPER.readValue(msg.payload(), type));
    }

    @Override
    public <T> Completable publish(String topic, T message) {
        return Completable.fromAction(() -> bus.onNext(new Envelope(topic, PAYLOAD_MAPPER.writeValueAsString(message))));
    }

    private record Envelope(String address, String payload) {
    }
}
