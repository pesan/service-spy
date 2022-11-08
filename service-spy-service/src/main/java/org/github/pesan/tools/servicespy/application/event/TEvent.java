package org.github.pesan.tools.servicespy.application.event;

import org.github.pesan.tools.servicespy.application.ExceptionDetails;
import org.github.pesan.tools.servicespy.application.RequestId;

public sealed interface TEvent {

    RequestId id();

    record RequestBegin(RequestId id, RequestDataEntryDto payload) implements TEvent {}
    record RequestData(RequestId id, byte[] payload) implements TEvent {}
    record RequestEnd(RequestId id) implements TEvent {}
    record RequestError(RequestId id, ExceptionDetails payload) implements TEvent {}

    record ResponseBegin(RequestId id, ResponseDataEntryDto payload) implements TEvent {}
    record ResponseData(RequestId id, byte[] payload) implements TEvent {}
    record ResponseEnd(RequestId id) implements TEvent {}
    record ResponseError(RequestId id, ExceptionDetails payload) implements TEvent {}
}
