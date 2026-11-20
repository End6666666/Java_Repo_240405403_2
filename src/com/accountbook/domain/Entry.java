package com.accountbook.domain;

import com.accountbook.exception.ValidationException;

import java.time.LocalDate;

/**
 * 一条收支记录：轻量富领域对象。
 *
 * <p>它只负责“一条记录自身是否合法”（{@link #validate()}），
 * 跨记录规则（id 是否存在、是否重复）由 Service 负责。</p>
 *
 * <p>注意：{@link ValidationException} 继承自
 * {@link com.accountbook.exception.BusinessException}，
 * 调用方捕获异常时必须先捕获 {@code ValidationException}。</p>
 */
public class Entry {

    /** 类别最大长度 */
    public static final int MAX_CATEGORY_LENGTH = 20;

    /** 备注最大长度 */
    public static final int MAX_NOTE_LENGTH = 100;

    private String id;
    private LocalDate date;
    private EntryType type;
    private String category;
    private double amount;
    private String note;

    public Entry() {
    }

    public Entry(String id, LocalDate date, EntryType type,
                 String category, double amount, String note) {
        this.id = id;
        this.date = date;
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.note = note;
    }

    /**
     * 基础字段校验，不合法即抛 {@link ValidationException}。
     * 金额校验顺序刻意先判 NaN/Infinity，再判 &lt;= 0。
     */
    public void validate() throws ValidationException {
        if (date == null) {
            throw new ValidationException("日期不能为空");
        }
        if (type == null) {
            throw new ValidationException("请选择收支类型");
        }
        if (category == null || category.trim().isEmpty()) {
            throw new ValidationException("类别不能为空");
        }
        if (category.trim().length() > MAX_CATEGORY_LENGTH) {
            throw new ValidationException("类别不能超过 " + MAX_CATEGORY_LENGTH + " 个字符");
        }
        if (Double.isNaN(amount) || Double.isInfinite(amount)) {
            throw new ValidationException("金额不是有效数字");
        }
        if (amount <= 0) {
            throw new ValidationException("金额必须大于 0");
        }
        if (note != null && note.length() > MAX_NOTE_LENGTH) {
            throw new ValidationException("备注不能超过 " + MAX_NOTE_LENGTH + " 个字符");
        }
    }

    /**
     * 金额保留两位小数（四舍五入到分）。
     * 金额用 double 存储，求和后必须规整，否则会出现 0.30000000000000004。
     */
    public static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public EntryType getType() {
        return type;
    }

    public void setType(EntryType type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    @Override
    public String toString() {
        return "Entry{id='" + id + "', date=" + date + ", type=" + type
                + ", category='" + category + "', amount=" + amount
                + ", note='" + note + "'}";
    }
}
