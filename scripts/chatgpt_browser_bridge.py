#!/usr/bin/env python3
"""Bridge commands for interacting with an already-open ChatGPT Chrome tab."""

from __future__ import annotations

import argparse
import base64
import json
import shutil
import subprocess
import sys
import time
from dataclasses import dataclass
from typing import Any


DEFAULT_URL_PREFIX = "https://chatgpt.com/"
DEFAULT_MAX_CHARS = 12000
FIELD_SEPARATOR = "|||"

EXIT_OK = 0
EXIT_ENVIRONMENT = 2
EXIT_TAB_NOT_FOUND = 3
EXIT_PAGE_UNREADABLE = 4
EXIT_COMPOSER_UNAVAILABLE = 5
EXIT_SEND_FAILED = 6


@dataclass
class CommandResult:
    url: str
    title: str
    data: dict[str, Any]


@dataclass
class TabInfo:
    window_id: int
    window_index: int
    tab_index: int
    title: str
    url: str
    active: bool

    def to_payload(self) -> dict[str, Any]:
        return {
            "window_index": self.window_index,
            "tab_index": self.tab_index,
            "title": self.title,
            "url": self.url,
            "active": self.active,
        }


class BridgeCommandError(Exception):
    """A structured bridge command error."""

    def __init__(self, code: str, message: str, exit_code: int) -> None:
        super().__init__(message)
        self.code = code
        self.message = message
        self.exit_code = exit_code


class BridgeArgumentParser(argparse.ArgumentParser):
    """Argument parser that raises structured errors instead of exiting."""

    def error(self, message: str) -> None:  # pragma: no cover - exercised via dispatch
        raise BridgeCommandError("INVALID_ARGUMENTS", message, EXIT_ENVIRONMENT)


def build_success(command: str, result: CommandResult) -> dict[str, Any]:
    return {
        "ok": True,
        "command": command,
        "url": result.url,
        "title": result.title,
        "data": result.data,
    }


def build_error(command: str, code: str, message: str) -> dict[str, Any]:
    return {
        "ok": False,
        "command": command,
        "error": {
            "code": code,
            "message": message,
        },
    }


def create_parser() -> BridgeArgumentParser:
    parser = BridgeArgumentParser(prog="chatgpt_browser_bridge.py")
    subparsers = parser.add_subparsers(dest="command", required=True)

    list_tabs = subparsers.add_parser("list-tabs")
    list_tabs.add_argument("--tab-match", default="")

    read = subparsers.add_parser("read")
    read.add_argument("--tab-match", default="")
    read.add_argument("--output", default="json", choices=["json"])
    read.add_argument("--max-chars", type=int, default=DEFAULT_MAX_CHARS)

    type_cmd = subparsers.add_parser("type")
    type_cmd.add_argument("--tab-match", default="")
    type_cmd.add_argument("--text")
    type_cmd.add_argument("--send-after-type", action="store_true")
    type_cmd.add_argument("--max-chars", type=int, default=DEFAULT_MAX_CHARS)

    send = subparsers.add_parser("send")
    send.add_argument("--tab-match", default="")
    send.add_argument("--max-chars", type=int, default=DEFAULT_MAX_CHARS)

    return parser


def dispatch(argv: list[str]) -> tuple[int, dict[str, Any]]:
    command = argv[0] if argv else "unknown"
    try:
        args = create_parser().parse_args(argv)
        command = args.command

        if command == "list-tabs":
            result = handle_list_tabs(args)
        elif command == "read":
            result = handle_read(args)
        elif command == "type":
            if not args.text:
                raise BridgeCommandError(
                    "TEXT_REQUIRED",
                    "type 命令必须显式提供 --text。",
                    EXIT_ENVIRONMENT,
                )
            result = handle_type(args)
        elif command == "send":
            result = handle_send(args)
        else:  # pragma: no cover - argparse already guards this
            raise BridgeCommandError(
                "UNKNOWN_COMMAND",
                f"未知命令：{command}",
                EXIT_ENVIRONMENT,
            )
        return EXIT_OK, build_success(command, result)
    except BridgeCommandError as exc:
        return exc.exit_code, build_error(command, exc.code, exc.message)
    except Exception as exc:  # pragma: no cover - defensive guard
        return EXIT_ENVIRONMENT, build_error(command, "INTERNAL_ERROR", str(exc))


def main(argv: list[str] | None = None) -> int:
    exit_code, payload = dispatch(argv or sys.argv[1:])
    print(json.dumps(payload, ensure_ascii=False))
    return exit_code


def handle_list_tabs(args: argparse.Namespace) -> CommandResult:
    tabs = find_chatgpt_tabs(args.tab_match)
    active = next((tab for tab in tabs if tab.active), None)
    return CommandResult(
        url=active.url if active else "",
        title=active.title if active else "",
        data={"tabs": [tab.to_payload() for tab in tabs]},
    )


def handle_read(args: argparse.Namespace) -> CommandResult:
    tab = select_target_tab(args.tab_match)
    snapshot = read_snapshot(tab, max_chars=args.max_chars)
    ensure_page_readable(snapshot)
    return CommandResult(
        url=snapshot["url"],
        title=snapshot["title"],
        data={
            "messages": snapshot["messages"],
            "bodyText": snapshot["bodyText"],
            "composer": snapshot["composer"],
            "sendButtonAvailable": snapshot["sendButtonAvailable"],
        },
    )


def handle_type(args: argparse.Namespace) -> CommandResult:
    tab = select_target_tab(args.tab_match)
    snapshot = read_snapshot(tab, max_chars=args.max_chars)
    ensure_page_readable(snapshot)
    type_result = perform_type(tab, args.text)

    payload = {
        "typed": True,
        "sent": False,
        "composer": {
            "available": True,
            "mode": type_result["mode"],
            "currentText": type_result["currentText"],
        },
    }

    if args.send_after_type:
        send_result = perform_send(tab, max_chars=args.max_chars)
        payload["sent"] = True
        payload["send"] = send_result

    return CommandResult(
        url=tab.url,
        title=tab.title,
        data=payload,
    )


def handle_send(args: argparse.Namespace) -> CommandResult:
    tab = select_target_tab(args.tab_match)
    send_result = perform_send(tab, max_chars=args.max_chars)
    return CommandResult(
        url=tab.url,
        title=tab.title,
        data=send_result,
    )


def ensure_supported_platform() -> None:
    if sys.platform != "darwin":
        raise BridgeCommandError(
            "UNSUPPORTED_PLATFORM",
            "当前桥接器首轮仅支持 macOS。",
            EXIT_ENVIRONMENT,
        )


def ensure_osascript_available() -> None:
    ensure_supported_platform()
    if shutil.which("osascript") is None:
        raise BridgeCommandError(
            "OSASCRIPT_NOT_AVAILABLE",
            "当前系统未找到 osascript，无法调用 AppleScript。",
            EXIT_ENVIRONMENT,
        )


def ensure_chrome_running() -> None:
    ensure_osascript_available()
    try:
        process = subprocess.run(
            ["pgrep", "-x", "Google Chrome"],
            capture_output=True,
            text=True,
            check=False,
        )
        if process.returncode == 0:
            return
    except FileNotFoundError:
        pass

    raise BridgeCommandError(
        "CHROME_NOT_RUNNING",
        "Google Chrome 未运行，无法桥接已打开的 ChatGPT 标签页。",
        EXIT_ENVIRONMENT,
    )


def apple_script_string(value: str) -> str:
    escaped = value.replace("\\", "\\\\").replace('"', '\\"')
    return f'"{escaped}"'


def run_osascript(script: str, *, exit_code: int = EXIT_ENVIRONMENT) -> str:
    ensure_chrome_running()
    process = subprocess.run(
        ["osascript", "-e", script],
        capture_output=True,
        text=True,
        check=False,
    )
    if process.returncode != 0:
        stderr = process.stderr.strip() or process.stdout.strip() or "AppleScript 执行失败。"
        if is_apple_events_javascript_disabled(stderr):
            raise BridgeCommandError(
                "APPLE_EVENTS_JAVASCRIPT_DISABLED",
                "Google Chrome 未开启 Apple 事件 JavaScript 执行。",
                EXIT_ENVIRONMENT,
            )
        raise BridgeCommandError("APPLESCRIPT_FAILED", stderr, exit_code)
    return process.stdout.strip()


def is_apple_events_javascript_disabled(message: str) -> bool:
    lowered = message.lower()
    markers = [
        "executing javascript through applescript is turned off",
        "allow javascript from apple events",
        "通过 applescript 执行 javascript 的功能已关闭",
        "apple 事件中的 javascript",
    ]
    return any(marker in lowered for marker in markers)


def find_chatgpt_tabs(tab_match: str = "") -> list[TabInfo]:
    prefix_literal = apple_script_string(DEFAULT_URL_PREFIX)
    matcher_literal = apple_script_string(tab_match)
    separator_literal = apple_script_string(FIELD_SEPARATOR)
    script = f"""
tell application "Google Chrome"
  set targetPrefix to {prefix_literal}
  set tabMatcher to {matcher_literal}
  set fieldSeparator to {separator_literal}
  set outLines to {{}}
  set frontWindowId to -1
  try
    set frontWindowId to id of front window
  end try
  repeat with w in windows
    set wId to id of w
    set wIndex to index of w
    set activeTabIndex to active tab index of w
    set tIndex to 0
    repeat with t in tabs of w
      set tIndex to tIndex + 1
      try
        set tabUrl to URL of t
        set tabTitle to title of t
        if tabUrl starts with targetPrefix then
          if tabMatcher is "" or tabUrl contains tabMatcher or tabTitle contains tabMatcher then
            set isActive to false
            if wId = frontWindowId and tIndex = activeTabIndex then
              set isActive to true
            end if
            set end of outLines to (wId as string) & fieldSeparator & (wIndex as string) & fieldSeparator & (tIndex as string) & fieldSeparator & tabTitle & fieldSeparator & tabUrl & fieldSeparator & (isActive as string)
          end if
        end if
      end try
    end repeat
  end repeat
  set oldDelims to AppleScript's text item delimiters
  set AppleScript's text item delimiters to linefeed
  set outputText to outLines as text
  set AppleScript's text item delimiters to oldDelims
  return outputText
end tell
""".strip()
    output = run_osascript(script)
    tabs: list[TabInfo] = []
    if not output:
        return tabs
    for line in output.splitlines():
        fields = line.split(FIELD_SEPARATOR)
        if len(fields) != 6:
            continue
        try:
            tabs.append(
                TabInfo(
                    window_id=int(fields[0]),
                    window_index=int(fields[1]),
                    tab_index=int(fields[2]),
                    title=fields[3],
                    url=fields[4],
                    active=fields[5].lower() == "true",
                )
            )
        except ValueError:
            continue
    return tabs


def select_target_tab(tab_match: str = "") -> TabInfo:
    tabs = find_chatgpt_tabs(tab_match)
    if not tabs:
        suffix = f"（匹配：{tab_match}）" if tab_match else ""
        raise BridgeCommandError(
            "TARGET_TAB_NOT_FOUND",
            f"未找到已打开的 {DEFAULT_URL_PREFIX} 标签页{suffix}。",
            EXIT_TAB_NOT_FOUND,
        )
    target = next((tab for tab in tabs if tab.active), tabs[0])
    activate_tab(target)
    return target


def activate_tab(tab: TabInfo) -> None:
    script = f"""
tell application "Google Chrome"
  repeat with w in windows
    if id of w is {tab.window_id} then
      set index of w to 1
      set active tab index of w to {tab.tab_index}
      return
    end if
  end repeat
end tell
""".strip()
    run_osascript(script, exit_code=EXIT_TAB_NOT_FOUND)


def execute_javascript_on_selected_tab(tab: TabInfo, javascript: str, *, exit_code: int) -> str:
    activate_tab(tab)
    encoded = base64.b64encode(javascript.encode("utf-8")).decode("ascii")
    encoded_literal = apple_script_string(encoded)
    script = f"""
tell application "Google Chrome"
  set encodedJs to {encoded_literal}
  return execute active tab of front window javascript "eval(atob('" & encodedJs & "'))"
end tell
""".strip()
    return run_osascript(script, exit_code=exit_code)


def build_read_javascript(max_chars: int) -> str:
    return f"""
(() => {{
  const maxChars = {max_chars};
  const normalize = (value) => String(value || "")
    .replace(/\\r/g, "")
    .replace(/\\n{{3,}}/g, "\\n\\n")
    .trim()
    .slice(0, maxChars);
  const findComposer = () => {{
    const textarea = document.querySelector('textarea:not([disabled])');
    const rich = document.querySelector('[contenteditable="true"][role="textbox"], div.ProseMirror[contenteditable="true"]');
    const node = rich || textarea;
    if (!node) {{
      return {{
        available: false,
        mode: "",
        currentText: "",
      }};
    }}
    const currentText = "value" in node ? node.value : (node.innerText || node.textContent || "");
    return {{
      available: true,
      mode: node.tagName.toLowerCase(),
      currentText: normalize(currentText),
    }};
  }};
  const findSendButton = () => Array.from(document.querySelectorAll('button')).find((button) => {{
    if (button.disabled) return false;
    const aria = (button.getAttribute('aria-label') || '').trim();
    const testid = (button.getAttribute('data-testid') || '').trim();
    const text = (button.innerText || '').trim();
    return /send|发送|提交/i.test(aria) || /send/i.test(testid) || /^send$/i.test(text) || /^发送$/i.test(text);
  }});
  const messages = Array.from(document.querySelectorAll('[data-message-author-role]')).map((el, index) => ({{
    index: index + 1,
    role: el.getAttribute('data-message-author-role') || '',
    text: normalize(el.innerText || el.textContent || ''),
  }})).filter((item) => item.text);
  return JSON.stringify({{
    url: location.href,
    title: document.title,
    bodyText: normalize(document.body?.innerText || ''),
    messages,
    composer: findComposer(),
    sendButtonAvailable: Boolean(findSendButton()),
  }});
}})()
""".strip()


def build_type_javascript(text: str) -> str:
    encoded_text = base64.b64encode(text.encode("utf-8")).decode("ascii")
    return f"""
(() => {{
  const decodedText = atob('{encoded_text}');
  const textarea = document.querySelector('textarea:not([disabled])');
  const rich = document.querySelector('[contenteditable="true"][role="textbox"], div.ProseMirror[contenteditable="true"]');
  const node = rich || textarea;
  const applyTextarea = (node, value) => {{
    const descriptor = Object.getOwnPropertyDescriptor(window.HTMLTextAreaElement.prototype, 'value');
    if (descriptor && descriptor.set) {{
      descriptor.set.call(node, value);
    }} else {{
      node.value = value;
    }}
    node.dispatchEvent(new InputEvent('input', {{ bubbles: true, data: value, inputType: 'insertText' }}));
    node.dispatchEvent(new Event('change', {{ bubbles: true }}));
  }};
  const applyRich = (node, value) => {{
    node.focus();
    node.innerHTML = '';
    node.textContent = value;
    node.dispatchEvent(new InputEvent('input', {{ bubbles: true, data: value, inputType: 'insertText' }}));
  }};
  if (!node) {{
    return JSON.stringify({{ ok: false, reason: 'composer-not-found' }});
  }}
  if ('value' in node) {{
    applyTextarea(node, decodedText);
  }} else {{
    applyRich(node, decodedText);
  }}
  const currentText = 'value' in node ? node.value : (node.innerText || node.textContent || '');
  return JSON.stringify({{
    ok: true,
    mode: node.tagName.toLowerCase(),
    currentText,
  }});
}})()
""".strip()


def build_send_javascript() -> str:
    return """
(() => {
  const composer = document.querySelector('[contenteditable="true"][role="textbox"], div.ProseMirror[contenteditable="true"], textarea:not([disabled])');
  const button = Array.from(document.querySelectorAll('button')).find((candidate) => {
    if (candidate.disabled) return false;
    const aria = (candidate.getAttribute('aria-label') || '').trim();
    const testid = (candidate.getAttribute('data-testid') || '').trim();
    const text = (candidate.innerText || '').trim();
    return /send|发送|提交/i.test(aria) || /send/i.test(testid) || /^send$/i.test(text) || /^发送$/i.test(text);
  });
  if (button) {
    button.click();
    return JSON.stringify({ sent: true, method: 'button' });
  }
  if (!composer) {
    return JSON.stringify({ sent: false, reason: 'composer-not-found' });
  }
  composer.focus();
  const eventInit = {
    key: 'Enter',
    code: 'Enter',
    which: 13,
    keyCode: 13,
    bubbles: true,
    cancelable: true,
  };
  composer.dispatchEvent(new KeyboardEvent('keydown', eventInit));
  composer.dispatchEvent(new KeyboardEvent('keypress', eventInit));
  composer.dispatchEvent(new KeyboardEvent('keyup', eventInit));
  return JSON.stringify({ sent: true, method: 'enter' });
})()
""".strip()


def read_snapshot(tab: TabInfo, *, max_chars: int) -> dict[str, Any]:
    raw = execute_javascript_on_selected_tab(
        tab,
        build_read_javascript(max_chars),
        exit_code=EXIT_PAGE_UNREADABLE,
    )
    if not raw:
        raise BridgeCommandError(
            "PAGE_UNREADABLE",
            "目标页面没有返回可读取的结构化内容。",
            EXIT_PAGE_UNREADABLE,
        )
    try:
        snapshot = json.loads(raw)
    except json.JSONDecodeError as exc:
        raise BridgeCommandError(
            "PAGE_UNREADABLE",
            f"目标页面返回了无法解析的内容：{exc}",
            EXIT_PAGE_UNREADABLE,
        ) from exc
    return snapshot


def ensure_page_readable(snapshot: dict[str, Any]) -> None:
    url = str(snapshot.get("url") or "")
    body_text = str(snapshot.get("bodyText") or "")
    if url.startswith("chrome-error://"):
        raise BridgeCommandError(
            "PAGE_UNREADABLE",
            "目标标签页当前是 Chrome 错误页。",
            EXIT_PAGE_UNREADABLE,
        )

    error_markers = [
        "无法访问此网站",
        "ERR_CONNECTION",
        "This site can’t be reached",
        "This site can't be reached",
    ]
    if any(marker in body_text for marker in error_markers):
        raise BridgeCommandError(
            "PAGE_UNREADABLE",
            "目标标签页当前未返回可用的 ChatGPT 页面内容。",
            EXIT_PAGE_UNREADABLE,
        )


def perform_type(tab: TabInfo, text: str) -> dict[str, Any]:
    raw = execute_javascript_on_selected_tab(
        tab,
        build_type_javascript(text),
        exit_code=EXIT_COMPOSER_UNAVAILABLE,
    )
    try:
        payload = json.loads(raw)
    except json.JSONDecodeError as exc:
        raise BridgeCommandError(
            "COMPOSER_UNAVAILABLE",
            f"输入框写入结果不可解析：{exc}",
            EXIT_COMPOSER_UNAVAILABLE,
        ) from exc

    if not payload.get("ok"):
        raise BridgeCommandError(
            "COMPOSER_UNAVAILABLE",
            "目标页面未找到可写输入框。",
            EXIT_COMPOSER_UNAVAILABLE,
        )
    return {
        "mode": payload.get("mode") or "",
        "currentText": str(payload.get("currentText") or ""),
    }


def perform_send(tab: TabInfo, *, max_chars: int) -> dict[str, Any]:
    before = read_snapshot(tab, max_chars=max_chars)
    ensure_page_readable(before)
    current_text = str(before.get("composer", {}).get("currentText") or "")
    if not current_text:
        raise BridgeCommandError(
            "NOTHING_TO_SEND",
            "当前输入框为空，无法执行发送。",
            EXIT_SEND_FAILED,
        )

    raw = execute_javascript_on_selected_tab(
        tab,
        build_send_javascript(),
        exit_code=EXIT_SEND_FAILED,
    )
    try:
        payload = json.loads(raw)
    except json.JSONDecodeError as exc:
        raise BridgeCommandError(
            "SEND_FAILED",
            f"发送结果不可解析：{exc}",
            EXIT_SEND_FAILED,
        ) from exc

    if not payload.get("sent"):
        reason = payload.get("reason") or "unknown"
        raise BridgeCommandError(
            "SEND_FAILED",
            f"发送失败：{reason}",
            EXIT_SEND_FAILED,
        )

    method = str(payload.get("method") or "")
    for _ in range(4):
        time.sleep(0.5)
        after = read_snapshot(tab, max_chars=max_chars)
        ensure_page_readable(after)
        after_text = str(after.get("composer", {}).get("currentText") or "")
        if after_text != current_text:
            return {
                "sent": True,
                "method": method,
                "composer": after.get("composer", {}),
            }

    raise BridgeCommandError(
        "SEND_FAILED",
        "发送动作已触发，但输入框内容未变化，无法确认消息已发送。",
        EXIT_SEND_FAILED,
    )


if __name__ == "__main__":
    raise SystemExit(main())
