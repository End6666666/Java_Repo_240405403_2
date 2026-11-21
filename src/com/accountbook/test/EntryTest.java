package com.accountbook.test;

import com.accountbook.domain.Entry;
import com.accountbook.domain.EntryType;
import com.accountbook.exception.ValidationException;
import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Entry 基础字段校验。
 *
 * <p>三类覆盖：正常输入通过、边界值（刚好的长度、最小正金额）、非法输入抛
 * {@link ValidationException}。</p>
 */
public class EntryTest {

    private static Entry validEntry() {
        Entry entry = new Entry();
        entry.setId("E001");
        entry.setDate(LocalDate.of(2026, 11, 21));
        entry.setType(EntryType.EXPENSE);
        entry.setCategory("餐饮");
        entry.setAmount(25.5);
        entry.setNote("午饭");
        return entry;
    }

    private static String repeat(char ch, int times) {
        StringBuilder sb = new StringBuilder(times);
        for (int i = 0; i < times; i++) {
            sb.append(ch);
        }
        return sb.toString();
    }

    // ---------- 正常 ----------

    @Test
    public void validEntryPassesValidate() {
        validEntry().validate();
    }

    @Test
    public void noteCanBeNull() {
        Entry entry = validEntry();
        entry.setNote(null);
        entry.validate();
    }

    // ---------- 边界 ----------

    @Test
    public void amountOneCentPasses() {
        Entry entry = validEntry();
        entry.setAmount(0.01);
        entry.validate();
    }

    @Test
    public void categoryExactlyMaxLengthPasses() {
        Entry entry = validEntry();
        entry.setCategory(repeat('类', Entry.MAX_CATEGORY_LENGTH));
        assertEquals(Entry.MAX_CATEGORY_LENGTH, entry.getCategory().length());
        entry.validate();
    }

    @Test
    public void categoryOverMaxLengthFails() {
        Entry entry = validEntry();
        entry.setCategory(repeat('类', Entry.MAX_CATEGORY_LENGTH + 1));
        ValidationException ex = assertThrows(ValidationException.class, entry::validate);
        assertTrue(ex.getMessage().contains(String.valueOf(Entry.MAX_CATEGORY_LENGTH)));
    }

    @Test
    public void noteExactlyMaxLengthPasses() {
        Entry entry = validEntry();
        entry.setNote(repeat('备', Entry.MAX_NOTE_LENGTH));
        assertEquals(Entry.MAX_NOTE_LENGTH, entry.getNote().length());
        entry.validate();
    }

    @Test
    public void noteOverMaxLengthFails() {
        Entry entry = validEntry();
        entry.setNote(repeat('备', Entry.MAX_NOTE_LENGTH + 1));
        ValidationException ex = assertThrows(ValidationException.class, entry::validate);
        assertTrue(ex.getMessage().contains(String.valueOf(Entry.MAX_NOTE_LENGTH)));
    }

    // ---------- 异常 ----------

    @Test
    public void nullDateFails() {
        Entry entry = validEntry();
        entry.setDate(null);
        assertThrows(ValidationException.class, entry::validate);
    }

    @Test
    public void nullTypeFails() {
        Entry entry = validEntry();
        entry.setType(null);
        assertThrows(ValidationException.class, entry::validate);
    }

    @Test
    public void blankCategoryFails() {
        Entry entry = validEntry();
        entry.setCategory("   ");
        assertThrows(ValidationException.class, entry::validate);
    }

    @Test
    public void nullCategoryFails() {
        Entry entry = validEntry();
        entry.setCategory(null);
        assertThrows(ValidationException.class, entry::validate);
    }

    @Test
    public void zeroAmountFails() {
        Entry entry = validEntry();
        entry.setAmount(0);
        ValidationException ex = assertThrows(ValidationException.class, entry::validate);
        assertTrue(ex.getMessage().contains("金额"));
    }

    @Test
    public void negativeAmountFails() {
        Entry entry = validEntry();
        entry.setAmount(-10);
        assertThrows(ValidationException.class, entry::validate);
    }

    @Test
    public void notANumberFails() {
        Entry entry = validEntry();
        entry.setAmount(Double.NaN);
        assertThrows(ValidationException.class, entry::validate);
    }

    @Test
    public void infinityFails() {
        Entry entry = validEntry();
        entry.setAmount(Double.POSITIVE_INFINITY);
        assertThrows(ValidationException.class, entry::validate);
    }

    // ---------- 金额规整 ----------

    @Test
    public void round2KeepsTwoDecimals() {
        assertEquals(0.3, Entry.round2(0.1 + 0.2), 0.000001);
        assertEquals(1.01, Entry.round2(1.005), 0.000001);
        assertEquals(-2.35, Entry.round2(-2.345), 0.000001);
    }
}
