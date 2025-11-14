package sm64music;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class Sm64MusicExtractor extends JFrame {
    private RomReader rom;
    private final DefaultListModel<SequenceEntry> listModel = new DefaultListModel<>();
    private final JList<SequenceEntry> seqList = new JList<>(listModel);
    private final JLabel status = new JLabel("Ready");

    public Sm64MusicExtractor() {
        super("SM64 Music Extractor");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 500);

        JButton openBtn = new JButton("Open ROM");
        JButton exportBtn = new JButton("Export Selected to TXT");
        JButton exportAllBtn = new JButton("Export ALL Sequences…");
        JButton exportRangesBtn = new JButton("Export ALL Ranges…");

        openBtn.addActionListener(e -> openRom());
        exportBtn.addActionListener(e -> exportSelected());
        exportAllBtn.addActionListener(e -> exportAllSequences());
        exportRangesBtn.addActionListener(e -> exportAllRanges());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(openBtn);
        top.add(exportBtn);
        top.add(exportAllBtn);
        top.add(exportRangesBtn);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(seqList), BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);

        // Double-click viewer
        seqList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    SequenceEntry sel = seqList.getSelectedValue();
                    if (sel != null) viewSequence(sel);
                }
            }
        });

        // Right-click menu for ranges
        JPopupMenu popup = new JPopupMenu();
        JMenuItem copyRangeItem = new JMenuItem("Copy Range(s) to Clipboard");
        JMenuItem saveRangeItem = new JMenuItem("Save Range(s) to TXT");
        popup.add(copyRangeItem);
        popup.add(saveRangeItem);
        seqList.setComponentPopupMenu(popup);

        copyRangeItem.addActionListener(e -> copySelectedRanges());
        saveRangeItem.addActionListener(e -> saveSelectedRanges());
    }

    private void openRom() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("N64 ROMs", "z64", "n64", "v64"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File romFile = chooser.getSelectedFile();
            try {
                rom = new RomReader(romFile);

                if (!rom.looksLikeSm64()) {
                    JOptionPane.showMessageDialog(this,
                            "This does not look like SM64 (title not found).",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    rom = null;
                    listModel.clear();
                    status.setText("No ROM loaded");
                    return;
                }

                List<SequenceEntry> entries = rom.parseSequences();
                listModel.clear();
                if (entries.isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                            "No sequences found. (Sequence bank header not recognized.)",
                            "Warning", JOptionPane.WARNING_MESSAGE);
                    status.setText("0 sequences");
                } else {
                    for (SequenceEntry e : entries) listModel.addElement(e);
                    status.setText(entries.size() + " sequences loaded");
                }
            } catch (Exception ex) {
                rom = null; listModel.clear();
                JOptionPane.showMessageDialog(this, "Failed to read ROM: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                status.setText("Load error");
            }
        }
    }

    private void exportSelected() {
        if (rom == null) return;
        SequenceEntry sel = seqList.getSelectedValue();
        if (sel == null) return;

        JFileChooser saveChooser = new JFileChooser();
        saveChooser.setDialogTitle("Save Sequence As");
        saveChooser.setSelectedFile(new File(
            String.format("seq_%02X_0x%06X.txt", sel.id, sel.seqOffset)
        ));

        if (saveChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File outFile = saveChooser.getSelectedFile();
            try (FileWriter fw = new FileWriter(outFile)) {
                byte[] data = rom.getBytes(sel.seqOffset, sel.length);
                for (int i = 0; i < data.length; i++) {
                    int off = sel.seqOffset + i;
                    fw.write(String.format("0x%06X: %02X%n", off, data[i] & 0xFF));
                }
                status.setText("Saved: " + outFile.getName());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "Failed export: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void viewSequence(SequenceEntry sel) {
        if (rom == null || sel == null) return;
        byte[] data = rom.getBytes(sel.seqOffset, sel.length);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            int off = sel.seqOffset + i;
            sb.append(String.format("0x%06X: %02X%n", off, data[i] & 0xFF));
        }
        JTextArea textArea = new JTextArea(sb.toString());
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        textArea.setEditable(false);
        textArea.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(textArea);

        JOptionPane.showMessageDialog(this, scroll,
                "Sequence " + sel.id + " (" + sel.length + " bytes)",
                JOptionPane.PLAIN_MESSAGE);
    }

    /** Export all sequences as byte dumps (old behavior). */
    private void exportAllSequences() {
        if (rom == null || listModel.isEmpty()) return;
        JFileChooser dirChooser = new JFileChooser();
        dirChooser.setDialogTitle("Choose folder to export all sequences");
        dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (dirChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File dir = dirChooser.getSelectedFile();
            int ok = 0;
            for (int i = 0; i < listModel.size(); i++) {
                SequenceEntry se = listModel.get(i);
                File out = new File(dir, String.format("seq_%02X_0x%06X.txt", se.id, se.seqOffset));
                try (FileWriter fw = new FileWriter(out)) {
                    byte[] data = rom.getBytes(se.seqOffset, se.length);
                    for (int j = 0; j < data.length; j++) {
                        int off = se.seqOffset + j;
                        fw.write(String.format("0x%06X: %02X%n", off, data[j] & 0xFF));
                    }
                    ok++;
                } catch (IOException ignore) { }
            }
            status.setText("Exported " + ok + " sequences to " + dir.getAbsolutePath());
        }
    }

    /** Export all sequences as range lines. */
    private void exportAllRanges() {
        if (rom == null || listModel.isEmpty()) return;
        JFileChooser saveChooser = new JFileChooser();
        saveChooser.setDialogTitle("Save All Ranges As");
        saveChooser.setSelectedFile(new File("all_ranges.txt"));
        if (saveChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File outFile = saveChooser.getSelectedFile();
            try (FileWriter fw = new FileWriter(outFile)) {
                for (int i = 0; i < listModel.size(); i++) {
                    SequenceEntry se = listModel.get(i);
                    int start = se.seqOffset;
                    int end = se.seqOffset + se.length;
                    fw.write(String.format("RANGE: %06X-%06X%n", start, end));
                }
                status.setText("Saved ranges to " + outFile.getName());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "Failed saving ranges: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /** Copy selected ranges to clipboard. */
    private void copySelectedRanges() {
        List<SequenceEntry> selected = seqList.getSelectedValuesList();
        if (selected.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        for (SequenceEntry sel : selected) {
            int start = sel.seqOffset;
            int end = sel.seqOffset + sel.length;
            sb.append(String.format("RANGE: %06X-%06X%n", start, end));
        }
        StringSelection selection = new StringSelection(sb.toString());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
        status.setText("Copied " + selected.size() + " range(s) to clipboard.");
    }

    /** Save selected ranges to TXT. */
    private void saveSelectedRanges() {
        List<SequenceEntry> selected = seqList.getSelectedValuesList();
        if (selected.isEmpty()) return;
        JFileChooser saveChooser = new JFileChooser();
        saveChooser.setDialogTitle("Save Selected Ranges As");
        saveChooser.setSelectedFile(new File("ranges.txt"));
        if (saveChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File outFile = saveChooser.getSelectedFile();
            try (FileWriter fw = new FileWriter(outFile)) {
                for (SequenceEntry sel : selected) {
                    int start = sel.seqOffset;
                    int end = sel.seqOffset + sel.length;
                    fw.write(String.format("RANGE: %06X-%06X%n", start, end));
                }
                status.setText("Saved " + selected.size() + " range(s) to " + outFile.getName());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "Failed saving: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Sm64MusicExtractor().setVisible(true));
    }
}
