package sm64mio;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;

public class MIO0Extractor extends JFrame {

    private JTextArea log;

    public MIO0Extractor() {
        super("SM64 MIO0 → Uncompressed Extractor");

        JButton openButton = new JButton("Open ROM");
        log = new JTextArea(20, 60);
        log.setEditable(false);

        openButton.addActionListener(e -> chooseROM());

        add(openButton, BorderLayout.NORTH);
        add(new JScrollPane(log), BorderLayout.CENTER);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void chooseROM() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File rom = chooser.getSelectedFile();
            log.append("Opened ROM: " + rom.getName() + "\n");
            try {
                byte[] data = Files.readAllBytes(rom.toPath());
                extractMIO0(data, rom.getParentFile());
            } catch (IOException ex) {
                log.append("Error reading ROM: " + ex.getMessage() + "\n");
            }
        }
    }

    private void extractMIO0(byte[] rom, File outDir) {
        int scanStart, scanEnd;
        if (rom.length > 0x800000) {
            log.append("Extended ROM detected (" + rom.length + " bytes)\n");
            scanStart = 0x800000;
            scanEnd = rom.length;
            log.append("Scanning region: 0x"
                    + Integer.toHexString(scanStart).toUpperCase()
                    + " – 0x"
                    + Integer.toHexString(scanEnd).toUpperCase()
                    + "\n");
        } else {
            log.append("Normal ROM detected (8 MB)\n");
            scanStart = 0x000000;
            scanEnd = 0x800000;
            log.append("Scanning region: 0x0 – 0x7FFFFF\n");
        }

        int count = 0;
        for (int i = scanStart; i < scanEnd - 16; i++) {
            if (rom[i] == 'M' && rom[i + 1] == 'I' && rom[i + 2] == 'O' && rom[i + 3] == '0') {
                int uncompressedLen = readInt(rom, i + 4);
                int compOffset = readInt(rom, i + 8);
                int uncompOffset = readInt(rom, i + 12);

                if (uncompressedLen <= 0 || uncompressedLen > 0x400000) continue;
                if (i + compOffset >= rom.length) continue;
                if (i + uncompOffset >= rom.length) continue;

                String name = String.format("mio0_%06X", i);

                log.append("Valid MIO0 at 0x" + Integer.toHexString(i).toUpperCase()
                        + " (" + uncompressedLen + " bytes) → " + name + "\n");

                try {
                    byte[] decoded = decodeMIO0(rom, i);
                    File outFile = new File(outDir, name + ".bin");
                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        fos.write(decoded);
                    }
                    log.append("→ Extracted " + outFile.getName()
                            + " (" + decoded.length + " bytes)\n");
                    count++;
                } catch (Exception e) {
                    log.append("Failed to decode MIO0 at 0x" + Integer.toHexString(i).toUpperCase()
                            + ": " + e.getMessage() + "\n");
                }
            }
        }
        log.append("Finished. Extracted " + count + " files.\n");
    }

    private int readInt(byte[] arr, int off) {
        return ((arr[off] & 0xFF) << 24)
                | ((arr[off + 1] & 0xFF) << 16)
                | ((arr[off + 2] & 0xFF) << 8)
                | (arr[off + 3] & 0xFF);
    }

    // ✅ Fixed MIO0 decoder
    private byte[] decodeMIO0(byte[] rom, int start) throws IOException {
        int uncompressedLen = readInt(rom, start + 4);
        int compOffset = readInt(rom, start + 8);
        int uncompOffset = readInt(rom, start + 12);

        byte[] output = new byte[uncompressedLen];
        int outPos = 0;

        int mask = 0, maskBits = 0;
        int compPos = start + compOffset;
        int uncompPos = start + uncompOffset;

        while (outPos < uncompressedLen) {
            if (maskBits == 0) {
                mask = rom[compPos++] & 0xFF;
                maskBits = 8;
            }

            if ((mask & 0x80) != 0) {
                // raw byte
                if (uncompPos >= rom.length) throw new IOException("Uncomp overflow");
                output[outPos++] = rom[uncompPos++];
            } else {
                if (compPos + 1 >= rom.length) throw new IOException("Comp overflow");
                int b1 = rom[compPos++] & 0xFF;
                int b2 = rom[compPos++] & 0xFF;

                int dist = ((b1 & 0xF) << 8) | b2;
                int len = (b1 >> 4) + 3;

                int ref = outPos - dist - 1;
                if (ref < 0) ref = 0; // allow backref at start

                for (int j = 0; j < len; j++) {
                    if (outPos >= uncompressedLen) break;
                    output[outPos] = output[ref];
                    outPos++;
                    ref++;
                }
            }

            mask <<= 1;
            maskBits--;
        }

        return output;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MIO0Extractor::new);
    }
}
