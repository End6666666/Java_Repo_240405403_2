package com.accountbook.test;

import com.accountbook.domain.Entry;
import com.accountbook.domain.EntryType;
import com.accountbook.exception.StorageException;
import com.accountbook.repository.FileEntryRepository;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * CSV 持久化测试：用真实的临时文件，不 mock 文件系统。
 */
public class RepositoryTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Path file;
    private FileEntryRepository repository;

    @Before
    public void setUp() throws IOException {
        Path dir = folder.newFolder("data").toPath();
        file = dir.resolve("entries.csv");
        repository = new FileEntryRepository(file);
    }

    private static Entry entry(String id, EntryType type, String category,
                               double amount, String note) {
        Entry entry = new Entry();
        entry.setId(id);
        entry.setDate(LocalDate.of(2026, 11, 21));
        entry.setType(type);
        entry.setCategory(category);
        entry.setAmount(amount);
        entry.setNote(note);
        return entry;
    }

    // ---------- 正常 ----------

    @Test
    public void saveThenLoadRoundTrip() throws IOException {
        repository.save(List.of(
                entry("E1", EntryType.INCOME, "工资", 8000, "十月"),
                entry("E2", EntryType.EXPENSE, "餐饮", 37.5, "午饭")));

        List<Entry> loaded = repository.load();
        assertEquals(2, loaded.size());
        assertEquals("E1", loaded.get(0).getId());
        assertEquals(EntryType.INCOME, loaded.get(0).getType());
        assertEquals("工资", loaded.get(0).getCategory());
        assertEquals(8000.0, loaded.get(0).getAmount(), 0.000001);
        assertEquals("E2", loaded.get(1).getId());
    }

    @Test
    public void emptyFileLoadsAsEmptyList() throws IOException {
        Files.createFile(file);
        assertTrue(repository.load().isEmpty());
    }

    @Test
    public void saveEmptyListEmptiesTheFile() throws IOException {
        repository.save(List.of(entry("E1", EntryType.EXPENSE, "餐饮", 1, "")));
        repository.save(Collections.emptyList());
        assertTrue(repository.load().isEmpty());
    }

    @Test
    public void overwriteReplacesOldContent() throws IOException {
        repository.save(List.of(entry("E1", EntryType.EXPENSE, "餐饮", 1, "")));
        repository.save(List.of(
                entry("E2", EntryType.INCOME, "工资", 2, ""),
                entry("E3", EntryType.EXPENSE, "交通", 3, "")));

        List<Entry> loaded = repository.load();
        assertEquals(2, loaded.size());
        assertEquals("E2", loaded.get(0).getId());
    }

    @Test
    public void blankLinesAreSkipped() throws IOException {
        Files.write(file, List.of(
                "E1,2026-11-21,INCOME,工资,100,备注",
                "",
                "   ",
                "E2,2026-11-21,EXPENSE,餐饮,20,"),
                StandardCharsets.UTF_8);
        assertEquals(2, repository.load().size());
    }

    // ---------- 转义 ----------

    @Test
    public void commaInsideNoteSurvivesRoundTrip() {
        repository.save(List.of(entry("E1", EntryType.EXPENSE, "日用", 9.9, "牙膏,牙刷")));
        List<Entry> loaded = repository.load();
        assertEquals("牙膏,牙刷", loaded.get(0).getNote());
    }

    @Test
    public void quoteInsideNoteSurvivesRoundTrip() {
        repository.save(List.of(entry("E1", EntryType.EXPENSE, "日用", 9.9, "他说\"还行\"")));
        List<Entry> loaded = repository.load();
        assertEquals("他说\"还行\"", loaded.get(0).getNote());
    }

    @Test
    public void commaInsideCategorySurvivesRoundTrip() {
        repository.save(List.of(entry("E1", EntryType.EXPENSE, "餐饮,零食", 9.9, "")));
        assertEquals("餐饮,零食", repository.load().get(0).getCategory());
    }

    @Test
    public void newlineInsideNoteSurvivesRoundTrip() {
        repository.save(List.of(
                entry("E1", EntryType.EXPENSE, "餐饮", 9.9, "第一行\n第二行"),
                entry("E2", EntryType.INCOME, "工资", 100, "")));
        List<Entry> loaded = repository.load();
        assertEquals(2, loaded.size());
        assertEquals("第一行\n第二行", loaded.get(0).getNote());
        assertEquals("E2", loaded.get(1).getId());
    }

    @Test
    public void amountIsWrittenAsPlainDecimal() throws IOException {
        repository.save(List.of(entry("E1", EntryType.INCOME, "工资", 10000000, "")));
        String content = Files.readString(file, StandardCharsets.UTF_8);
        assertFalse("不应写出科学计数法：" + content, content.contains("E7"));
        assertTrue(content.contains("10000000"));
    }

    // ---------- 异常 ----------

    @Test
    public void missingFileReturnsEmptyListInsteadOfThrowing() {
        assertTrue(repository.load().isEmpty());
    }

    @Test
    public void wrongFieldCountThrowsStorage() throws IOException {
        Files.write(file, List.of("E1,2026-11-21,INCOME"), StandardCharsets.UTF_8);
        StorageException ex = assertThrows(StorageException.class, repository::load);
        assertTrue("消息里要能看出是第几行：" + ex.getMessage(), ex.getMessage().contains("第 1 行"));
    }

    @Test
    public void badDateThrowsStorage() throws IOException {
        Files.write(file, List.of("E1,2026/11/21,INCOME,工资,100,"), StandardCharsets.UTF_8);
        assertThrows(StorageException.class, repository::load);
    }

    @Test
    public void unknownTypeThrowsStorage() throws IOException {
        Files.write(file, List.of("E1,2026-11-21,UNKNOWN,工资,100,"), StandardCharsets.UTF_8);
        assertThrows(StorageException.class, repository::load);
    }

    @Test
    public void badAmountThrowsStorage() throws IOException {
        Files.write(file, List.of("E1,2026-11-21,INCOME,工资,abc,"), StandardCharsets.UTF_8);
        assertThrows(StorageException.class, repository::load);
    }

    @Test
    public void emptyIdThrowsStorage() throws IOException {
        Files.write(file, List.of(",2026-11-21,INCOME,工资,100,"), StandardCharsets.UTF_8);
        assertThrows(StorageException.class, repository::load);
    }

    @Test
    public void missingDirectoryIsCreatedOnSave() throws IOException {
        Path deep = folder.getRoot().toPath().resolve("a/b/c/entries.csv");
        FileEntryRepository deepRepo = new FileEntryRepository(deep);
        deepRepo.save(new ArrayList<>(List.of(entry("E1", EntryType.INCOME, "工资", 1, ""))));
        assertTrue(Files.exists(deep));
        assertEquals(1, deepRepo.load().size());
    }
}
