import java.awt.*;
import java.awt.event.KeyEvent;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileSystemView;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ExecutionException;

public class TextEditor extends JFrame {

    private JPanel filePanel;
    private JPanel textPanel;
    private JScrollPane scrollPane;
    private JTextField searchField;
    private JTextArea textArea;
    private JButton openButton;
    private JButton saveButton;
    private JButton searchButton;
    private JButton prevMatchButton;
    private JButton nextMatchButton;
    private JCheckBox useRegEx;
    private JFileChooser jfc;
    private File currentFile;
    private int index;
    private boolean textChanged;
    private static final String TITLE = "Notepad";
    private Map<Integer, Integer> searchResults = new LinkedHashMap<>();
    private Integer[] searchResultsKeys;

    public TextEditor() {

        setTitle(TITLE);
        setSize(750, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (textChanged) {
                    int action = saveAlert();
                    if (action == JOptionPane.YES_OPTION || action == JOptionPane.NO_OPTION) {
                        System.exit(0);
                    }
                } else {
                    System.exit(0);
                }
            }
        });

        filePanel = new JPanel();
        filePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        setMargin(filePanel, 0, 0, 0, 15);

        textPanel = new JPanel();
        textPanel.setLayout(new BorderLayout());
        setMargin(textPanel, 5, 20, 20, 20);

        searchField = new JTextField();
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                searchResults.clear();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
        searchField.setPreferredSize(new Dimension(250, 33));

        jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Text Files (*.txt)", "txt");
        jfc.addChoosableFileFilter(filter);
        jfc.setAcceptAllFileFilterUsed(true);

        textArea = new JTextArea();
        textArea.getDocument().addDocumentListener(new ChangeListener());
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        scrollPane = new JScrollPane(textArea);

        useRegEx = new JCheckBox("Use regex");

        setButtons();
        setListeners();
        setMenu();

        filePanel.add(openButton);
        filePanel.add(saveButton);
        filePanel.add(searchField);
        filePanel.add(searchButton);
        filePanel.add(prevMatchButton);
        filePanel.add(nextMatchButton);
        filePanel.add(useRegEx);

        textPanel.add(scrollPane);

        add(filePanel, BorderLayout.NORTH);
        add(textPanel, BorderLayout.CENTER);

        setVisible(true);
    }

    private void setMenu() {

        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        JMenu searchMenu = new JMenu("Search");
        searchMenu.setMnemonic(KeyEvent.VK_S);

        JMenuItem openMenuItem = new JMenuItem("Open");
        openMenuItem.addActionListener(openButton.getActionListeners()[0]);

        JMenuItem saveMenuItem = new JMenuItem("Save");
        saveMenuItem.addActionListener(saveButton.getActionListeners()[0]);

        JMenuItem saveAsMenuItem = new JMenuItem("Save As");
        saveAsMenuItem.addActionListener(event -> {
            jfc.setSelectedFile(Objects.requireNonNullElseGet(currentFile, () -> new File("*.txt")));
            if (jfc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                currentFile = jfc.getSelectedFile();
                save();
            }
        });

        JMenuItem closeMenuItem = new JMenuItem("Close");
        closeMenuItem.addActionListener(event -> {
            if (textChanged) {
                int action = saveAlert();
                if (action == JOptionPane.CANCEL_OPTION || action == JOptionPane.CLOSED_OPTION) return;
            }
            textArea.setText("");
            textChanged = false;
            currentFile = null;
            setTitle(TITLE);
        });

        JMenuItem exitMenuItem = new JMenuItem("Exit");
        exitMenuItem.addActionListener(event -> System.exit(0));

        JMenuItem searchMenuItem = new JMenuItem("Start search");
        searchMenuItem.addActionListener(searchButton.getActionListeners()[0]);

        JMenuItem prevMatchMenuItem = new JMenuItem("Previous match");
        prevMatchMenuItem.addActionListener(prevMatchButton.getActionListeners()[0]);

        JMenuItem nextMatchMenuItem = new JMenuItem("Next match");
        nextMatchMenuItem.addActionListener(nextMatchButton.getActionListeners()[0]);

        JMenuItem regExMenuItem = new JMenuItem("Use regular expressions");
        regExMenuItem.addActionListener(event -> useRegEx.doClick());

        fileMenu.add(openMenuItem);
        fileMenu.add(saveMenuItem);
        fileMenu.add(saveAsMenuItem);
        fileMenu.add(closeMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(exitMenuItem);

        searchMenu.add(searchMenuItem);
        searchMenu.add(prevMatchMenuItem);
        searchMenu.add(nextMatchMenuItem);
        searchMenu.add(regExMenuItem);

        menuBar.add(fileMenu);
        menuBar.add(searchMenu);

        setJMenuBar(menuBar);
    }

    private void setButtons() {

        Dimension buttonSize = new Dimension(32, 32);

        openButton = new JButton(new ImageIcon("icons/openIcon.png"));
        openButton.setPreferredSize(buttonSize);

        saveButton = new JButton(new ImageIcon("icons/saveIcon.png"));
        saveButton.setPreferredSize(buttonSize);

        searchButton = new JButton(new ImageIcon("icons/searchIcon.png"));
        searchButton.setPreferredSize(buttonSize);

        prevMatchButton = new JButton(new ImageIcon("icons/previousMatchIcon.png"));
        prevMatchButton.setPreferredSize(buttonSize);

        nextMatchButton = new JButton(new ImageIcon("icons/nextMatchIcon.png"));
        nextMatchButton.setPreferredSize(buttonSize);
    }

    private void setListeners() {

        openButton.addActionListener(event -> open());

        saveButton.addActionListener(event -> {
            if (currentFile == null) {
                jfc.setSelectedFile(new File("*.txt"));
                if (jfc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                    currentFile = jfc.getSelectedFile();
                    save();
                }
            } else {
                save();
            }
        });

        searchButton.addActionListener(event -> search());

        prevMatchButton.addActionListener(event -> prevMatch());

        nextMatchButton.addActionListener(event -> nextMatch());
    }

    private void open() {
        if (textChanged) {
            int action = saveAlert();
            if (action == JOptionPane.CANCEL_OPTION || action == JOptionPane.CLOSED_OPTION) return;
        }
        try {
            jfc.setSelectedFile(new File(""));
            if (jfc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                currentFile = jfc.getSelectedFile();
                if (currentFile.exists()) {
                    FileReader reader = new FileReader(currentFile);
                    textArea.read(reader, null);
                    textArea.getDocument().addDocumentListener(new ChangeListener());
                    setTitle(currentFile.getName() + " - " + TITLE);
                } else {
                    currentFile = null;
                    JOptionPane.showMessageDialog(null, "File not found.", "Opening error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (IOException ignored) {
        }
    }

    private void search() {
        searchResults.clear();
        index = 0;
        String pattern = searchField.getText().toLowerCase();
        int patternLength = pattern.length();
        String text = textArea.getText().toLowerCase();

        SwingWorker<Boolean, Object> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                int i = -1;
                if (!pattern.isEmpty()) {
                    if (useRegEx.isSelected()) {
                        Matcher matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(text);
                        while (matcher.find()) {
                            searchResults.put(matcher.start(), matcher.end());
                        }
                    } else {
                        while (true) {
                            i = text.indexOf(pattern, i + 1);
                            if (i == -1) {
                                break;
                            }
                            searchResults.put(i, i + patternLength);
                        }
                    }
                }
                searchResultsKeys = searchResults.keySet().toArray(new Integer[0]);
                return !searchResults.isEmpty();
            }

            @Override
            protected void done() {
                boolean found = false;
                try {
                    found = get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
                if (found) {
                    int start = index;
                    int end = searchResults.get(start);
                    textArea.setCaretPosition(start + (end - start));
                    textArea.select(start, start + (end - start));
                    textArea.grabFocus();
                }
            }
        };

        worker.execute();
    }

    private void prevMatch() {
        if (searchResultsKeys.length != 0) {
            int start = index > 0 ? searchResultsKeys[--index] : searchResultsKeys[index = searchResults.size() - 1];
            int end = searchResults.get(start);
            grabFocus(start, end);
        }
    }

    private void nextMatch() {
        if (searchResultsKeys.length != 0) {
            int start = index < searchResults.size() - 1 ? searchResultsKeys[++index] : searchResultsKeys[index = 0];
            int end = searchResults.get(start);
            grabFocus(start, end);
        }
    }

    private void grabFocus(int start, int end) {
        textArea.setCaretPosition(start + (end - start));
        textArea.select(start, start + (end - start));
        textArea.grabFocus();
    }

    private void save() {
        FileWriter writer;
        try {
            writer = new FileWriter(currentFile);
            textArea.write(writer);
        } catch (IOException ignored) {
        }
        textChanged = false;
        setTitle(currentFile.getName() + " - " + TITLE);
    }

    private int saveAlert() {
        int input = JOptionPane.showConfirmDialog(null, "Do you want to save changes?");
        if (input == JOptionPane.YES_OPTION) {
            saveButton.doClick();
        }
        return input;
    }

    private void setMargin(JComponent component, int top, int right, int bottom, int left) {
        Border border = component.getBorder();
        Border marginBorder = new EmptyBorder(new Insets(top, left, bottom, right));
        component.setBorder(border == null ? marginBorder : new CompoundBorder(marginBorder, border));
    }

    private class ChangeListener implements DocumentListener {

        @Override
        public void insertUpdate(DocumentEvent e) {
            setChanged();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            setChanged();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            setChanged();
        }

        private void setChanged() {
            textChanged = true;
            if (!getTitle().startsWith("*")) {
                setTitle("*" + getTitle());
            }
        }
    }
}
