#!/usr/bin/env python3
"""Send text to an already-open ChatGPT Chrome tab using accessibility paste."""

from __future__ import annotations

import argparse
import subprocess
import sys
import tempfile
from pathlib import Path


def run_osascript(script: str) -> None:
    subprocess.run(["osascript", "-e", script], check=True, capture_output=True, text=True)


def focus_target_tab(target_url: str) -> None:
    script = f"""
tell application "Google Chrome"
  activate
  set targetUrl to "{target_url}"
  set targetPrefix to "https://chatgpt.com/"
  set foundTab to false
  repeat with w in windows
    set tabCount to number of tabs in w
    repeat with i from 1 to tabCount
      set t to tab i of w
      set currentUrl to URL of t
      if (currentUrl is targetUrl) or (targetUrl is targetPrefix and currentUrl starts with targetPrefix) then
        set active tab index of w to i
        set index of w to 1
        set foundTab to true
        exit repeat
      end if
    end repeat
    if foundTab then exit repeat
  end repeat
  if foundTab is false then error "TARGET_TAB_NOT_FOUND"
end tell
"""
    run_osascript(script)


def paste_and_send(text: str) -> None:
    with tempfile.NamedTemporaryFile("w", delete=False, encoding="utf-8") as handle:
        handle.write(text)
        temp_path = Path(handle.name)
    try:
        subprocess.run(["pbcopy"], check=True, input=text, text=True)
        script = """
delay 0.8
tell application "System Events"
  keystroke "v" using command down
  delay 0.5
  key code 36
end tell
"""
        run_osascript(script)
    finally:
        temp_path.unlink(missing_ok=True)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--url", default="https://chatgpt.com/")
    parser.add_argument("--text-file", required=True)
    args = parser.parse_args(argv)

    text = Path(args.text_file).read_text(encoding="utf-8").strip()
    if not text or text == "NO_ACTION":
        return 0

    focus_target_tab(args.url)
    paste_and_send(text)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
