package sm64music;

import java.util.HashMap;
import java.util.Map;

public class SequenceEntry {
    public final int id;           // index in bank
    public final int tableOffset;  // entry location inside bank header
    public final int seqOffset;    // ROM offset (absolute)
    public final int length;       // bytes

    private static final Map<Integer, String> SONG_NAMES = new HashMap<>();
    static {
        // Common IDs – you can extend this map.
        SONG_NAMES.put(0x00, "Lakitu Intro");
        SONG_NAMES.put(0x01, "Title Screen");
        SONG_NAMES.put(0x02, "Bob-omb Battlefield");
        SONG_NAMES.put(0x03, "Inside Castle");
        SONG_NAMES.put(0x04, "Snow Mountain");
        SONG_NAMES.put(0x05, "Slider");
        SONG_NAMES.put(0x06, "Metal Cap");
        SONG_NAMES.put(0x07, "Bowser’s Road");
        SONG_NAMES.put(0x08, "Wing Cap");
        SONG_NAMES.put(0x09, "Dire Dire Docks");
        SONG_NAMES.put(0x0A, "Big Boo’s Haunt");
        SONG_NAMES.put(0x0B, "Koopa’s Road");
        SONG_NAMES.put(0x0C, "Boss");
        SONG_NAMES.put(0x0E, "Bowser Battle");
        SONG_NAMES.put(0x12, "Castle Grounds");
        SONG_NAMES.put(0x13, "Final Bowser");
        SONG_NAMES.put(0x19, "Ending");
    }

    public SequenceEntry(int id, int tableOffset, int seqOffset, int length) {
        this.id = id;
        this.tableOffset = tableOffset;
        this.seqOffset = seqOffset;
        this.length = length;
    }

    @Override
    public String toString() {
        String name = SONG_NAMES.getOrDefault(id, "Unknown");
        return String.format("[%02X] %s @0x%06X (len=%d)", id, name, seqOffset, length);
    }
}
