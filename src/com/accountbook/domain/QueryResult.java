package com.accountbook.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 查询结果对象：命中明细 + 汇总。
 *
 * <p>余额、收入合计、支出合计都是<b>派生量</b>，由明细现算，
 * 不入库、不缓存在 Entry 里。</p>
 */
public class QueryResult {

    private List<Entry> entries = new ArrayList<>();
    private double totalIncome;
    private double totalExpense;
    private double balance;
    private int count;

    public QueryResult() {
    }

    public QueryResult(List<Entry> entries) {
        setEntries(entries);
    }

    /**
     * 设置明细并立即重算汇总。
     * 传入列表会被拷贝一份，避免外部改动影响结果对象。
     */
    public void setEntries(List<Entry> entries) {
        this.entries = (entries == null)
                ? new ArrayList<>()
                : new ArrayList<>(entries);
        recalculate();
    }

    /** 根据当前明细重算收入合计、支出合计、余额、条数。 */
    public void recalculate() {
        double income = 0.0;
        double expense = 0.0;
        for (Entry entry : entries) {
            if (entry.getType() == EntryType.INCOME) {
                income += entry.getAmount();
            } else if (entry.getType() == EntryType.EXPENSE) {
                expense += entry.getAmount();
            }
        }
        this.totalIncome = Entry.round2(income);
        this.totalExpense = Entry.round2(expense);
        this.balance = Entry.round2(income - expense);
        this.count = entries.size();
    }

    public List<Entry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public double getTotalIncome() {
        return totalIncome;
    }

    public double getTotalExpense() {
        return totalExpense;
    }

    public double getBalance() {
        return balance;
    }

    public int getCount() {
        return count;
    }

    @Override
    public String toString() {
        return "QueryResult{count=" + count + ", totalIncome=" + totalIncome
                + ", totalExpense=" + totalExpense + ", balance=" + balance + '}';
    }
}
