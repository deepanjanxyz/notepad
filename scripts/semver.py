#!/usr/bin/env python3
"""Small SemVer 2.0 ordering helper for the release workflows."""

from __future__ import annotations

import re
import sys
from functools import cmp_to_key

VERSION = re.compile(
    r"^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)"
    r"(?:-([0-9A-Za-z.-]+))?"
    r"(?:\+[0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*)?$"
)


def parse(value: str) -> tuple[tuple[int, int, int], tuple[str, ...] | None]:
    match = VERSION.fullmatch(value.removeprefix("v"))
    if not match:
        raise ValueError(f"invalid SemVer version: {value}")
    core = tuple(int(match.group(i)) for i in range(1, 4))
    prerelease = match.group(4)
    if prerelease is None:
        return core, None
    identifiers = tuple(prerelease.split("."))
    for identifier in identifiers:
        if not identifier or (identifier.isdigit() and len(identifier) > 1 and identifier[0] == "0"):
            raise ValueError(f"invalid SemVer prerelease identifier: {value}")
    return core, identifiers


def compare(left: str, right: str) -> int:
    left_core, left_pre = parse(left)
    right_core, right_pre = parse(right)
    if left_core != right_core:
        return (left_core > right_core) - (left_core < right_core)
    if left_pre is None or right_pre is None:
        return (left_pre is None) - (right_pre is None)
    for a, b in zip(left_pre, right_pre):
        if a == b:
            continue
        if a.isdigit() and b.isdigit():
            return (int(a) > int(b)) - (int(a) < int(b))
        if a.isdigit() != b.isdigit():
            return -1 if a.isdigit() else 1
        return (a > b) - (a < b)
    return (len(left_pre) > len(right_pre)) - (len(left_pre) < len(right_pre))


def main() -> None:
    command = sys.argv[1]
    if command == "latest":
        versions = []
        for line in sys.stdin:
            value = line.strip().removeprefix("v")
            if not value:
                continue
            try:
                parse(value)
            except ValueError:
                continue
            versions.append(value)
        print(max(versions, key=cmp_to_key(compare)) if versions else "")
    elif command == "max":
        print(max(sys.argv[2:], key=cmp_to_key(compare)))
    elif command == "gt":
        print("true" if compare(sys.argv[2], sys.argv[3]) > 0 else "false")
    else:
        raise SystemExit(f"unknown command: {command}")


if __name__ == "__main__":
    main()
