package filemanager;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 *
 * @author Beatriz Conrat
 */

public class FileManager extends JFrame {

    private JList<File> fileList;
    private DefaultListModel<File> listModel;
    private JTextField pathField;
    private JTextArea fileDetailsArea;
    private File currentDirectory;
    private Timer refreshTimer;
    private boolean mouseActivity;

    public FileManager() {
        setTitle("File Manager");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        currentDirectory = new File(System.getProperty("user.dir"));

        listModel = new DefaultListModel<>();
        fileList = new JList<>(listModel);
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileList.setCellRenderer(new FileCellRenderer());
        fileList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    File selectedFile = fileList.getSelectedValue();
                    if (selectedFile != null && selectedFile.isDirectory()) {
                        currentDirectory = selectedFile;
                        refreshFileList();
                    }
                }
                mouseActivityDetected();
            }
        });
        fileList.addListSelectionListener(new FileSelectionListener());

        JScrollPane scrollPane = new JScrollPane(fileList);
        scrollPane.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mouseActivityDetected();
            }
        });

        pathField = new JTextField(currentDirectory.getAbsolutePath());
        pathField.setEditable(false);

        fileDetailsArea = new JTextArea();
        fileDetailsArea.setEditable(false);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshFileList());

        JButton createButton = new JButton("Create");
        createButton.addActionListener(e -> createFile());

        JButton renameButton = new JButton("Rename");
        renameButton.addActionListener(e -> renameFile());

        JButton deleteButton = new JButton("Delete");
        deleteButton.addActionListener(e -> deleteFile());

        JButton copyButton = new JButton("Copy");
        copyButton.addActionListener(e -> copyFile());

        JButton moveButton = new JButton("Move");
        moveButton.addActionListener(e -> moveFile());

        JButton navigateButton = new JButton("Navigate");
        navigateButton.addActionListener(e -> navigateToDirectory());

        JButton parentButton = new JButton("Up");
        parentButton.addActionListener(e -> navigateToParentDirectory());

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new GridLayout(1, 8));
        controlPanel.add(refreshButton);
        controlPanel.add(createButton);
        controlPanel.add(renameButton);
        controlPanel.add(deleteButton);
        controlPanel.add(copyButton);
        controlPanel.add(moveButton);
        controlPanel.add(navigateButton);
        controlPanel.add(parentButton);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane, new JScrollPane(fileDetailsArea));
        splitPane.setDividerLocation(400);

        add(pathField, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        refreshFileList();

        // Set up the Timer to refresh the file list every 5 seconds if there's no mouse activity
        refreshTimer = new Timer(5000, e -> {
            if (!mouseActivity) {
                refreshFileList();
            }
        });
        refreshTimer.start();

        // Timer to reset mouse activity
        new Timer(2000, e -> mouseActivity = false).start();
    }

    private void mouseActivityDetected() {
        mouseActivity = true;
    }

    private void refreshFileList() {
        listModel.clear();
        File[] files = currentDirectory.listFiles();
        if (files != null) {
            for (File file : files) {
                listModel.addElement(file);
            }
        }
        pathField.setText(currentDirectory.getAbsolutePath());
        fileDetailsArea.setText("");
    }

    private void createFile() {
        String fileName = JOptionPane.showInputDialog(this, "Enter file/directory name:");
        if (fileName != null && !fileName.trim().isEmpty()) {
            File newFile = new File(currentDirectory, fileName);
            try {
                if (newFile.mkdir()) {
                    refreshFileList();
                    JOptionPane.showMessageDialog(this, "Directory created successfully.");
                } else if (newFile.createNewFile()) {
                    refreshFileList();
                    JOptionPane.showMessageDialog(this, "File created successfully.");
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to create file/directory.");
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            }
        }
    }

    private void renameFile() {
        File selectedFile = fileList.getSelectedValue();
        if (selectedFile != null) {
            String newFileName = JOptionPane.showInputDialog(this, "Enter new name:", selectedFile.getName());
            if (newFileName != null && !newFileName.trim().isEmpty()) {
                File newFile = new File(currentDirectory, newFileName);
                if (selectedFile.renameTo(newFile)) {
                    refreshFileList();
                    JOptionPane.showMessageDialog(this, "File renamed successfully.");
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to rename file.");
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "No file selected.");
        }
    }

    private void deleteFile() {
        File selectedFile = fileList.getSelectedValue();
        if (selectedFile != null) {
            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this file?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                if (selectedFile.isDirectory()) {
                    deleteDirectory(selectedFile);
                } else if (selectedFile.delete()) {
                    refreshFileList();
                    JOptionPane.showMessageDialog(this, "File deleted successfully.");
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to delete file.");
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "No file selected.");
        }
    }

    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
        refreshFileList();
        JOptionPane.showMessageDialog(this, "Directory deleted successfully.");
    }

    private void copyFile() {
        File selectedFile = fileList.getSelectedValue();
        if (selectedFile != null) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select destination directory");
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int option = fileChooser.showDialog(this, "Select");
            if (option == JFileChooser.APPROVE_OPTION) {
                File destinationDirectory = fileChooser.getSelectedFile();
                Path sourcePath = selectedFile.toPath();
                Path destinationPath = destinationDirectory.toPath().resolve(selectedFile.getName());
                try {
                    if (selectedFile.isDirectory()) {
                        copyDirectory(sourcePath, destinationPath);
                    } else {
                        Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                    JOptionPane.showMessageDialog(this, "File copied successfully.");
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this, "Error copying file: " + e.getMessage());
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "No file selected.");
        }
    }

    private void copyDirectory(Path source, Path destination) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetPath = destination.resolve(source.relativize(dir));
                if (!Files.exists(targetPath)) {
                    Files.createDirectory(targetPath);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, destination.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void moveFile() {
        File selectedFile = fileList.getSelectedValue();
        if (selectedFile != null) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select destination directory");
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int option = fileChooser.showDialog(this, "Select");
            if (option == JFileChooser.APPROVE_OPTION) {
                File destinationDirectory = fileChooser.getSelectedFile();
                Path sourcePath = selectedFile.toPath();
                Path destinationPath = destinationDirectory.toPath().resolve(selectedFile.getName());
                try {
                    if (selectedFile.isDirectory()) {
                        moveDirectory(sourcePath, destinationPath);
                    } else {
                        Files.move(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                    refreshFileList();
                    JOptionPane.showMessageDialog(this, "File moved successfully.");
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this, "Error moving file: " + e.getMessage());
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "No file selected.");
        }
    }

    private void moveDirectory(Path source, Path destination) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetPath = destination.resolve(source.relativize(dir));
                if (!Files.exists(targetPath)) {
                    Files.createDirectory(targetPath);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.move(file, destination.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void navigateToDirectory() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Navigate to Directory");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        int option = fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (selectedFile.isDirectory()) {
                currentDirectory = selectedFile;
                refreshFileList();
            } else {
                JOptionPane.showMessageDialog(this, "Selected file is not a directory.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void navigateToParentDirectory() {
        if (currentDirectory.getParentFile() != null) {
            currentDirectory = currentDirectory.getParentFile();
            refreshFileList();
        }
    }

    private class FileCellRenderer extends DefaultListCellRenderer {
        private FileSystemView fileSystemView = FileSystemView.getFileSystemView();

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof File) {
                File file = (File) value;
                setIcon(fileSystemView.getSystemIcon(file));
                setText(fileSystemView.getSystemDisplayName(file));
                setToolTipText(file.getPath());
            }
            return this;
        }
    }

    private class FileSelectionListener implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent e) {
            File selectedFile = fileList.getSelectedValue();
            if (selectedFile != null) {
                fileDetailsArea.setText(getFileDetails(selectedFile));
            }
        }

        private String getFileDetails(File file) {
            StringBuilder details = new StringBuilder();
            details.append("Name: ").append(file.getName()).append("\n");
            details.append("Path: ").append(file.getPath()).append("\n");
            details.append("Size: ").append(file.length()).append(" bytes\n");
            details.append("Last Modified: ").append(file.lastModified()).append("\n");
            return details.toString();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FileManager().setVisible(true));
    }
}
