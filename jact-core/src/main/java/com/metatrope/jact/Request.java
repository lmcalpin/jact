package com.metatrope.jact;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Request {
    private final Map<Class<?>, Consumer<?>> requestHandlerMap;
    private Consumer<?> defaultHandler;

    public Request(Map<Class<?>, Consumer<?>> requestHandlerMap, Consumer<?> defaultHandler) {
        this.requestHandlerMap = requestHandlerMap;
        this.defaultHandler = defaultHandler;
    }

    static class RequestBuilder {
        private final Map<Class<?>, Consumer<?>> requestHandlerMap;
        private Consumer<?> defaultHandler;

        public RequestBuilder() {
            this.requestHandlerMap = new HashMap<>();
            this.defaultHandler = null;
        }

        public <T> void whenMatch(Class<T> clazz, Consumer<T> action) {
            requestHandlerMap.put(clazz, action);
        }

        public void defaultHandler(Consumer<?> action) {
            this.defaultHandler = action;
        }

        public Request build() {
            return new Request(requestHandlerMap, defaultHandler);
        }
    }

    public static RequestBuilder builder() {
        return new RequestBuilder();
    }

    @SuppressWarnings("unchecked")
    public <T> void process(T payload) {
        Class<?> payloadClass = payload.getClass();
        Consumer<T> action = (Consumer<T>) requestHandlerMap.get(payloadClass);
        if (action == null) {
            action = (Consumer<T>) defaultHandler;
        }
        action.accept(payload);
    }
}
