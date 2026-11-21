package com.accountbook.service;

import com.accountbook.domain.Entry;
import com.accountbook.domain.EntryType;
import com.accountbook.domain.QueryResult;
import com.accountbook.exception.BusinessException;
import com.accountbook.exception.StorageException;
import com.accountbook.exception.ValidationException;

import java.util.List;

/**
 * 业务接口：记账本的增删改查。
 *
 * <p>它不关心数据存在文件还是数据库，也不关心界面长什么样。</p>
 */
public interface AccountBookService {

    /**
     * 新增一条记录。
     *
     * @throws ValidationException 字段不合法（金额 ≤ 0、日期为空等）
     * @throws BusinessException   id 重复
     * @throws StorageException    保存到文件失败
     */
    void add(Entry entry) throws ValidationException, BusinessException, StorageException;

    /**
     * 按 id 修改一条记录。
     *
     * @throws ValidationException 字段不合法
     * @throws BusinessException   id 不存在
     * @throws StorageException    保存到文件失败
     */
    void update(Entry entry) throws ValidationException, BusinessException, StorageException;

    /**
     * 按 id 删除一条记录。
     *
     * @throws BusinessException id 不存在
     * @throws StorageException  保存到文件失败
     */
    void delete(String id) throws BusinessException, StorageException;

    /**
     * 列出全部记录。
     *
     * @return 只读快照，调用方改不动内部缓存
     */
    List<Entry> listAll();

    /**
     * 查询：关键字与类型可以只用其中一个，也可以组合。
     *
     * @param keyword 匹配类别或备注，忽略大小写；空串或 null 表示不限
     * @param type    限定收入/支出；null 表示不限
     * @return 命中明细与汇总，永不为 null
     */
    QueryResult search(String keyword, EntryType type);

    /**
     * 丢弃内存缓存，从文件重新加载。
     *
     * @throws StorageException 读取失败
     */
    void reload() throws StorageException;
}
