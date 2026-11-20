package com.accountbook.exception;

/**
 * 业务异常：ID 不存在、重复删除、业务状态不允许等。
 * 由 Service 抛出，Controller 捕获后向用户显示可读提示。
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
