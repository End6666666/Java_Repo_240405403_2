package com.accountbook.service;

import com.accountbook.domain.Entry;
import com.accountbook.domain.EntryType;
import com.accountbook.domain.QueryResult;
import com.accountbook.exception.BusinessException;
import com.accountbook.exception.StorageException;
import com.accountbook.exception.ValidationException;
import com.accountbook.repository.EntryRepository;
import com.accountbook.util.LogUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 内存缓存 + 文件持久化的业务实现。
 *
 * <p>缓存与文件的一致性策略：先改缓存，再整份写回文件；
 * 写回失败就把缓存改回去（回滚），保证内存里看到的状态和文件里的一致。
 * 不做回滚会出现“界面显示已删除，重启后记录又回来了”这种鬼故事。</p>
 */
public class AccountBookServiceImpl implements AccountBookService {

    private final EntryRepository repository;

    /** 内存缓存，ArrayList 按作业要求 */
    private final List<Entry> cache;

    public AccountBookServiceImpl(EntryRepository repository) throws StorageException {
        this.repository = Objects.requireNonNull(repository, "repository 不能为空");
        this.cache = new ArrayList<>(repository.load());
        LogUtil.info("记账本服务初始化完成，当前 " + cache.size() + " 条记录");
    }

    @Override
    public void add(Entry entry) throws ValidationException, BusinessException, StorageException {
        if (entry == null) {
            throw new ValidationException("记录不能为空");
        }
        entry.validate();

        if (isBlank(entry.getId())) {
            entry.setId(nextId());
        } else if (indexOf(entry.getId()) >= 0) {
            throw new BusinessException("编号 " + entry.getId() + " 已存在，不能重复录入");
        }

        cache.add(entry);
        try {
            persist();
        } catch (StorageException e) {
            cache.remove(entry);
            throw e;
        }
        LogUtil.info("新增记录：" + entry);
    }

    @Override
    public void update(Entry entry) throws ValidationException, BusinessException, StorageException {
        if (entry == null) {
            throw new ValidationException("记录不能为空");
        }
        entry.validate();

        int index = indexOf(entry.getId());
        if (index < 0) {
            throw new BusinessException("编号 " + entry.getId() + " 的记录不存在，无法修改");
        }

        Entry old = cache.set(index, entry);
        try {
            persist();
        } catch (StorageException e) {
            cache.set(index, old);
            throw e;
        }
        LogUtil.info("修改记录：" + entry);
    }

    @Override
    public void delete(String id) throws BusinessException, StorageException {
        int index = indexOf(id);
        if (index < 0) {
            throw new BusinessException("编号 " + id + " 的记录不存在，无法删除");
        }

        Entry removed = cache.remove(index);
        try {
            persist();
        } catch (StorageException e) {
            cache.add(index, removed);
            throw e;
        }
        LogUtil.info("删除记录：" + removed);
    }

    @Override
    public List<Entry> listAll() {
        return Collections.unmodifiableList(new ArrayList<>(cache));
    }

    @Override
    public QueryResult search(String keyword, EntryType type) {
        String needle = (keyword == null) ? "" : keyword.trim().toLowerCase();
        List<Entry> hits = new ArrayList<>();

        for (Entry entry : cache) {
            if (type != null && entry.getType() != type) {
                continue;
            }
            if (!needle.isEmpty() && !matches(entry, needle)) {
                continue;
            }
            hits.add(entry);
        }

        QueryResult result = new QueryResult();
        result.setEntries(hits);
        LogUtil.info("查询 keyword='" + keyword + "', type=" + type
                + " 命中 " + result.getCount() + " 条");
        return result;
    }

    @Override
    public void reload() throws StorageException {
        // 先读到新数据再替换，读失败时保留原缓存，不至于把界面清空
        List<Entry> fresh = repository.load();
        cache.clear();
        cache.addAll(fresh);
        LogUtil.info("已重新加载，当前 " + cache.size() + " 条记录");
    }

    /** 关键字匹配类别或备注，忽略大小写。 */
    private boolean matches(Entry entry, String needle) {
        return contains(entry.getCategory(), needle) || contains(entry.getNote(), needle);
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase().contains(needle);
    }

    /** 改动缓冲后整份写回文件。 */
    private void persist() throws StorageException {
        repository.save(new ArrayList<>(cache));
    }

    /** 按 id 找下标，找不到返回 -1。 */
    private int indexOf(String id) {
        if (id == null) {
            return -1;
        }
        for (int i = 0; i < cache.size(); i++) {
            if (id.equals(cache.get(i).getId())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 生成一个未被占用的编号。
     * 用时间戳而不是自增序号：删掉中间某条后再新增，自增序号可能撞上已有编号。
     */
    private String nextId() {
        long stamp = System.currentTimeMillis();
        String candidate = "E" + stamp;
        while (indexOf(candidate) >= 0) {
            stamp++;
            candidate = "E" + stamp;
        }
        return candidate;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
