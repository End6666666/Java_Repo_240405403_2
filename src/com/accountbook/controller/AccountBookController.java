package com.accountbook.controller;

import com.accountbook.domain.Entry;
import com.accountbook.domain.QueryResult;
import com.accountbook.exception.BusinessException;
import com.accountbook.exception.StorageException;
import com.accountbook.exception.ValidationException;
import com.accountbook.service.AccountBookService;
import com.accountbook.util.LogUtil;
import com.accountbook.view.MainFrame;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 事件协调者。
 *
 * <p>职责：绑定界面事件、把界面上的文本组装成 {@link Entry}、调用 Service、
 * 捕获三类异常并转成可读提示、刷新界面。</p>
 *
 * <p>注意 catch 顺序：ValidationException 继承 BusinessException，
 * 必须写在前面，否则输入校验错误会被当成业务错误提示。</p>
 *
 * <p>不写文件、不做业务校验（金额是否为正、类别长度等交给 Entry.validate）。</p>
 */
public class AccountBookController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final MainFrame view;
    private final AccountBookService service;

    public AccountBookController(MainFrame view, AccountBookService service) {
        this.view = view;
        this.service = service;
        bindEvents();
        refreshTable();
    }

    private void bindEvents() {
        view.getAddButton().addActionListener(e -> onAdd());
        view.getUpdateButton().addActionListener(e -> onUpdate());
        view.getDeleteButton().addActionListener(e -> onDelete());
        view.getClearButton().addActionListener(e -> view.clearForm());
        view.getSearchButton().addActionListener(e -> onSearch());
        view.getResetButton().addActionListener(e -> onReset());
    }

    private void onAdd() {
        execute(() -> {
            Entry entry = readEntryFromForm();
            // 新增一律由 Service 生成编号，界面上的编号字段忽略
            entry.setId("");
            service.add(entry);
            view.clearForm();
            view.showInfo("新增成功，编号 " + entry.getId());
        }, true);
    }

    private void onUpdate() {
        execute(() -> {
            String id = view.getIdText().trim();
            if (id.isEmpty()) {
                throw new ValidationException("请先在表格里选中要修改的记录");
            }
            Entry entry = readEntryFromForm();
            entry.setId(id);
            service.update(entry);
            view.clearForm();
            view.showInfo("修改成功：" + id);
        }, true);
    }

    private void onDelete() {
        execute(() -> {
            String id = view.getSelectedEntryId();
            if (id == null || id.trim().isEmpty()) {
                throw new ValidationException("请先在表格里选中要删除的记录");
            }
            service.delete(id);
            view.clearForm();
            view.showInfo("已删除：" + id);
        }, true);
    }

    private void onSearch() {
        execute(() -> {
            QueryResult result = service.search(view.getKeywordText(), view.getSearchTypeOrNull());
            view.setTableData(result.getEntries());
            view.setSummary(result);
            if (result.getCount() == 0) {
                view.showInfo("没有符合条件的记录");
            } else {
                view.showInfo("查询到 " + result.getCount() + " 条记录");
            }
        }, false);
    }

    private void onReset() {
        execute(() -> {
            view.clearForm();
            refreshTable();
            view.showInfo("已显示全部记录");
        }, false);
    }

    /** 统一执行与异常处理，禁止空 catch */
    private void execute(Runnable action, boolean refreshAfter) {
        try {
            action.run();
            if (refreshAfter) {
                refreshTable();
            }
        } catch (ValidationException ex) {
            LogUtil.warn("输入校验失败：" + ex.getMessage());
            view.showInputError(ex.getMessage());
        } catch (BusinessException ex) {
            LogUtil.warn("业务规则拦截：" + ex.getMessage());
            view.showWarning(ex.getMessage());
        } catch (StorageException ex) {
            LogUtil.error("数据存取失败：" + ex.getMessage());
            view.showError("数据保存或读取失败：" + ex.getMessage());
        } catch (RuntimeException ex) {
            LogUtil.error("未预期的系统异常：" + ex);
            view.showError("系统异常：" + ex.getMessage());
        }
    }

    /** 把界面上的文本组装成 Entry；格式转换失败抛 ValidationException */
    private Entry readEntryFromForm() throws ValidationException {
        Entry entry = new Entry();
        entry.setId(view.getIdText().trim());

        String dateText = view.getDateText().trim();
        if (dateText.isEmpty()) {
            throw new ValidationException("日期不能为空，格式为 yyyy-MM-dd");
        }
        try {
            entry.setDate(LocalDate.parse(dateText, DATE_FORMAT));
        } catch (DateTimeParseException e) {
            throw new ValidationException("日期格式不正确，应为 yyyy-MM-dd：" + dateText);
        }

        entry.setType(view.getSelectedType());
        entry.setCategory(view.getCategoryText().trim());

        String amountText = view.getAmountText().trim();
        if (amountText.isEmpty()) {
            throw new ValidationException("金额不能为空");
        }
        try {
            entry.setAmount(Double.parseDouble(amountText));
        } catch (NumberFormatException e) {
            throw new ValidationException("金额必须是数字：" + amountText);
        }

        entry.setNote(view.getNoteText().trim());
        return entry;
    }

    /** 重新从 Service 取全部记录并刷新表格与汇总 */
    private void refreshTable() {
        List<Entry> all = service.listAll();
        view.setTableData(all);
        view.setSummary(new QueryResult(all));
    }
}
