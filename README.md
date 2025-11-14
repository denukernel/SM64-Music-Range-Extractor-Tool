<img width="780" height="380" alt="image" src="https://github.com/user-attachments/assets/4d70c2a5-d983-4773-81e7-62becd221ed7" />

# SM64 Music Range Extractor Tool

A lightweight Java tool that scans a Super Mario 64 ROM and extracts the
exact M64 sequence ranges (ROM offsets and lengths). These ranges are useful for:

- Vinesauce ROM Corruptor
- Custom music injection
- SM64 ROM hacking
- Debugging sequence banks
- Data analysis of the sequence table

## Features
- Detects sequence bank automatically at 0x7B0860  
- Lists all sequences with correct boundaries  
- Converts ROMs (.z64 / .n64 / .v64) to big-endian in memory  
- Exports selected or all ranges to `.txt`  
- Hex viewer for individual sequences (double-click)  
- Simple Swing GUI  
- Optional MIO0 extractor included

## Supported ROM Formats
- **.z64 (big-endian)**
- **.n64 (byteswapped)**
- **.v64 (word-swapped)**

These are auto-normalized to .z64 internally.

## How to Use
1. Launch the JAR  
2. Click **Open ROM**
3. Select your SM64 ROM  
4. Sequences will be listed automatically  
5. Export them via:
   - “Export Selected”
   - “Export ALL Sequences…”
   - “Export ALL Ranges…”

## Requirements
- Java 8 or newer

## License
MIT License
