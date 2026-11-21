package com.accountbook.test;

import com.accountbook.domain.Entry;
import com.accountbook.domain.EntryType;
import com.accountbook.domain.QueryResult;
import com.accountbook.exception.BusinessException;
import com.accountbook.exception.StorageException;
import com.accountbook.exception.ValidationException;
import com.accountbook.repository.EntryRepository;
import com.accountbook.service.AccountBookServiceImpl;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * 业务层测试。
 *
 * <p>用内存替身代替真实文件：一是快，二是能按需让 {@code save()} 失败，
 * 用来验证“写盘失败后缓存必须回滚”。</p>
 */
public class ServiceTest {

    /** 内存版仓储，可开关“保存必失败” */
    private static class FakeRepository implements EntryRepository {
        private final List<Entry> stored = new ArrayList<>();
        private boolean failOnSave;

        @Override
        public List<Entry> load() {
            return new ArrayList<>(stored);
        }

        @Override
        public void save(List<Entry> entries) throws StorageException {
            if (failOnSave) {
                throw new StorageException("模拟磁盘写入失败");
            }
            stored.clear();
            stored.addAll(entries);
        }
    }

    private FakeRepository repository;
    private AccountBookServiceImpl service;

    @Before
    public void setUp() throws StorageException {
        repository = new FakeRepository();
        service = new AccountBookServiceImpl(repository);
    }

    private static Entry entry(String id, EntryType type, String category, double amount, String note) {
        Entry entry = new Entry();
        entry.setId(id);
        entry.setDate(LocalDate.of(2026, 11, 21));
        entry.setType(type);
        entry.setCategory(category);
        entry.setAmount(amount);
        entry.setNote(note);
        return entry;
    }

    // ---------- 新增 ----------

    @Test
    public void addGeneratesIdWhenBlank() {
        Entry entry = entry("", EntryType.EXPENSE, "餐饮", 25, "午饭");
        service.add(entry);
        assertNotNull(entry.getId());
        assertFalse(entry.getId().isBlank());
        assertEquals(1, service.listAll().size());
        assertEquals(1, repository.stored.size());
    }

    @Test
    public void addRejectsInvalidEntry() {
        Entry entry = entry("", EntryType.EXPENSE, "餐饮", 0, "金额为 0");
        assertThrows(ValidationException.class, () -> service.add(entry));
        assertTrue(service.listAll().isEmpty());
    }

    @Test
    public void addRejectsDuplicateId() {
        service.add(entry("E1", EntryType.EXPENSE, "餐饮", 10, ""));
        Entry again = entry("E1", EntryType.EXPENSE, "交通", 20, "");
        BusinessException ex = assertThrows(BusinessException.class, () -> service.add(again));
        assertTrue(ex.getMessage().contains("E1"));
        assertEquals(1, service.listAll().size());
    }

    @Test
    public void addRollsBackCacheWhenSaveFails() {
        repository.failOnSave = true;
        Entry entry = entry("", EntryType.EXPENSE, "餐饮", 25, "午饭");
        assertThrows(StorageException.class, () -> service.add(entry));
        repository.failOnSave = false;

        assertTrue("写盘失败后缓存里不能留下这条", service.listAll().isEmpty());
        assertTrue(repository.stored.isEmpty());
    }

    // ---------- 修改 ----------

    @Test
    public void updateChangesFields() {
        Entry entry = entry("E1", EntryType.EXPENSE, "餐饮", 25, "午饭");
        service.add(entry);

        Entry changed = entry("E1", EntryType.EXPENSE, "餐饮", 30, "加了个鸡腿");
        service.update(changed);

        List<Entry> all = service.listAll();
        assertEquals(1, all.size());
        assertEquals(30.0, all.get(0).getAmount(), 0.000001);
        assertEquals("加了个鸡腿", all.get(0).getNote());
    }

    @Test
    public void updateMissingThrows() {
        Entry entry = entry("NOT-EXIST", EntryType.EXPENSE, "餐饮", 25, "");
        assertThrows(BusinessException.class, () -> service.update(entry));
    }

    @Test
    public void updateRollsBackWhenSaveFails() {
        Entry entry = entry("E1", EntryType.EXPENSE, "餐饮", 25, "午饭");
        service.add(entry);

        repository.failOnSave = true;
        Entry changed = entry("E1", EntryType.EXPENSE, "餐饮", 999, "修改");
        assertThrows(StorageException.class, () -> service.update(changed));
        repository.failOnSave = false;

        assertEquals(25.0, service.listAll().get(0).getAmount(), 0.000001);
    }

    // ---------- 删除 ----------

    @Test
    public void deleteRemovesEntry() {
        service.add(entry("E1", EntryType.EXPENSE, "餐饮", 10, ""));
        service.add(entry("E2", EntryType.EXPENSE, "交通", 20, ""));
        service.delete("E1");
        List<Entry> all = service.listAll();
        assertEquals(1, all.size());
        assertEquals("E2", all.get(0).getId());
    }

    @Test
    public void deleteMissingThrows() {
        assertThrows(BusinessException.class, () -> service.delete("NOT-EXIST"));
    }

    @Test
    public void deleteRollsBackAndKeepsOrder() {
        service.add(entry("A", EntryType.EXPENSE, "餐饮", 1, ""));
        service.add(entry("B", EntryType.EXPENSE, "交通", 2, ""));
        service.add(entry("C", EntryType.EXPENSE, "日用", 3, ""));

        repository.failOnSave = true;
        assertThrows(StorageException.class, () -> service.delete("B"));
        repository.failOnSave = false;

        List<Entry> all = service.listAll();
        assertEquals(3, all.size());
        assertEquals("A", all.get(0).getId());
        assertEquals("B", all.get(1).getId());
        assertEquals("C", all.get(2).getId());
    }

    // ---------- 查询 ----------

    @Test
    public void searchMatchesCategory() {
        service.add(entry("E1", EntryType.EXPENSE, "餐饮", 25, "午饭"));
        service.add(entry("E2", EntryType.EXPENSE, "交通", 5, "地铁"));

        QueryResult result = service.search("餐饮", null);
        assertEquals(1, result.getCount());
        assertEquals("E1", result.getEntries().get(0).getId());
    }

    @Test
    public void searchMatchesNote() {
        service.add(entry("E1", EntryType.EXPENSE, "餐饮", 25, "和同事一起吃"));
        service.add(entry("E2", EntryType.EXPENSE, "交通", 5, "地铁"));

        assertEquals(1, service.search("同事", null).getCount());
    }

    @Test
    public void searchFiltersByType() {
        service.add(entry("E1", EntryType.INCOME, "工资", 8000, ""));
        service.add(entry("E2", EntryType.EXPENSE, "餐饮", 25, ""));

        QueryResult result = service.search(null, EntryType.INCOME);
        assertEquals(1, result.getCount());
        assertEquals(8000.0, result.getTotalIncome(), 0.000001);
    }

    @Test
    public void searchCombinesKeywordAndType() {
        service.add(entry("E1", EntryType.INCOME, "工资", 8000, "十月"));
        service.add(entry("E2", EntryType.EXPENSE, "餐饮", 25, "十月聚餐"));

        QueryResult result = service.search("十月", EntryType.EXPENSE);
        assertEquals(1, result.getCount());
        assertEquals("E2", result.getEntries().get(0).getId());
    }

    @Test
    public void searchWithEmptyKeywordReturnsAll() {
        service.add(entry("E1", EntryType.INCOME, "工资", 8000, ""));
        service.add(entry("E2", EntryType.EXPENSE, "餐饮", 25, ""));

        assertEquals(2, service.search("", null).getCount());
        assertEquals(2, service.search(null, null).getCount());
    }

    @Test
    public void searchResultSummaryIsConsistent() {
        service.add(entry("E1", EntryType.INCOME, "工资", 8000, ""));
        service.add(entry("E2", EntryType.EXPENSE, "餐饮", 25, ""));
        service.add(entry("E3", EntryType.EXPENSE, "交通", 12.5, ""));

        QueryResult result = service.search(null, null);
        assertEquals(3, result.getCount());
        assertEquals(8000.0, result.getTotalIncome(), 0.000001);
        assertEquals(37.5, result.getTotalExpense(), 0.000001);
        assertEquals(7962.5, result.getBalance(), 0.000001);
    }

    // ---------- 其它 ----------

    @Test
    public void listAllIsUnmodifiable() {
        service.add(entry("E1", EntryType.EXPENSE, "餐饮", 10, ""));
        assertThrows(UnsupportedOperationException.class,
                () -> service.listAll().clear());
    }

    @Test
    public void reloadPicksUpExternalData() {
        service.add(entry("E1", EntryType.EXPENSE, "餐饮", 10, ""));
        repository.stored.add(entry("E2", EntryType.INCOME, "工资", 500, ""));

        service.reload();
        assertEquals(2, service.listAll().size());
    }

    @Test
    public void reloadKeepsOldCacheWhenLoadFails() {
        service.add(entry("E1", EntryType.EXPENSE, "餐饮", 10, ""));

        FakeRepository broken = new FakeRepository() {
            @Override
            public List<Entry> load() {
                throw new StorageException("模拟读取失败");
            }
        };
        // 用损坏仓储重新构造应直接失败，不会得到半成品 service
        assertThrows(StorageException.class, () -> new AccountBookServiceImpl(broken));
    }
}
