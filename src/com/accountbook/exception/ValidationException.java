package com.accountbook.exception;

/**
 * 输入校验异常：日期为空/格式错、类型为空、类别为空、金额 &lt;= 0、备注超长等。
 *
 * <p>继承自 {@link BusinessException}（作业异常表规定），
 * 因此捕获时<b>必须先捕获本类再捕获 BusinessException</b>，
 * 否则校验错误会被当成业务错误处理。</p>
 */
public class ValidationException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
