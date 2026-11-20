package com.accountbook.domain;

/**
 * 收支类型。
 *
 * <p>持久化时使用 {@link #name()}（INCOME / EXPENSE），
 * 界面展示时使用 {@link #getLabel()}（收入 / 支出）。</p>
 */
public enum EntryType {

    INCOME("收入"),
    EXPENSE("支出");

    private final String label;

    EntryType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** 便于 JComboBox 直接显示中文。 */
    @Override
    public String toString() {
        return label;
    }
}
