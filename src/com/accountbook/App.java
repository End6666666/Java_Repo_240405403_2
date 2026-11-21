package com.accountbook;

import com.accountbook.controller.AccountBookController;
import com.accountbook.exception.StorageException;
import com.accountbook.repository.FileEntryRepository;
import com.accountbook.service.AccountBookService;
import com.accountbook.service.AccountBookServiceImpl;
import com.accountbook.util.LogUtil;
import com.accountbook.view.MainFrame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 程序入口：只做装配（创建 Repository → Service → View → Controller）。
 */
public final class App {

    private App() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(App::start);
    }

    private static void start() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            LogUtil.warn("系统外观不可用，改用默认外观：" + e.getMessage());
        }

        try {
            Path dataFile = Paths.get("data", "entries.csv");
            AccountBookService service = new AccountBookServiceImpl(new FileEntryRepository(dataFile));
            MainFrame frame = new MainFrame();
            new AccountBookController(frame, service);
            frame.setVisible(true);
            LogUtil.info("界面已启动，数据文件：" + dataFile.toAbsolutePath());
        } catch (StorageException e) {
            LogUtil.error("启动失败：" + e.getMessage());
            JOptionPane.showMessageDialog(null,
                    "账本文件无法读取，程序不能启动：\n" + e.getMessage(),
                    "启动失败", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
}
