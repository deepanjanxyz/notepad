#!/usr/bin/env python3
"""Small SemVer 2.0.0 ordering helper for release workflows."""
import re
import sys

VERSION = re.compile(r"^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)(?:-([0-9A-Za-z.-]+))?(?:\+[0-9A-Za-z.-]+)?$")


def key(value):
    value = value.strip()
    if value.startswith("v"):
        value = value[1:]
    match = VERSION.fullmatch(value)
    if not match:
        raise ValueError(f"invalid SemVer version: {value}")
    major, minor, patch = (int(match.group(i)) for i in (1, 2, 3))
    prerelease = match.group(4)
    if prerelease is None:
        return major, minor, patch, 1, ()
    identifiers = []
    for item in prerelease.split("."):
        if item.isdigit():
            if len(item) > 1 and item.startswith("0"):
                raise ValueError(f"invalid numeric prerelease identifier: {value}")
            identifiers.append((0, int(item)))
        else:
            identifiers.append((1, item))
    return major, minor, patch, 0, tuple(identifiers)


def compare(left, right):
    a, b = key(left), key(right)
    if a[:3] != b[:3]:
        return (a[:3] > b[:3]) - (a[:3] < b[:3])
    if a[3] != b[3]:
        return (a[3] > b[3]) - (a[3] < b[3])
    if a[4] == b[4]:
        return 0
    # SemVer: compare prerelease identifiers pairwise; shorter wins if all
    # shared identifiers are equal.
    for x, y in zip(a[4], b[4]):
        if x != y:
            return (x > y) - (x < y)
    return (len(a[4]) > len(b[4])) - (len(a[4]) < len(b[4]))


if len(sys.argv) == 2 and sys.argv[1] == "max":
    versions = [line.strip() for line in sys.stdin if line.strip()]
    if versions:
        print(max(versions, key=key))
elif len(sys.argv) == 4 and sys.argv[1] == "compare":
    print(compare(sys.argv[2], sys.argv[3]))
else:
    raise SystemExit("usage: semver.py max | semver.py compare LEFT RIGHT")
