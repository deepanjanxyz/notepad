#!/usr/bin/env python3
"""Drive the Elite Memo Pro debug build on an emulator and capture screenshots.

The driver only uses adb: it locates real UI elements from `uiautomator dump`
output (by text or content description) and acts on their bounds, so nothing
relies on hard-coded screen coordinates. Note dispositions (pin, archive, label,
trash) are applied while the note is still the one on screen, so no list
scrolling is ever required.
"""

import math
import os
import re
import subprocess
import time
import traceback
import xml.etree.ElementTree as ET

PKG = "com.deepanjanxyz.notepad"
ACTIVITY = PKG + "/.MainActivity"

ROOT = os.getcwd()
SHOTS_DIR = os.path.join(ROOT, "screenshots")
LOGS_DIR = os.path.join(ROOT, "notes-and-logs")
DEBUG_DIR = os.path.join(LOGS_DIR, "debug")
os.makedirs(SHOTS_DIR, exist_ok=True)
os.makedirs(LOGS_DIR, exist_ok=True)
os.makedirs(DEBUG_DIR, exist_ok=True)
LOG_PATH = os.path.join(LOGS_DIR, "run.log")

captures = []
log_lines = []
_dump_logged = False

SHEET_MARKERS = ("SELECT LABELS", "CHOOSE COLOR", "Create Note", "Create new label")
SEARCH_HINT = "Search notes, titles, thoughts..."


def log(msg):
    line = "[%s] %s" % (time.strftime("%H:%M:%S"), msg)
    print(line, flush=True)
    log_lines.append(line)


def adb(*args, binary=False, check=False):
    res = subprocess.run(["adb", *args], capture_output=True, check=check)
    if binary:
        return res.stdout
    return res.stdout.decode("utf-8", "replace")


def adb_full(*args):
    res = subprocess.run(["adb", *args], capture_output=True)
    return (res.stdout.decode("utf-8", "replace"),
            res.stderr.decode("utf-8", "replace"),
            res.returncode)


# ---------------------------------------------------------------- UI helpers

def _clean(xml):
    start = xml.find("<hierarchy")
    if start < 0:
        start = xml.find("<?xml")
    if start < 0:
        start = 0
    end = xml.rfind("</hierarchy>")
    end = end + len("</hierarchy>") if end >= 0 else len(xml)
    return xml[start:end]


def dump():
    global _dump_logged
    tty_out, _, tty_rc = adb_full("exec-out", "uiautomator", "dump", "/dev/tty")
    candidate = _clean(tty_out)
    if "<hierarchy" in candidate:
        if not _dump_logged:
            log("dump: ok via tty (len=%d)" % len(candidate))
            _dump_logged = True
        return candidate
    adb_full("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    candidate = _clean(adb("exec-out", "cat", "/sdcard/ui.xml"))
    if not _dump_logged:
        log("dump: tty failed rc=%s; file len=%d" % (tty_rc, len(candidate)))
        _dump_logged = True
    return candidate


def parse(xml):
    try:
        return list(ET.fromstring(xml).iter("node"))
    except ET.ParseError:
        return []


def bounds(node):
    m = re.search(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.get("bounds", ""))
    if not m:
        return None
    return tuple(int(g) for g in m.groups())


def center(node):
    b = bounds(node)
    if not b:
        return None
    return ((b[0] + b[2]) // 2, (b[1] + b[3]) // 2)


def find(xml, text=None, desc=None, exact=True):
    out = []
    for n in parse(xml):
        if text is not None:
            t = n.get("text", "")
            if (t != text) if exact else (text not in t):
                continue
        if desc is not None:
            d = n.get("content-desc", "")
            if (d != desc) if exact else (desc not in d):
                continue
        out.append(n)
    return out


def wait_find(predicate, timeout=20.0, interval=0.6):
    deadline = time.time() + timeout
    while time.time() < deadline:
        found = predicate(dump())
        if found:
            return found
        time.sleep(interval)
    return None


def tap(x, y):
    adb("shell", "input", "tap", str(x), str(y))


def tap_node(node):
    c = center(node)
    if not c:
        return False
    tap(*c)
    time.sleep(0.5)
    return True


def tap_text(text, exact=True, timeout=20.0):
    found = wait_find(lambda x: find(x, text=text, exact=exact), timeout)
    return tap_node(found[0]) if found else False


def tap_desc(desc, timeout=20.0):
    found = wait_find(lambda x: find(x, desc=desc), timeout)
    return tap_node(found[0]) if found else False


def tap_any(desc=None, text=None, timeout=20.0):
    def pred(x):
        return (find(x, desc=desc) if desc else []) or (find(x, text=text) if text else [])
    found = wait_find(pred, timeout)
    return tap_node(found[0]) if found else False


def screen_size():
    m = re.search(r"(\d+)x(\d+)", adb("shell", "wm", "size"))
    return (int(m.group(1)), int(m.group(2))) if m else (1080, 2400)


def scroll_top():
    w, h = screen_size()
    for _ in range(8):
        adb("shell", "input", "swipe", str(w // 2), str(int(h * 0.30)),
            str(w // 2), str(int(h * 0.80)), "250")
        time.sleep(0.2)


def sanitize(text):
    allowed = set("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 ,.-")
    cleaned = "".join(c for c in text if c in allowed)
    return cleaned.replace(" ", "%s")


def type_text(text):
    adb("shell", "input", "text", sanitize(text))
    time.sleep(0.4)


def press_back():
    adb("shell", "input", "keyevent", "4")
    time.sleep(0.8)


def ime_shown():
    return "mInputShown=true" in adb("shell", "dumpsys", "input_method")


def hide_ime():
    if ime_shown():
        adb("shell", "input", "keyevent", "4")
        time.sleep(0.5)


def long_press(node):
    c = center(node)
    if not c:
        return False
    adb("shell", "input", "swipe", str(c[0]), str(c[1]), str(c[0]), str(c[1]), "900")
    time.sleep(1.0)
    return True


def edit_fields(xml):
    nodes = [n for n in parse(xml) if "EditText" in n.get("class", "") and bounds(n)]
    nodes.sort(key=lambda n: bounds(n)[1])
    return nodes


def type_into(placeholder, text, timeout=20.0):
    found = wait_find(lambda x: find(x, text=placeholder), timeout)
    if not found:
        return False
    tap_node(found[0])
    type_text(text)
    return True


def type_into_field(index, text):
    nodes = edit_fields(dump())
    if index < len(nodes):
        tap_node(nodes[index])
        type_text(text)
        return True
    return False


def screenshot(name, description):
    data = adb("exec-out", "screencap", "-p", binary=True)
    if not data.startswith(b"\x89PNG"):
        log("WARNING: %s did not look like a PNG" % name)
    with open(os.path.join(SHOTS_DIR, name), "wb") as fh:
        fh.write(data)
    captures.append((name, description))
    log("captured %s" % name)


# ------------------------------------------------------------- diagnostics

def current_focus():
    for line in adb("shell", "dumpsys", "window", "windows").splitlines():
        if "mCurrentFocus" in line:
            return line.strip()
    return "?"


def debug_save(tag):
    xml = dump()
    with open(os.path.join(DEBUG_DIR, tag + ".xml"), "w") as fh:
        fh.write(xml)
    data = adb("exec-out", "screencap", "-p", binary=True)
    with open(os.path.join(DEBUG_DIR, tag + ".png"), "wb") as fh:
        fh.write(data)
    log("debug saved %s (xml=%d, png=%d)" % (tag, len(xml), len(data)))


# ---------------------------------------------------------------- navigation

def at_home(xml):
    return find(xml, desc="New Note") or find(xml, text="All")


def ensure_home(max_presses=2):
    for _ in range(max_presses + 1):
        if at_home(dump()):
            return True
        press_back()
    return bool(at_home(dump()))


def close_overlays(max_presses=4):
    for _ in range(max_presses):
        if any(m in dump() for m in SHEET_MARKERS):
            press_back()
        else:
            return


def close_drawer(max_presses=3):
    for _ in range(max_presses):
        if not find(dump(), text="Source on GitHub"):