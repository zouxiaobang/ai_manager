"""Extract all unique Chinese characters from source files for font config."""
import os
import re
import subprocess
import sys

def get_git_root():
    try:
        result = subprocess.run(
            ["git", "rev-parse", "--show-toplevel"],
            capture_output=True, text=True, cwd=os.path.dirname(__file__)
        )
        if result.returncode == 0:
            return result.stdout.strip()
    except:
        pass
    return None

def extract_chinese(text):
    """Extract all unique Chinese characters (CJK Unified Ideographs + punctuation) from text."""
    chars = set()
    for ch in text:
        cp = ord(ch)
        # CJK Unified Ideographs (汉字)
        if 0x4E00 <= cp <= 0x9FFF:
            chars.add(ch)
        # CJK Unified Ideographs Extension A
        elif 0x3400 <= cp <= 0x4DBF:
            chars.add(ch)
        # CJK Symbols and Punctuation （，。、：；？！等）
        elif 0x3000 <= cp <= 0x303F:
            chars.add(ch)
        # Fullwidth Forms （，。！？等全角符号）
        elif 0xFF00 <= cp <= 0xFFEF:
            chars.add(ch)
        # General Punctuation （部分中文标点如·、—）
        elif 0x2000 <= cp <= 0x206F:
            if ch in ('·', '—', '…', '─'):
                chars.add(ch)
    return chars

def collect_from_file(filepath):
    chars = set()
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            for line in f:
                # Skip comment-only lines (C-style comments)
                stripped = line.strip()
                if stripped.startswith('//') or stripped.startswith('/*') or stripped.startswith('*'):
                    # Keep Chinese chars from comments too - some comments contain important info
                    pass
                chars.update(extract_chinese(line))
    except Exception as e:
        print(f"Error reading {filepath}: {e}")
    return chars

def main():
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    main_dir = os.path.join(root, 'main')
    
    all_chars = set()
    file_list = []
    
    for dirpath, dirnames, filenames in os.walk(main_dir):
        for fn in filenames:
            if fn.endswith(('.cpp', '.h', '.c')):
                file_list.append(os.path.join(dirpath, fn))
    
    for fp in sorted(file_list):
        chars = collect_from_file(fp)
        if chars:
            print(f"  {os.path.relpath(fp, root)}: {''.join(sorted(chars))}")
            all_chars.update(chars)
    
    sorted_chars = sorted(all_chars)
    
    print("\n" + "=" * 60)
    print(f"Total unique Chinese characters: {len(sorted_chars)}")
    print("\nCharacters:")
    print(''.join(sorted_chars))
    
    print("\n\nSorted by Unicode codepoint:")
    for i, ch in enumerate(sorted_chars, 1):
        print(f"  U+{ord(ch):04X}  {ch}")
    
    # Generate hex array for font config
    print("\n\nHex codes for font config:")
    hex_codes = ', '.join(f'0x{ord(ch):04X}' for ch in sorted_chars)
    print(hex_codes)
    
    # Also output as C array
    print("\n\nAs C array:")
    print(f"static const uint16_t chinese_chars[] = {{")
    for i in range(0, len(sorted_chars), 10):
        chunk = sorted_chars[i:i+10]
        codes = ', '.join(f'0x{ord(ch):04X}' for ch in chunk)
        print(f"    {codes},")
    print(f"}};")

if __name__ == '__main__':
    main()
