package com.accountbook.view;

import com.accountbook.domain.Entry;
import com.accountbook.domain.EntryType;
import com.accountbook.domain.QueryResult;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 主窗口。
 *
 * <p>职责边界（作业要求）：只负责界面与输入收集，不读写文件、不直接调用
 * Repository、不做业务校验。所有按钮的点击行为由 Controller 绑定。</p>
 */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String[] COLUMNS = {"编号", "日期", "类型", "类别", "金额", "备注"};

    private final JTextField idField = new JTextField(12);
    private final JTextField dateField = new JTextField(12);
    private final JComboBox<EntryType> typeBox = new JComboBox<>(EntryType.values());
    private final JTextField categoryField = new JTextField(12);
    private final JTextField amountField = new JTextField(12);
    private final JTextField noteField = new JTextField(24);

    private final JButton addButton = new JButton("新增");
    private final JButton updateButton = new JButton("修改");
    private final JButton deleteButton = new JButton("删除");
    private final JButton clearButton = new JButton("清空");

    private final JTextField keywordField = new JTextField(14);
    private final JComboBox<String> searchTypeBox =
            new JComboBox<>(new String[]{"全部", EntryType.INCOME.getLabel(), EntryType.EXPENSE.getLabel()});
    private final JButton searchButton = new JButton("查询");
    private final JButton resetButton = new JButton("显示全部");

    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    private final JLabel summaryLabel = new JLabel();
    private final JLabel statusLabel = new JLabel(" ");

    public MainFrame() {
        setTitle("私人记账本");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 640);
        setLocationRelativeTo(null);
        buildUi();
        clearForm();
        updateSummary(0, 0, 0, 0);
    }

    private void buildUi() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("记账信息"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.anchor = GridBagConstraints.WEST;

        idField.setEditable(false);
        idField.setToolTipText("编号由系统自动生成，不需要手动填写");

        addField(form, c, 0, 0, "编号", idField);
        addField(form, c, 0, 2, "日期", dateField);
        addField(form, c, 1, 0, "类型", typeBox);
        addField(form, c, 1, 2, "类别", categoryField);
        addField(form, c, 2, 0, "金额", amountField);
        addField(form, c, 2, 2, "备注", noteField);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));

        buttonRow.add(addButton);
        buttonRow.add(updateButton);
        buttonRow.add(deleteButton);
        buttonRow.add(clearButton);

        JPanel top = new JPanel(new BorderLayout(0, 4));
        top.add(form, BorderLayout.CENTER);
        top.add(buttonRow, BorderLayout.SOUTH);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        searchPanel.setBorder(BorderFactory.createTitledBorder("查询"));
        searchPanel.add(new JLabel("关键字"));
        searchPanel.add(keywordField);
        searchPanel.add(new JLabel("类型"));
        searchPanel.add(searchTypeBox);
        searchPanel.add(searchButton);
        searchPanel.add(resetButton);

        JPanel summaryPanel = new JPanel(new BorderLayout(0, 2));
        summaryPanel.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        summaryPanel.add(summaryLabel, BorderLayout.NORTH);
        summaryPanel.add(statusLabel, BorderLayout.SOUTH);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(searchPanel, BorderLayout.NORTH);
        bottom.add(summaryPanel, BorderLayout.SOUTH);

        table.setRowHeight(24);
        table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(90);
        table.getColumnModel().getColumn(1).setPreferredWidth(90);
        table.getColumnModel().getColumn(2).setPreferredWidth(60);
        table.getColumnModel().getColumn(3).setPreferredWidth(120);
        table.getColumnModel().getColumn(4).setPreferredWidth(90);
        table.getColumnModel().getColumn(5).setPreferredWidth(260);

        // 选中表格某行时把这条记录填回表单，方便修改
        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            int row = table.getSelectedRow();
            if (row >= 0) {
                fillFormFromRow(row);
            }
        });

        setLayout(new BorderLayout(0, 6));
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    private static void addField(JPanel panel, GridBagConstraints c,
                                 int row, int col, String label, JComponent comp) {
        c.gridx = col;
        c.gridy = row;
        c.gridwidth = 1;
        c.weightx = 0;
        panel.add(new JLabel(label), c);
        c.gridx = col + 1;
        c.weightx = 1;
        panel.add(comp, c);
    }

    private void fillFormFromRow(int row) {
        // 表格开了排序，视图行号不等于模型行号，必须先转换
        int modelRow = table.convertRowIndexToModel(row);
        idField.setText(cellText(modelRow, 0));
        dateField.setText(cellText(modelRow, 1));
        categoryField.setText(cellText(modelRow, 3));
        amountField.setText(cellText(modelRow, 4));
        noteField.setText(cellText(modelRow, 5));

        String typeLabel = cellText(modelRow, 2);
        for (EntryType type : EntryType.values()) {
            if (type.getLabel().equals(typeLabel)) {
                typeBox.setSelectedItem(type);
                break;
            }
        }
    }

    private String cellText(int row, int column) {
        Object value = tableModel.getValueAt(row, column);
        return value == null ? "" : value.toString();
    }

    // ---------- 供 Controller 读取的输入 ----------

    public String getIdText() {
        return idField.getText();
    }

    public String getDateText() {
        return dateField.getText();
    }

    public EntryType getSelectedType() {
        return (EntryType) typeBox.getSelectedItem();
    }

    public String getCategoryText() {
        return categoryField.getText();
    }

    public String getAmountText() {
        return amountField.getText();
    }

    public String getNoteText() {
        return noteField.getText();
    }

    public String getKeywordText() {
        return keywordField.getText();
    }

    /** 返回 null 表示"全部类型" */
    public EntryType getSearchTypeOrNull() {
        int index = searchTypeBox.getSelectedIndex();
        return index <= 0 ? null : EntryType.values()[index - 1];
    }

    public String getSelectedEntryId() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return null;
        }
        return cellText(table.convertRowIndexToModel(row), 0);
    }

    public JTable getTable() {
        return table;
    }

    public JButton getAddButton() {
        return addButton;
    }

    public JButton getUpdateButton() {
        return updateButton;
    }

    public JButton getDeleteButton() {
        return deleteButton;
    }

    public JButton getClearButton() {
        return clearButton;
    }

    public JButton getSearchButton() {
        return searchButton;
    }

    public JButton getResetButton() {
        return resetButton;
    }

    // ---------- 供 Controller 刷新界面 ----------

    public void setTableData(List<Entry> entries) {
        tableModel.setRowCount(0);
        for (Entry entry : entries) {
            tableModel.addRow(new Object[]{
                    entry.getId(),
                    entry.getDate() == null ? "" : entry.getDate().format(DATE_FORMAT),
                    entry.getType() == null ? "" : entry.getType().getLabel(),
                    entry.getCategory(),
                    String.format("%.2f", entry.getAmount()),
                    entry.getNote()
            });
        }
    }

    public void setSummary(QueryResult result) {
        updateSummary(result.getTotalIncome(), result.getTotalExpense(),
                result.getBalance(), result.getCount());
    }

    private void updateSummary(double income, double expense, double balance, int count) {
        summaryLabel.setText(String.format("收入合计 %.2f    支出合计 %.2f    余额 %.2f    共 %d 条",
                income, expense, balance, count));
    }

    public void clearForm() {
        idField.setText("");
        dateField.setText(LocalDate.now().format(DATE_FORMAT));
        typeBox.setSelectedIndex(0);
        categoryField.setText("");
        amountField.setText("");
        noteField.setText("");
        table.clearSelection();
    }

    // ---------- 消息提示 ----------

    public void showInfo(String message) {
        statusLabel.setForeground(new Color(0, 128, 0));
        statusLabel.setText(message);
    }

    public void showInputError(String message) {
        statusLabel.setForeground(Color.RED);
        statusLabel.setText("输入有误：" + message);
        JOptionPane.showMessageDialog(this, message, "输入有误", JOptionPane.WARNING_MESSAGE);
    }

    public void showWarning(String message) {
        statusLabel.setForeground(new Color(180, 100, 0));
        statusLabel.setText(message);
        JOptionPane.showMessageDialog(this, message, "提示", JOptionPane.WARNING_MESSAGE);
    }

    public void showError(String message) {
        statusLabel.setForeground(Color.RED);
        statusLabel.setText(message);
        JOptionPane.showMessageDialog(this, message, "错误", JOptionPane.ERROR_MESSAGE);
    }
}
