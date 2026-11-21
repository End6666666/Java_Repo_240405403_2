package com.accountbook.repository;

import com.accountbook.domain.Entry;
import com.accountbook.exception.StorageException;

import java.util.List;

/**
 * 持久化抽象：只管“把记录读出来”和“把记录写回去”。
 *
 * <p>不做业务校验、不弹窗、不碰 Swing。具体实现可以是文件、数据库，
 * 本实验里是 {@link FileEntryRepository}（CSV 文本文件）。</p>
 */
public interface EntryRepository {

    /**
     * 加载全部记录。
     *
     * <p>约定：账本文件不存在属于“首次使用”，返回空列表，不抛异常；
     * 其余 IO 失败或文件内容损坏才抛 {@link StorageException}。</p>
     *
     * @return 记录列表，永不为 null
     * @throws StorageException 文件读取失败或内容格式损坏
     */
    List<Entry> load() throws StorageException;

    /**
     * 覆盖写入全部记录。
     *
     * @param entries 要保存的记录，null 视为空列表
     * @throws StorageException 写入失败
     */
    void save(List<Entry> entries) throws StorageException;
}
