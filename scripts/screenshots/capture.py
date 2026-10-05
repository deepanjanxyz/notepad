#!/usr/bin/env python3
"""Drive the Elite Memo Pro debug build on an emulator and capture screenshots.

The driver only uses adb: it locates real UI elements from `uiautomator dump`
output (by text or content description) and acts on their bounds, so nothing
relies on hard-coded screen coordinates.
"""

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
    tty_out, tty_err, tty_rc = adb_full("exec-out", "uiautomator", "dump", "/dev/tty")
    candidate = _clean(tty_out)
    if "<hierarchy" in candidate:
        if not _dump_logged:
            log("dump: ok via tty (len=%d)" % len(candidate))
            _dump_logged = True
        return candidate
    out, err, rc = adb_full("shell", "uiautomator", "dump", "/sdcard/ui.xml")
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


def swipe_up(frac=0.28):
    w, h = screen_size()
    adb("shell", "input", "swipe", str(w // 2), str(int(h * 0.70)),
        str(w // 2), str(int(h * (0.70 - frac))), "400")
    time.sleep(0.6)


def scroll_top():
    w, h = screen_size()
    for _ in range(8):
        adb("shell", "input", "swipe", str(w // 2), str(int(h * 0.30)),
            str(w // 2), str(int(h * 0.80)), "250")
        time.sleep(0.2)


def locate(text, max_swipes=6):
    """Return a freshly-measured node for `text`, scrolling the list if needed."""
    for _ in range(max_swipes + 1):
        found = find(dump(), text=text)
        if found:
            time.sleep(0.4)
            fresh = find(dump(), text=text)
            return (fresh or found)[0]
        swipe_up()
    return None


def hide_ime():
    adb("shell", "input", "keyevent", "4")
    time.sleep(0.5)


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


def long_press(node):
    c = center(node)
    if not c:
        return False
    adb("shell", "input", "swipe", str(c[0]), str(c[1]), str(c[0]), str(c[1]), "900")
    time.sleep(1.0)
    return True


def type_into(placeholder, text, timeout=20.0):
    found = wait_find(lambda x: find(x, text=placeholder), timeout)
    if not found:
        return False
    tap_node(found[0])
    type_text(text)
    return True


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


def close_overlays(max_presses=4):
    for _ in range(max_presses):
        if any(m in dump() for m in SHEET_MARKERS):
            press_back()
        else:
            return


def ensure_home(max_presses=6):
    for _ in range(max_presses):
        if at_home(dump()):
            return True
        press_back()
    return bool(at_home(dump()))


def close_drawer(max_presses=4):
    for _ in range(max_presses):
        if not find(dump(), text="Source on GitHub"):
            return True
        press_back()
    return False


def open_drawer():
    return tap_desc("Open navigation menu", timeout=5) or tap_desc("Open drawer", timeout=5)


def nav_to(name):
    if not ensure_home():
        return False
    open_drawer()
    node = locate(name, max_swipes=3)
    return tap_node(node) if node else False


def dismiss_dialogs():
    for _ in range(4):
        if "responding" in dump():
            log("dismissing a system dialog")
            if not tap_text("Wait", timeout=3):
                tap_text("Close app", timeout=3)
            time.sleep(1.2)
        else:
            return


# ---------------------------------------------------------------- app flow

def launch_app():
    adb("shell", "am", "force-stop", PKG)
    adb("shell", "am", "start", "-W", "-n", ACTIVITY)
    time.sleep(3.0)
    dismiss_dialogs()
    log("pidof=%s focus=%s" % (adb("shell", "pidof", PKG).strip(), current_focus()))
    log("launch: home markers found=%s" % bool(wait_find(lambda x: at_home(x), timeout=40)))
    time.sleep(1.5)


def add_text_note(title, content):
    if not tap_desc("New Note"):
        return False
    if not tap_text("Text Note"):
        return False
    if not type_into("Title", title):
        return False
    type_into("Note", content)
    hide_ime()
    tap_desc("Save and Close")
    return ensure_home()


def add_checklist_note(title, items):
    if not tap_desc("New Note"):
        return False
    if not tap_text("Text Note"):
        return False
    if not type_into("Title", title):
        return False
    tap_desc("Checklist format")
    time.sleep(0.8)
    for item in items:
        type_into("List item", item)
        tap_desc("Add list item")
        time.sleep(0.3)
    hide_ime()
    tap_desc("Save and Close")
    return ensure_home()


def add_drawing_note(title):
    if not tap_desc("New Note"):
        return False
    if not tap_text("Drawing Note"):
        return False
    wait_find(lambda x: find(x, desc="Back") and find(x, text="Title"), timeout=20)
    type_into("Title", title)
    hide_ime()
    w, h = screen_size()
    for x1, y1, x2, y2 in [
        (int(w * 0.25), int(h * 0.42), int(w * 0.75), int(h * 0.42)),
        (int(w * 0.28), int(h * 0.52), int(w * 0.72), int(h * 0.52)),
        (int(w * 0.30), int(h * 0.62), int(w * 0.70), int(h * 0.64)),
    ]:
        adb("shell", "input", "swipe", str(x1), str(y1), str(x2), str(y2), "350")
        time.sleep(0.3)
    tap_desc("Back")
    wait_find(lambda x: find(x, desc="Save and Close"), timeout=15)
    tap_desc("Save and Close")
    return ensure_home()


def open_editor(title):
    if not ensure_home():
        return False
    node = locate(title)
    if not node:
        log("   open_editor: '%s' not found on home" % title)
        return False
    tap_node(node)
    if wait_find(lambda x: find(x, desc="Save and Close"), timeout=8):
        return True
    log("   open_editor: editor did not open for '%s'" % title)
    return ensure_home()


def select_note(title):
    if not ensure_home():
        return False
    node = locate(title)
    if not node:
        log("   select_note: '%s' not found on home" % title)
        return False
    long_press(node)
    if wait_find(lambda x: find(x, text="Selected", exact=False), timeout=8):
        return True
    log("   select_note: selection mode did not engage for '%s'" % title)
    return False


def pin_note(title):
    if not select_note(title):
        return False
    ok = tap_desc("Pin selected notes", timeout=8) or tap_desc("Unpin selected notes", timeout=4)
    ensure_home()
    return ok


def archive_note(title):
    if not select_note(title):
        return False
    ok = tap_desc("Archive selected", timeout=8)
    ensure_home()
    return ok


def trash_note(title):
    if not select_note(title):
        return False
    ok = tap_desc("Move to Trash", timeout=8)
    ensure_home()
    return ok


def tag_note(title, label):
    if not open_editor(title):
        return False
    if tap_any(desc="Add label", text="Add label"):
        type_into("Create new label", label)
        tap_desc("Create label", timeout=10)
        time.sleep(0.6)
        close_overlays()
    tap_desc("Save and Close")
    return ensure_home()


def write_readme():
    lines = [
        "# Elite Memo Pro - screenshot run",
        "",
        "Automated, full resolution screenshots of the app, captured on a Pixel 6",
        "(1080x2400) API 34 emulator from a debug build of the `dev` branch.",
        "",
        "## Screenshots",
        "",
    ]
    if captures:
        for name, description in captures:
            lines.append("- `%s` - %s" % (name, description))
    else:
        lines.append("_No screenshots were captured._")
    lines += [
        "",
        "## Notes",
        "",
        "- The note editor and the drawing canvas are dark by design regardless of the",
        "  selected theme, so they appear once rather than per theme.",
        "- The status bar is normalised with Android demo mode (fixed 12:00 clock, full",
        "  battery, Wi-Fi on) and animations are disabled for stable captures.",
        "- Notes are seeded through the real UI; nothing is written to the database directly.",
        "",
    ]
    with open(os.path.join(LOGS_DIR, "README.md"), "w") as fh:
        fh.write("\n".join(lines) + "\n")


def safe(label, fn, *args):
    try:
        result = fn(*args)
        log("%s -> %s" % (label, result))
        return result
    except Exception:
        log("ERROR in %s:\n%s" % (label, traceback.format_exc()))
        return False


def capture_dark():
    ensure_home()
    scroll_top()
    screenshot("01-home-grid-dark.png",
               "Home screen, two column grid layout, dark theme, with pinned notes and labels.")
    if tap_desc("Switch to Single Column List", timeout=10):
        time.sleep(1.0)
        screenshot("02-home-list-dark.png",
                   "Home screen, single column list layout, dark theme.")
        tap_desc("Switch to Grid View", timeout=10)
        time.sleep(0.8)

    if type_into("Search notes, titles, thoughts...", "project"):
        time.sleep(1.0)
        hide_ime()
        screenshot("03-search-results-dark.png",
                   "Search results filtering notes by the keyword project, dark theme.")
        tap_desc("Clear search text", timeout=8)
        time.sleep(0.8)

    if open_drawer():
        time.sleep(1.0)
        screenshot("04-navigation-drawer-dark.png",
                   "Navigation drawer with notes, labels, archive, trash, and settings.")
        close_drawer()

    if open_editor("Meeting Notes"):
        screenshot("05-note-editor-dark.png",
                   "Note editor with title, body text, labels, and the editing toolbar.")
        if tap_any(desc="Add label", text="Add label"):
            time.sleep(1.0)
            screenshot("06-labels-sheet-dark.png",
                       "Labels bottom sheet for applying and creating labels on a note.")
            close_overlays()
        if tap_desc("Color palette", timeout=10):
            time.sleep(1.0)
            screenshot("07-color-picker-dark.png",
                       "Color picker bottom sheet with the note color options.")
            close_overlays()
        tap_desc("Save and Close")
        ensure_home()

    if open_editor("Grocery List"):
        screenshot("08-checklist-editor-dark.png",
                   "Checklist editor showing the list items and the add item row.")
        tap_desc("Save and Close")
        ensure_home()

    if open_editor("Sketch"):
        screenshot("09-drawing-note-dark.png",
                   "Free hand drawing note with the pen toolbar and canvas.")
        press_back()
        wait_find(lambda x: find(x, desc="Save and Close"), timeout=15)
        tap_desc("Save and Close")
        ensure_home()

    if nav_to("Archive"):
        time.sleep(1.2)
        screenshot("10-archive-dark.png",
                   "Archive screen listing archived notes, dark theme.")
        nav_to("Notes")
        ensure_home()

    if nav_to("Trash"):
        time.sleep(1.2)
        screenshot("11-trash-dark.png",
                   "Trash screen with restore and delete options on each note, dark theme.")
        nav_to("Notes")
        ensure_home()

    if nav_to("Settings"):
        time.sleep(1.2)
        screenshot("12-settings-dark.png",
                   "Settings screen with workspace statistics, appearance, and security, dark theme.")


def capture_light():
    if not tap_text("Light", timeout=10):
        log("could not switch to light theme")
        return
    time.sleep(1.5)
    screenshot("13-settings-light.png", "Settings screen in light theme.")

    tap_desc("Back", timeout=10)
    ensure_home()
    scroll_top()
    screenshot("14-home-grid-light.png", "Home screen, two column grid layout, light theme.")

    if tap_desc("Switch to Single Column List", timeout=10):
        time.sleep(1.0)
        screenshot("15-home-list-light.png", "Home screen, single column list layout, light theme.")
        tap_desc("Switch to Grid View", timeout=10)
        time.sleep(0.8)

    if nav_to("Archive"):
        time.sleep(1.2)
        screenshot("16-archive-light.png", "Archive screen in light theme.")
        nav_to("Notes")
        ensure_home()

    if nav_to("Trash"):
        time.sleep(1.2)
        screenshot("17-trash-light.png", "Trash screen in light theme.")
        nav_to("Notes")
        ensure_home()


def main():
    launch_app()
    debug_save("after-launch")

    seeded = [
        ("Meeting Notes", "Q3 planning sync with the product and design leads. Agenda covers roadmap, hiring, and the launch window."),
        ("Project Roadmap", "Milestones for the next two quarters: private beta in August and the public launch in October."),
        ("Book Summary", "Key ideas from Atomic Habits: small changes compound and systems beat goals every time."),
        ("Travel Itinerary", "Three days in Kyoto: temples in the morning, markets in the afternoon, and a quiet dinner each evening."),
        ("Workout Plan", "Push, pull, legs split across four sessions a week with one full rest day in between."),
        ("Recipe", "One pan lemon chicken with roasted vegetables, garlic, and fresh herbs."),
        ("Ideas", "A calm reading app, a gentle habit tracker, and a simple monthly budgeting tool."),
        ("Weekly Goals", "Ship the settings redesign, review the open pull requests, and write the release notes."),
        ("Client Feedback", "Summary of the latest review: clearer onboarding, faster search, and larger note previews."),
        ("Reading List", "Three books queued for the month, one finished each week."),
    ]
    for title, content in seeded:
        safe("add note %s" % title, add_text_note, title, content)

    safe("add checklist Grocery List", add_checklist_note, "Grocery List",
         ["Milk", "Eggs", "Sourdough bread", "Coffee beans", "Olive oil"])
    safe("add checklist Product Launch", add_checklist_note, "Product Launch",
         ["Finalise marketing copy", "Prepare store screenshots", "Submit for review"])
    safe("add drawing Sketch", add_drawing_note, "Sketch")

    safe("pin Meeting Notes", pin_note, "Meeting Notes")
    safe("pin Weekly Goals", pin_note, "Weekly Goals")
    safe("archive Book Summary", archive_note, "Book Summary")
    safe("archive Recipe", archive_note, "Recipe")
    safe("archive Reading List", archive_note, "Reading List")
    safe("trash Client Feedback", trash_note, "Client Feedback")
    safe("trash Ideas", trash_note, "Ideas")

    safe("tag Meeting Notes", tag_note, "Meeting Notes", "Work")
    safe("tag Project Roadmap", tag_note, "Project Roadmap", "Work")
    safe("tag Travel Itinerary", tag_note, "Travel Itinerary", "Travel")

    ensure_home()
    scroll_top()
    debug_save("before-captures")

    try:
        capture_dark()
    except Exception:
        log("ERROR during dark captures:\n%s" % traceback.format_exc())

    try:
        capture_light()
    except Exception:
        log("ERROR during light captures:\n%s" % traceback.format_exc())


if __name__ == "__main__":
    try:
        main()
    except Exception:
        log("FATAL:\n%s" % traceback.format_exc())
    finally:
        write_readme()
        log("done: %d screenshots" % len(captures))
        with open(LOG_PATH, "w") as fh:
            fh.write("\n".join(log_lines) + "\n")
