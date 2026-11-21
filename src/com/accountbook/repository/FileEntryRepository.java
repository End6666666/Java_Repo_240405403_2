package com.accountbook.repository;

import com.accountbook.domain.Entry;
import com.accountbook.domain.EntryType;
import com.accountbook.exception.StorageException;
import com.accountbook.util.LogUtil;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * CSV 文本文件实现。
 *
 * <p>文件格式：一行一条记录，字段顺序固定为
 * {@code id,date,type,category,amount,note}，不写表头。</p>
 *
 * <p>转义规则：字段里出现逗号、双引号或换行时，整个字段用双引号包起来，
 * 字段内部的双引号写成两个。因此<b>不能用 {@code split(",")} 解析</b>，
 * 否则备注里写个逗号就会把整行拆错。</p>
 *
 * <p>写入采用“先写临时文件再原子替换”，避免写到一半崩溃把原账本毁掉。</p>
 */
public class FileEntryRepository implements EntryRepository {

    /** 日期格式，与 CSV 中第 2 个字段一致 */
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 每行应有的字段数：id,date,type,category,amount,note */
    private static final int FIELD_COUNT = 6;

    private final Path file;

    public FileEntryRepository(String filePath) {
        this(Paths.get(filePath));
    }

    public FileEntryRepository(Path file) {
        this.file = Objects.requireNonNull(file, "账本文件路径不能为空");
    }

    public Path getFile() {
        return file;
    }

    /**
     * 读取全部记录。
     *
     * <p>“文件不存在”与“文件存在但读不了”是两种性质不同的事：
     * 前者是首次使用，返回空列表；后者是真故障，必须抛异常。
     * 如果这里把读失败也当空列表返回，上层接着一保存，原账本就没了。</p>
     */
    @Override
    public List<Entry> load() throws StorageException {
        if (!Files.exists(file)) {
            LogUtil.info("账本文件不存在，按首次使用处理：" + file.toAbsolutePath());
            return new ArrayList<>();
        }

        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (NoSuchFileException e) {
            // exists() 与 readAllLines() 之间文件可能被删掉，这里兜底
            LogUtil.info("账本文件在读取前消失，按首次使用处理：" + file.toAbsolutePath());
            return new ArrayList<>();
        } catch (IOException e) {
            LogUtil.error("读取账本文件失败：" + file.toAbsolutePath(), e);
            throw new StorageException("读取账本文件失败：" + file.toAbsolutePath(), e);
        }

        List<Entry> result = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.trim().isEmpty()) {
                continue;
            }
            try {
                result.add(parseLine(line));
            } catch (RuntimeException e) {
                LogUtil.error("账本文件第 " + (i + 1) + " 行无法解析：" + line, e);
                throw new StorageException(
                        "账本文件第 " + (i + 1) + " 行损坏：" + e.getMessage() + "（" + file.toAbsolutePath() + "）", e);
            }
        }
        LogUtil.info("已从 " + file.toAbsolutePath() + " 加载 " + result.size() + " 条记录");
        return result;
    }

    @Override
    public void save(List<Entry> entries) throws StorageException {
        List<Entry> safe = (entries == null) ? new ArrayList<>() : entries;
        Path dir = file.toAbsolutePath().getParent();
        Path temp = null;
        try {
            if (dir != null) {
                Files.createDirectories(dir);
            }
            temp = Files.createTempFile(dir, "entries-", ".tmp");
            try (BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                for (Entry entry : safe) {
                    writer.write(toCsvLine(entry));
                    writer.newLine();
                }
            }
            moveIntoPlace(temp);
            temp = null;
            LogUtil.info("已保存 " + safe.size() + " 条记录到 " + file.toAbsolutePath());
        } catch (IOException e) {
            LogUtil.error("保存账本文件失败：" + file.toAbsolutePath(), e);
            throw new StorageException("保存账本文件失败：" + file.toAbsolutePath(), e);
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException e) {
                    // 临时文件删不掉不影响主流程，记一笔即可
                    LogUtil.warn("临时文件清理失败：" + temp.toAbsolutePath() + " - " + e.getMessage());
                }
            }
        }
    }

    /** 优先原子替换；文件系统不支持时退化为普通覆盖。 */
    private void moveIntoPlace(Path temp) throws IOException {
        try {
            Files.move(temp, file,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** 一行文本 → Entry。格式不对抛运行时异常，由调用方包成 StorageException。 */
    private Entry parseLine(String line) {
        List<String> fields = splitCsv(line);
        if (fields.size() != FIELD_COUNT) {
            throw new IllegalArgumentException(
                    "字段数应为 " + FIELD_COUNT + "，实际 " + fields.size());
        }

        String id = fields.get(0).trim();
        if (id.isEmpty()) {
            throw new IllegalArgumentException("id 为空");
        }

        LocalDate date;
        try {
            date = LocalDate.parse(fields.get(1), DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("日期格式应为 yyyy-MM-dd，实际为 '" + fields.get(1) + "'", e);
        }

        EntryType type;
        try {
            type = EntryType.valueOf(fields.get(2).trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("未知的收支类型 '" + fields.get(2) + "'", e);
        }

        double amount;
        try {
            amount = Double.parseDouble(fields.get(4));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("金额不是数字：'" + fields.get(4) + "'", e);
        }

        Entry entry = new Entry();
        entry.setId(id);
        entry.setDate(date);
        entry.setType(type);
        entry.setCategory(fields.get(3));
        entry.setAmount(amount);
        entry.setNote(fields.get(5));
        return entry;
    }

    /** Entry → 一行 CSV。 */
    private String toCsvLine(Entry entry) {
        StringBuilder sb = new StringBuilder();
        sb.append(escape(entry.getId())).append(',');
        sb.append(entry.getDate() == null ? "" : entry.getDate().format(DATE_FORMAT)).append(',');
        sb.append(entry.getType() == null ? "" : entry.getType().name()).append(',');
        sb.append(escape(entry.getCategory())).append(',');
        sb.append(formatAmount(entry.getAmount())).append(',');
        sb.append(escape(entry.getNote()));
        return sb.toString();
    }

    /**
     * 金额格式化为普通十进制字符串。
     * 不能直接用 {@code String.valueOf(double)}：金额达到 1e7 时会写成 1.0E7，
     * 别的程序（包括 Excel）读不懂。
     */
    private String formatAmount(double amount) {
        return BigDecimal.valueOf(amount).toPlainString();
    }

    /** 需要时给字段加双引号，内部双引号翻倍。 */
    private String escape(String value) {
        if (value == null) {
            return "";
        }
        boolean needQuote = value.indexOf(',') >= 0
                || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0;
        String escaped = value.replace("\"", "\"\"");
        return needQuote ? '"' + escaped + '"' : escaped;
    }

    /**
     * 按 CSV 规则拆字段：支持双引号包裹、引号内逗号、双引号转义（""）。
     * 手写状态机而不是正则，是因为正则版本在“引号内换行”和连续转义上很容易出错。
     */
    static List<String> splitCsv(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (inQuotes) {
            throw new IllegalArgumentException("双引号未闭合");
        }
        fields.add(current.toString());
        return fields;
    }
}
