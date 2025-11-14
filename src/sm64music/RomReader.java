package sm64music;

import java.io.*;
import java.util.*;

public class RomReader {
    private final byte[] rom;

    // Music region (ROM) – SM64 (U) [!]
    public static final int MUSIC_START = 0x7B0860;
    public static final int MUSIC_END   = 0x7CC620;

    public RomReader(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            rom = fis.readAllBytes();
        }
        normalizeEndiannessInMemory();
    }

    /** Auto-fix n64/v64 to big-endian .z64 in memory. */
    private void normalizeEndiannessInMemory() {
        if (rom.length < 4) return;
        int w = ((rom[0] & 0xFF) << 24) | ((rom[1] & 0xFF) << 16) | ((rom[2] & 0xFF) << 8) | (rom[3] & 0xFF);
        if (w == 0x37804012) { // .n64 (byte-swapped)
            for (int i = 0; i + 1 < rom.length; i += 2) {
                byte t = rom[i]; rom[i] = rom[i+1]; rom[i+1] = t;
            }
            System.out.println("[ROM] Converted .n64 → .z64 in memory");
        } else if (w == 0x40123780) { // .v64 (word-swapped)
            for (int i = 0; i + 3 < rom.length; i += 4) {
                byte b0 = rom[i], b1 = rom[i+1], b2 = rom[i+2], b3 = rom[i+3];
                rom[i] = b3; rom[i+1] = b2; rom[i+2] = b1; rom[i+3] = b0;
            }
            System.out.println("[ROM] Converted .v64 → .z64 in memory");
        }
    }

    private int be16(int off) {
        return ((rom[off] & 0xFF) << 8) | (rom[off + 1] & 0xFF);
    }

    private int be32(int off) {
        return ((rom[off] & 0xFF) << 24) | ((rom[off + 1] & 0xFF) << 16)
             | ((rom[off + 2] & 0xFF) << 8) | (rom[off + 3] & 0xFF);
    }

    public byte[] getBytes(int off, int len) {
        int a = Math.max(0, off);
        int b = Math.min(rom.length, off + Math.max(0, len));
        return Arrays.copyOfRange(rom, a, b);
    }

    public boolean looksLikeSm64() {
        String title = new String(getBytes(0x20, 20));
        return title.contains("SUPER MARIO 64");
    }

    /** Parse the sequence bank header at 0x7B0860. */
    public List<SequenceEntry> parseSequences() {
        List<SequenceEntry> list = new ArrayList<>();

        int hdr = MUSIC_START;
        if (hdr + 4 > rom.length) return list;

        int revision = be16(hdr + 0);
        int count    = be16(hdr + 2);

        // Basic sanity: count must be > 0 and header + table fits in region
        int tableStart = hdr + 4;
        int tableSize  = count * 8;
        if (count <= 0 || tableStart + tableSize > MUSIC_END || tableStart + tableSize > rom.length) {
            // Fallback: if header looks wrong, bail with empty list
            System.out.println("[SEQ] Invalid sequence header (rev=" + revision + ", count=" + count + ")");
            return list;
        }

        for (int i = 0; i < count; i++) {
            int entryOff = tableStart + i * 8;
            int relStart = be32(entryOff + 0); // relative to MUSIC_START
            int length   = be32(entryOff + 4);

            // Convert to ROM offsets
            int seqRom = MUSIC_START + relStart;

            // Validate bounds
            if (relStart < 0 || length <= 0) continue;
            if (seqRom < MUSIC_START || seqRom + length > MUSIC_END) continue;
            if (seqRom + length > rom.length) continue;

            list.add(new SequenceEntry(i, entryOff, seqRom, length));
        }

        System.out.println("[SEQ] Parsed " + list.size() + " sequences (rev=" + revision + ", count=" + count + ")");
        return list;
    }
}
