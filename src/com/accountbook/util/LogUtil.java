package com.accountbook.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * 日志封装：同时输出到控制台与 logs/app.log。
 *
 * <p>只做日志，不持有任何业务知识。</p>
 */
public final class LogUtil {

    private static final Logger LOGGER = Logger.getLogger("AccountBook");

    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static {
        LOGGER.setUseParentHandlers(true);
        try {
            File dir = new File("logs");
            if (!dir.exists() && !dir.mkdirs()) {
                System.err.println("[LogUtil] 无法创建日志目录: " + dir.getAbsolutePath());
            }
            FileHandler handler = new FileHandler("logs/app.log", true);
            handler.setEncoding("UTF-8");
            handler.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    StringBuilder sb = new StringBuilder();
                    sb.append('[').append(LocalDateTime.now().format(TS)).append("] ");
                    sb.append('[').append(record.getLevel().getName()).append("] ");
                    sb.append(formatMessage(record)).append(System.lineSeparator());
                    if (record.getThrown() != null) {
                        StringWriter sw = new StringWriter();
                        record.getThrown().printStackTrace(new PrintWriter(sw));
                        sb.append(sw);
                    }
                    return sb.toString();
                }
            });
            LOGGER.addHandler(handler);
        } catch (IOException e) {
            System.err.println("[LogUtil] 日志文件初始化失败: " + e.getMessage());
        }
    }

    private LogUtil() {
    }

    public static void info(String message) {
        LOGGER.info(message);
    }

    public static void warn(String message) {
        LOGGER.warning(message);
    }

    public static void error(String message, Throwable throwable) {
        LOGGER.log(Level.SEVERE, message, throwable);
    }

    /** 没有异常对象时只记消息，避免调用方为了凑参数传 null */
    public static void error(String message) {
        LOGGER.severe(message);
    }
}
