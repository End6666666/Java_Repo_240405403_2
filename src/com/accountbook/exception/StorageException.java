package com.accountbook.exception;

/**
 * 存储异常：文件不存在、IO 失败、保存失败、文件内容格式损坏。
 * 由 {@code FileEntryRepository} 抛出，Controller 捕获后提示
 * “数据保存/读取失败，请检查文件”，并用 {@code LogUtil.error} 记录。
 */
public class StorageException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
