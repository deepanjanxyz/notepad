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
    line = "[ %s] %s" % (time.strftime("%H:%M:%S"), msg)
    print(line, flush=True)
    log_lines.append(line)

