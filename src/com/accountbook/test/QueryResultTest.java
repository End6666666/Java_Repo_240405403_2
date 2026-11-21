package com.accountbook.test;

import com.accountbook.domain.Entry;
import com.accountbook.domain.EntryType;
import com.accountbook.domain.QueryResult;
import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * QueryResult 汇总计算。
 *
 * <p>重点验证：余额是现算的派生量、空结果不出现 NaN、double 求和不产生 0.30000000000000004。</p>
 */
public class QueryResultTest {

    private static Entry entry(EntryType type, double amount) {
        Entry entry = new Entry();
        entry.setId("E" + System.nanoTime());
        entry.setDate(LocalDate.of(2026, 11, 21));
        entry.setType(type);
        entry.setCategory("测试");
        entry.setAmount(amount);
        return entry;
    }

    @Test
    public void emptyResultIsAllZero() {
        QueryResult result = new QueryResult(Collections.emptyList());
        assertEquals(0.0, result.getTotalIncome(), 0.000001);
        assertEquals(0.0, result.getTotalExpense(), 0.000001);
        assertEquals(0.0, result.getBalance(), 0.000001);
        assertEquals(0, result.getCount());
    }

    @Test
    public void incomeOnlyBalancesPositive() {
        QueryResult result = new QueryResult(List.of(
                entry(EntryType.INCOME, 3000),
                entry(EntryType.INCOME, 500)));
        assertEquals(3500.0, result.getTotalIncome(), 0.000001);
        assertEquals(0.0, result.getTotalExpense(), 0.000001);
        assertEquals(3500.0, result.getBalance(), 0.000001);
        assertEquals(2, result.getCount());
    }

    @Test
    public void expenseOnlyBalancesNegative() {
        QueryResult result = new QueryResult(List.of(
                entry(EntryType.EXPENSE, 12.5),
                entry(EntryType.EXPENSE, 7.5)));
        assertEquals(20.0, result.getTotalExpense(), 0.000001);
        assertEquals(-20.0, result.getBalance(), 0.000001);
    }

    @Test
    public void mixedBalancesToDifference() {
        QueryResult result = new QueryResult(List.of(
                entry(EntryType.INCOME, 8000),
                entry(EntryType.EXPENSE, 37.5)));
        assertEquals(8000.0, result.getTotalIncome(), 0.000001);
        assertEquals(37.5, result.getTotalExpense(), 0.000001);
        assertEquals(7962.5, result.getBalance(), 0.000001);
    }

    @Test
    public void decimalSumDoesNotDrift() {
        QueryResult result = new QueryResult(List.of(
                entry(EntryType.EXPENSE, 0.1),
                entry(EntryType.EXPENSE, 0.2)));
        // 不做规整会是 0.30000000000000004
        assertEquals(0.3, result.getTotalExpense(), 0.000001);
        assertEquals(-0.3, result.getBalance(), 0.000001);
    }

    @Test
    public void entriesAreCopiedSoOutsideChangeDoesNotLeak() {
        List<Entry> source = new ArrayList<>();
        source.add(entry(EntryType.INCOME, 100));
        QueryResult result = new QueryResult(source);
        source.add(entry(EntryType.INCOME, 999));
        assertEquals(1, result.getCount());
        assertEquals(100.0, result.getTotalIncome(), 0.000001);
    }

    @Test
    public void entriesListIsReadOnly() {
        QueryResult result = new QueryResult(List.of(entry(EntryType.INCOME, 1)));
        assertThrows(UnsupportedOperationException.class,
                () -> result.getEntries().add(entry(EntryType.INCOME, 2)));
    }
}
