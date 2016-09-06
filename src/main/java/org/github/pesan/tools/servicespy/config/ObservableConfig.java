package org.github.pesan.tools.servicespy.config;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import rx.Observable;

@Configuration
public class ObservableConfig extends WebMvcConfigurerAdapter {
    private @Autowired RequestMappingHandlerAdapter requestMappingHandlerAdapter;

    @Value("${server.timeout:2000}")
    private long timeout;

    @PostConstruct
    public void init() {
        List<HandlerMethodReturnValueHandler> handlers = new ArrayList<>();
        handlers.add(new ObservableReturnValueHandler(timeout));
        handlers.addAll(requestMappingHandlerAdapter.getReturnValueHandlers());
        requestMappingHandlerAdapter.setReturnValueHandlers(handlers);
    }

    private static class ObservableReturnValueHandler implements HandlerMethodReturnValueHandler {
        private final long timeout;

        public ObservableReturnValueHandler(long timeout) {
            this.timeout = timeout;
        }

        @Override
        public boolean supportsReturnType(MethodParameter returnType) {
            return Observable.class.isAssignableFrom(returnType.getParameterType());
        }

        @Override
        public void handleReturnValue(Object returnValue, MethodParameter returnType, ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {
            if (returnValue == null) {
                return;
            }
            Observable<Object> observable = Observable.class.cast(returnValue);
            WebAsyncUtils.getAsyncManager(webRequest)
                    .startDeferredResultProcessing(asDeferredResult(observable, webRequest), mavContainer);
        }

        private DeferredResult<Object> asDeferredResult(Observable<Object> observable, NativeWebRequest request) {
            DeferredResult<Object> deferred = new DeferredResult<>(timeout);
            observable
                .singleOrDefault(HttpStatus.NOT_FOUND)
                .subscribe(result -> {
                    if (HttpStatus.class.equals(result.getClass())) {
                        request.getNativeResponse(HttpServletResponse.class).setStatus(((HttpStatus) result).value());
                        setStatusCode(request, (HttpStatus) result);
                        deferred.setResult(null);
                    } else {
                        deferred.setResult(result);
                    }
                }, deferred::setErrorResult);
            return deferred;
        }

        private static void setStatusCode(NativeWebRequest request, HttpStatus status) {
            request.getNativeResponse(HttpServletResponse.class).setStatus(status.value());
        }
    }
}

