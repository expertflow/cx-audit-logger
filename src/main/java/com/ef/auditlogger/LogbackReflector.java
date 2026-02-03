package com.ef.auditlogger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import org.slf4j.Logger;

public class LogbackReflector {
    private Method toLevelMethod;
    private Constructor<?> eventConstructor;
    private Method setCallerMethod;
    private Method callAppendersMethod;
    private boolean available;

    LogbackReflector() {
        try {
            Class<?> levelClass = Class.forName("ch.qos.logback.classic.Level");
            Class<?> loggerClass = Class.forName("ch.qos.logback.classic.Logger");
            Class<?> eventClass = Class.forName("ch.qos.logback.classic.spi.LoggingEvent");
            Class<?> iLoggingEvent = Class.forName("ch.qos.logback.classic.spi.ILoggingEvent");

            toLevelMethod = levelClass.getMethod("toLevel", String.class);
            eventConstructor = eventClass.getConstructor(
                    String.class, loggerClass, levelClass, String.class, Throwable.class, Object[].class
            );
            setCallerMethod = eventClass.getMethod("setCallerData", StackTraceElement[].class);
            callAppendersMethod = loggerClass.getMethod("callAppenders", iLoggingEvent);
            available = true;
        } catch (Throwable t) {
            available = false;
        }
    }

    boolean isAvailable() {
        return available;
    }

    void log(Logger logger, String levelStr, String message, StackTraceElement caller) {
        try {
            Object logbackLevel = toLevelMethod.invoke(null, levelStr != null ? levelStr.toUpperCase() : "INFO");
            Object event = eventConstructor.newInstance(
                    "ch.qos.logback.classic.Logger", logger, logbackLevel, message, null, null
            );
            setCallerMethod.invoke(event, (Object) new StackTraceElement[]{caller});
            callAppendersMethod.invoke(logger, event);
        } catch (Exception e) {
            logger.info(message);
        }
    }
}