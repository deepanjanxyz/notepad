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
            return True
        press_back()
    return False


def clear_search():
    for _ in range(3):
        if find(dump(), desc="Clear search text"):
            tap_desc("Clear search text", timeout=3)
            time.sleep(0.5)
        else:
            return


def open_drawer():
    return tap_desc("Open navigation menu", timeout=5) or tap_desc("Open drawer", timeout=5)


def relaunch_to_home():
    adb("shell", "am", "force-stop", PKG)
    adb("shell", "am", "start", "-W", "-n", ACTIVITY)
    time.sleep(2.5)
    dismiss_dialogs()
    return bool(wait_find(lambda x: at_home(x), timeout=30))


def nav_to(name):
    if not ensure_home():
        relaunch_to_home()
    clear_search()
    if not open_drawer():
        relaunch_to_home()
        if not open_drawer():
            return False
    time.sleep(0.6)
    found = wait_find(lambda x: find(x, text=name), timeout=5)
    return tap_node(found[0]) if found else False


def dismiss_dialogs():
    for _ in range(4):
        if "responding" in dump():
            log("dismissing a system dialog")
            if not tap_text("Wait", timeout=3):
                tap_text("Close app", timeout=3)
            time.sleep(1.2)
        else:
            return


def open_editor(title):
    """Open a note through the app's search field (no list scrolling needed)."""
    ensure_home()
    clear_search()
    if not find(dump(), text=SEARCH_HINT):
        log("   open_editor: search field missing; relaunching")
        relaunch_to_home()
    if not type_into(SEARCH_HINT, title):
        log("   open_editor: search field not found")
        return False
    time.sleep(1.0)
    hide_ime()
    found = wait_find(lambda x: find(x, text=title), timeout=8)
    if not found:
        log("   open_editor: '%s' not in search results" % title)
        clear_search()
        return False
    tap_node(found[0])
    ok = wait_find(lambda x: find(x, desc="Save and Close"), timeout=10)
    if not ok:
        log("   open_editor: editor did not open for '%s'" % title)
    return ok


def open_drawing(title):
    """Open a drawing note through search and confirm the drawing canvas loaded."""
    ensure_home()
    clear_search()
    if not find(dump(), text=SEARCH_HINT):
        relaunch_to_home()
    if not type_into(SEARCH_HINT, title):
        return False
    time.sleep(1.0)
    hide_ime()
    found = wait_find(lambda x: find(x, text=title), timeout=8)
    if not found:
        log("   open_drawing: '%s' not in search results" % title)
        clear_search()
        return False
    tap_node(found[0])
    return bool(wait_find(lambda x: find(x, desc="Canvas Background & Grid"), timeout=10))


# ---------------------------------------------------------------- app flow

def launch_app():
    adb("shell", "am", "force-stop", PKG)
    adb("shell", "am", "start", "-W", "-n", ACTIVITY)
    time.sleep(3.0)
    dismiss_dialogs()
    log("pidof=%s focus=%s" % (adb("shell", "pidof", PKG).strip(), current_focus()))
    log("launch: home markers found=%s" % bool(wait_find(lambda x: at_home(x), timeout=40)))
    time.sleep(1.5)


def add_label(label):
    if tap_any(desc="Add label", text="Add label"):
        type_into("Create new label", label)
        tap_desc("Create label", timeout=10)
        time.sleep(0.5)
        close_overlays()


def create_text_note(title, content, pin=False, label=None, archive=False,
                     shot_editor=None, shot_sheets=False):
    if not tap_desc("New Note"):
        return False
    if not tap_text("Text Note"):
        return False
    if not type_into("Title", title):
        return False
    type_into("Note", content)
    if shot_editor:
        hide_ime()
        time.sleep(0.6)
        screenshot(shot_editor,
                   "Note editor with title, body text, labels, and the editing toolbar.")
    if shot_sheets:
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
    if label:
        add_label(label)
    if pin:
        tap_desc("Pin Note", timeout=8)
    if archive:
        tap_desc("Archive Note", timeout=8)
    else:
        tap_desc("Save and Close")
    return ensure_home()


def create_checklist_note(title, items, shot=None):
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
    if shot:
        hide_ime()
        time.sleep(0.6)
        screenshot(shot, "Checklist editor showing the list items and the add item row.")
    tap_desc("Save and Close")
    return ensure_home()


def draw_star(cx, cy, radius):
    """Draw a five-pointed star outline as five straight pen strokes.

    The five outer vertices are computed from the canvas centre and radius, and
    each point of the star is one straight stroke, so the result is a clean,
    recognisable star rather than scattered lines.
    """
    verts = []
    for k in range(5):
        angle = math.radians(-90 + k * 72)
        verts.append((cx + radius * math.cos(angle), cy + radius * math.sin(angle)))
    for a, b in ((0, 2), (2, 4), (4, 1), (1, 3), (3, 0)):
        x1, y1 = verts[a]
        x2, y2 = verts[b]
        adb("shell", "input", "swipe", str(int(x1)), str(int(y1)),
            str(int(x2)), str(int(y2)), "300")
        time.sleep(0.35)


def create_drawing_note(title, shot=None):
    if not tap_desc("New Note"):
        return False
    if not tap_text("Drawing Note"):
        return False
    # The drawing canvas is ready only once the drawing top bar is on screen.
    if not wait_find(lambda x: find(x, desc="Canvas Background & Grid") or
                     (find(x, desc="Back") and find(x, text="Title")), timeout=20):
        log("   create_drawing_note: drawing canvas did not open")
        return False
    w, h = screen_size()
    draw_star(w * 0.5, h * 0.46, h * 0.18)
    type_into("Title", title)
    type_into_field(0, title)
    if shot:
        hide_ime()
        time.sleep(0.6)
        screenshot(shot, "Drawing note showing a star drawn on the canvas with the pen toolbar.")
    time.sleep(0.5)
    tap_desc("Back")
    wait_find(lambda x: find(x, desc="Save and Close"), timeout=15)
    tap_desc("Save and Close")
    return ensure_home()


def create_and_trash(title, content):
    if not create_text_note(title, content):
        return False
    # The newest note is the first card in the list (ORDER BY PINNED DESC, ID DESC).
    node = wait_find(lambda x: find(x, text=title), timeout=8)
    if not node:
        log("   create_and_trash: '%s' not on top of the list" % title)
        return False
    long_press(node[0])
    if not wait_find(lambda x: find(x, text="Selected", exact=False), timeout=8):
        return False
    ok = tap_desc("Move to Trash", timeout=8)
    return ensure_home() and ok


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
        for name, description in sorted(captures):
            lines.append("- `%s` - %s" % (name, description))
    else:
        lines.append("_No screenshots were captured._")
    lines += [
        "",
        "## Notes",
        "",
        "- All screenshots are captured in dark mode only.",
        "- The note editor and the drawing canvas are dark by design regardless of the",
        "  selected theme.",
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


def handle_notification_permission(timeout=8):
    deadline = time.time() + timeout
    while time.time() < deadline:
        allow = find(dump(), text="Allow")
        if allow:
            log("   reminder: granting notification permission")
            tap_node(allow[0])
            time.sleep(1.0)
            return True
        time.sleep(0.6)
    return False


def open_note_by_title(title):
    """Open a note that is already visible on the home screen (e.g. a pinned note)."""
    ensure_home()
    clear_search()
    for _ in range(3):
        found = wait_find(lambda x: find(x, text=title), timeout=6)
        if not found:
            return False
        tap_node(found[0])
        if wait_find(lambda x: find(x, desc="Save and Close"), timeout=5):
            return True
    log("   open_note_by_title: editor did not open for '%s'" % title)
    return False


def reminder_flow():
    if not open_note_by_title("Weekly Goals"):
        return False
    if not tap_desc("Add reminder", timeout=10):
        log("   reminder: 'Add reminder' button not found")
        return False
    if not wait_find(lambda x: find(x, text="Add Reminder"), timeout=10):
        log("   reminder: dialog did not open")
        return False
    if not (tap_text("Tomorrow Morning (9:00 AM)", timeout=6) or
            tap_text("Later Today (6:00 PM)", timeout=3)):
        log("   reminder: preset chip not found")
        return False
    time.sleep(0.6)
    screenshot("13-reminder-dialog-dark.png",
               "Reminder dialog with a preset time selected, before saving.")
    if not tap_text("Save", timeout=8):
        log("   reminder: Save button not found")
        return False
    handle_notification_permission()
    if not wait_find(lambda x: find(x, desc="Edit reminder"), timeout=10):
        log("   reminder: reminder was not applied")
        return False
    time.sleep(0.8)
    screenshot("14-reminder-applied-dark.png",
               "Note editor showing the reminder applied to the note.")
    tap_desc("Save and Close")
    return ensure_home()


def capture_dark():
    ensure_home()
    clear_search()
    scroll_top()
    screenshot("01-home-grid-dark.png",
               "Home screen, two column grid layout, dark theme, with pinned notes and labels.")
    if tap_desc("Switch to Single Column List", timeout=10):
        time.sleep(1.0)
        screenshot("02-home-list-dark.png",
                   "Home screen, single column list layout, dark theme.")
        tap_desc("Switch to Grid View", timeout=10)
        time.sleep(0.8)

    if type_into(SEARCH_HINT, "project"):
        time.sleep(1.0)
        hide_ime()
        screenshot("03-search-results-dark.png",
                   "Search results filtering notes by the keyword project, dark theme.")
        clear_search()
        time.sleep(0.5)

    if open_drawer():
        time.sleep(1.0)
        screenshot("04-navigation-drawer-dark.png",
                   "Navigation drawer with notes, labels, archive, trash, and settings.")
        close_drawer()
        relaunch_to_home()

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


def main():
    launch_app()
    debug_save("after-launch")

    safe("note Meeting Notes", create_text_note, "Meeting Notes",
         "Q3 planning sync with the product and design leads. Agenda covers roadmap, hiring, and the launch window.",
         True, "Work", False, "05-note-editor-dark.png", True)
    safe("note Project Roadmap", create_text_note, "Project Roadmap",
         "Milestones for the next two quarters: private beta in August and the public launch in October.",
         False, "Work", False)
    safe("note Book Summary", create_text_note, "Book Summary",
         "Key ideas from Atomic Habits: small changes compound and systems beat goals every time.",
         False, None, True)
    safe("note Recipe", create_text_note, "Recipe",
         "One pan lemon chicken with roasted vegetables, garlic, and fresh herbs.",
         False, None, True)
    safe("note Reading List", create_text_note, "Reading List",
         "Three books queued for the month, one finished each week.",
         False, None, True)
    safe("note Travel Itinerary", create_text_note, "Travel Itinerary",
         "Three days in Kyoto: temples in the morning, markets in the afternoon, and a quiet dinner each evening.",
         False, "Travel", False)
    safe("note Workout Plan", create_text_note, "Workout Plan",
         "Push, pull, legs split across four sessions a week with one full rest day in between.",
         False, None, False)
    safe("note Weekly Goals", create_text_note, "Weekly Goals",
         "Ship the settings redesign, review the open pull requests, and write the release notes.",
         True, None, False)
    safe("note Ideas", create_text_note, "Ideas",
         "A calm reading app, a gentle habit tracker, and a simple monthly budgeting tool.",
         False, None, False)
    safe("note Grocery List", create_checklist_note, "Grocery List",
         ["Milk", "Eggs", "Sourdough bread", "Coffee beans", "Olive oil"],
         "08-checklist-editor-dark.png")
    safe("note Product Launch", create_checklist_note, "Product Launch",
         ["Finalise marketing copy", "Prepare store screenshots", "Submit for review"])
    safe("note Sketch", create_drawing_note, "Sketch", "09-drawing-note-dark.png")
    safe("trash Client Feedback", create_and_trash, "Client Feedback",
         "Summary of the latest review: clearer onboarding, faster search, and larger note previews.")
    safe("trash Meeting Recap", create_and_trash, "Meeting Recap",
         "Follow-ups from the design review: adjust spacing and refine the empty states.")

    ensure_home()
    clear_search()
    scroll_top()
    debug_save("before-captures")

    try:
        capture_dark()
    except Exception:
        log("ERROR during dark captures:\n%s" % traceback.format_exc())

    safe("reminder flow", reminder_flow)


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
