package org.azertio.cli;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import org.slf4j.LoggerFactory;
import java.nio.file.Path;

class LogConfig {

    private LogConfig() {}

    static void redirectToFile(Path logFile) {
        var ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        ctx.reset();

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(ctx);
        encoder.setPattern("%date{HH:mm:ss.SSS} %-5level %msg%n");
        encoder.start();

        RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
        appender.setContext(ctx);
        appender.setFile(logFile.toString());
        appender.setEncoder(encoder);

        SizeAndTimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new SizeAndTimeBasedRollingPolicy<>();
        rollingPolicy.setContext(ctx);
        rollingPolicy.setFileNamePattern(logFile + ".%d{yyyy-MM-dd}.%i.gz");
        rollingPolicy.setMaxFileSize(FileSize.valueOf("5MB"));
        rollingPolicy.setMaxHistory(3);
        rollingPolicy.setTotalSizeCap(FileSize.valueOf("20MB"));
        rollingPolicy.setParent(appender);
        rollingPolicy.start();

        appender.setRollingPolicy(rollingPolicy);
        appender.start();

        var root = ctx.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
        root.addAppender(appender);

        var azertio = ctx.getLogger("org.azertio");
        azertio.setLevel(Level.INFO);
    }
}