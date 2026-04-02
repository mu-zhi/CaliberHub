#!/usr/bin/env python3
"""Poll a ChatGPT thread and optionally ask Codex to draft the next response."""

from __future__ import annotations

import argparse
import hashlib
import json
import subprocess
import sys
import time
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


DEFAULT_REPO = "/Users/rlc/Code/CaliberHub"
DEFAULT_TARGET_URL = "https://chatgpt.com/"
DEFAULT_STATE_DIR = Path.home() / ".caliberhub" / "chatgpt-heartbeat"
DEFAULT_BRIDGE = Path(DEFAULT_REPO) / "scripts" / "chatgpt_browser_bridge.py"
DEFAULT_SENDER = Path(DEFAULT_REPO) / "scripts" / "chatgpt_accessibility_send.py"
DEFAULT_LOG = Path.home() / "Library" / "Logs" / "caliberhub-chatgpt-heartbeat.log"
DEFAULT_LAST_MESSAGE = DEFAULT_STATE_DIR / "last_codex_message.txt"
DEFAULT_SNAPSHOT = DEFAULT_STATE_DIR / "latest_snapshot.json"
DEFAULT_STATE = DEFAULT_STATE_DIR / "state.json"
DEFAULT_FOLLOWUP_WINDOW_SECONDS = 300
DEFAULT_FOLLOWUP_POLL_SECONDS = 20
DEFAULT_MAX_AUTO_ROUNDS = 3
DEFAULT_STALLED_AFTER_SECONDS = 180
DEFAULT_SEND_CONFIRM_ATTEMPTS = 3
DEFAULT_SEND_CONFIRM_POLL_SECONDS = 2


def utc_now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


def append_log(log_path: Path, message: str) -> None:
    log_path.parent.mkdir(parents=True, exist_ok=True)
    with log_path.open("a", encoding="utf-8") as handle:
        handle.write(f"{utc_now_iso()} {message}\n")


def load_json(path: Path, default: dict[str, Any]) -> dict[str, Any]:
    if not path.exists():
        return default
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError:
        return default


def save_json(path: Path, data: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")


def digest_payload(payload: dict[str, Any]) -> str:
    normalized = json.dumps(payload, ensure_ascii=False, sort_keys=True)
    return hashlib.sha256(normalized.encode("utf-8")).hexdigest()


def get_messages(payload: dict[str, Any]) -> list[dict[str, Any]]:
    messages = payload.get("data", {}).get("messages", [])
    if not isinstance(messages, list):
        return []
    return [message for message in messages if isinstance(message, dict)]


def normalize_message_text(text: str) -> str:
    return " ".join(text.split()).strip()


def message_index(message: dict[str, Any]) -> int:
    index = message.get("index")
    return index if isinstance(index, int) else -1


def latest_message_by_role(payload: dict[str, Any], role: str | None = None) -> dict[str, Any] | None:
    messages = get_messages(payload)
    for message in reversed(messages):
        if role is None or message.get("role") == role:
            return message
    return None


def extract_tracking(payload: dict[str, Any]) -> dict[str, Any]:
    latest = latest_message_by_role(payload)
    latest_user = latest_message_by_role(payload, "user")
    latest_assistant = latest_message_by_role(payload, "assistant")
    latest_text = extract_latest_message_text(payload)
    return {
        "latest_role": latest.get("role") if latest else None,
        "latest_index": message_index(latest) if latest else -1,
        "latest_text": latest_text,
        "latest_user_index": message_index(latest_user) if latest_user else -1,
        "latest_user_text": latest_user.get("text", "") if latest_user else "",
        "latest_assistant_index": message_index(latest_assistant) if latest_assistant else -1,
        "latest_assistant_text": latest_assistant.get("text", "") if latest_assistant else "",
    }


def extract_latest_message_text(payload: dict[str, Any]) -> str:
    latest = latest_message_by_role(payload)
    if latest:
        for key in ("text", "content", "body", "message"):
            value = latest.get(key)
            if isinstance(value, str) and value.strip():
                return value.strip()
            if isinstance(value, list):
                flattened = " ".join(
                    item for item in value if isinstance(item, str) and item.strip()
                ).strip()
                if flattened:
                    return flattened
    body_text = payload.get("data", {}).get("bodyText", "")
    if isinstance(body_text, str):
        return body_text.strip()[-4000:]
    return ""


def has_context_marker(payload: dict[str, Any], marker: str) -> bool:
    for message in get_messages(payload):
        text = message.get("text")
        if isinstance(text, str) and marker in text:
            return True
    return False


def infer_conversation_stage(payload: dict[str, Any]) -> str:
    messages = get_messages(payload)
    body_text = payload.get("data", {}).get("bodyText", "")
    if not messages:
        return "empty_session"
    latest = messages[-1]
    latest_role = latest.get("role")
    if isinstance(body_text, str) and "思考中" in body_text:
        return "gpt_thinking"
    if latest_role == "user":
        return "waiting_gpt_reply"
    if latest_role == "assistant":
        return "waiting_our_followup"
    return "unknown"


def should_activate_codex(
    previous_state: dict[str, Any], conversation_stage: str, latest_text: str
) -> bool:
    previous_text = previous_state.get("last_seen_text")
    previous_stage = previous_state.get("conversation_stage")
    if conversation_stage != "waiting_our_followup":
        return False
    if not latest_text.strip():
        return False
    if previous_stage == "waiting_our_followup" and previous_text == latest_text:
        return False
    return True


def seconds_since(timestamp: str | None) -> float | None:
    if not timestamp:
        return None
    try:
        parsed = datetime.fromisoformat(timestamp)
    except ValueError:
        return None
    return max((datetime.now(timezone.utc) - parsed).total_seconds(), 0.0)


def infer_runner_state(
    previous_state: dict[str, Any], tracking: dict[str, Any], stalled_after_seconds: int
) -> str:
    latest_index = tracking["latest_index"]
    latest_role = tracking["latest_role"]
    latest_assistant_index = tracking["latest_assistant_index"]
    latest_user_index = tracking["latest_user_index"]
    last_consumed_assistant_index = int(previous_state.get("last_consumed_assistant_index", -1))
    last_sent_user_index = int(previous_state.get("last_sent_user_index", -1))
    sent_age = seconds_since(previous_state.get("last_sent_at"))

    if latest_index < 0:
        return "idle"
    if latest_assistant_index > last_consumed_assistant_index:
        return "assistant_replied_pending_followup"
    if last_sent_user_index >= 0 and latest_user_index >= last_sent_user_index:
        if latest_assistant_index < last_sent_user_index:
            if sent_age is not None and sent_age >= stalled_after_seconds:
                return "stalled_waiting_assistant"
            return "user_sent_waiting_assistant"
    if latest_role == "assistant":
        return "assistant_replied_consumed"
    if latest_role == "user":
        return "waiting_assistant"
    return "idle"


def should_attempt_followup(
    previous_state: dict[str, Any],
    runner_state: str,
    context_present: bool,
) -> bool:
    if not context_present:
        return False
    return runner_state == "assistant_replied_pending_followup"


def build_codex_prompt(snapshot_path: Path, repo_path: Path, target_url: str, output_path: Path) -> str:
    return f"""你在执行一个每 10 分钟运行一次的非交互巡检任务。

目标：
1. 阅读 {snapshot_path} 中保存的浏览器 GPT 最新会话快照。
2. 以 {repo_path / 'docs/engineering/current-delivery-status.md'} 为当前进度唯一真源。
3. 判断是否需要继续和浏览器中的 GPT 讨论方案，目标是收敛到双方都认可的方案。
4. 如果不需要发送任何内容，最终回复只输出 NO_ACTION。
5. 如果需要回复，最终回复只输出一段可直接发送给浏览器 GPT 的中文消息，要求简洁、具体、推进式，不要寒暄。

硬约束：
- 不要修改仓库文件。
- 不要运行任何破坏性 git 命令。
- 不要重复发送已经说过的话；优先回应新增异议、补充缺口、或明确确认条件。
- 回复必须基于快照中的最新内容，而不是凭空假设。
- 目标是当前唯一已打开的 ChatGPT 标签页；如果 URL 已从首页跳到具体会话页，以当前活动页为准。
"""


def run_bridge(bridge_script: Path, command: str) -> tuple[int, dict[str, Any]]:
    process = subprocess.run(
        [sys.executable, str(bridge_script), command, "--output", "json"]
        if command == "read"
        else [sys.executable, str(bridge_script), command],
        capture_output=True,
        text=True,
        check=False,
    )
    payload = json.loads(process.stdout or "{}")
    return process.returncode, payload


def run_codex(repo_path: Path, prompt: str, output_path: Path, log_path: Path) -> int:
    process = subprocess.run(
        [
            "/usr/local/bin/codex",
            "exec",
            "--skip-git-repo-check",
            "--ephemeral",
            "-C",
            str(repo_path),
            "-s",
            "danger-full-access",
            "-o",
            str(output_path),
            prompt,
        ],
        capture_output=True,
        text=True,
        check=False,
    )
    append_log(log_path, f"codex-exit={process.returncode}")
    if process.stdout.strip():
        append_log(log_path, f"codex-stdout={process.stdout.strip()[:1000]}")
    if process.stderr.strip():
        append_log(log_path, f"codex-stderr={process.stderr.strip()[:1000]}")
    return process.returncode


def send_notification(title: str, body: str) -> None:
    subprocess.run(
        ["osascript", "-e", f'display notification "{body}" with title "{title}"'],
        check=False,
        capture_output=True,
        text=True,
    )


def activate_codex_app(log_path: Path) -> bool:
    process = subprocess.run(
        ["osascript", "-e", 'tell application "Codex" to activate'],
        check=False,
        capture_output=True,
        text=True,
    )
    if process.returncode != 0:
        append_log(log_path, f"activate-codex-failed stderr={process.stderr.strip()[:500]}")
        return False
    append_log(log_path, "activate-codex-ok")
    return True


def confirm_send(
    bridge_script: Path,
    previous_user_index: int,
    expected_text: str,
    attempts: int,
    poll_seconds: int,
    log_path: Path,
) -> tuple[bool, dict[str, Any]]:
    normalized_expected = normalize_message_text(expected_text)
    for _ in range(max(attempts, 1)):
        time.sleep(max(poll_seconds, 1))
        read_exit, payload = run_bridge(bridge_script, "read")
        if read_exit != 0:
            append_log(log_path, "confirm-send-read-failed")
            continue
        tracking = extract_tracking(payload)
        latest_user_text = normalize_message_text(tracking["latest_user_text"])
        if tracking["latest_user_index"] > previous_user_index and (
            latest_user_text == normalized_expected or tracking["latest_role"] == "user"
        ):
            return True, payload
    return False, {}


def process_payload_change(
    payload: dict[str, Any],
    previous_state: dict[str, Any],
    *,
    args: argparse.Namespace,
    repo_path: Path,
    bridge_script: Path,
    log_path: Path,
    state_path: Path,
    snapshot_path: Path,
    output_path: Path,
    sender_script: Path,
) -> tuple[dict[str, Any], bool]:
    state = dict(previous_state)
    snapshot_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    payload_hash = digest_payload(payload)
    tracking = extract_tracking(payload)
    latest_text = tracking["latest_text"]
    context_present = has_context_marker(payload, "同步我们当前方案，请按这个口径继续协作：")
    conversation_stage = infer_conversation_stage(payload)
    runner_state = infer_runner_state(previous_state, tracking, args.stalled_after_seconds)
    append_log(log_path, f"bridge-read-ok hash={payload_hash[:12]}")

    payload_changed = state.get("last_payload_hash") != payload_hash
    if not payload_changed and runner_state != "assistant_replied_pending_followup":
        state["last_checked_at"] = utc_now_iso()
        state["runner_state"] = runner_state
        save_json(state_path, state)
        return state, False

    activate_codex = (
        not args.disable_codex_activate
        and should_activate_codex(previous_state, conversation_stage, latest_text)
    )
    attempt_followup = (
        not args.disable_codex
        and should_attempt_followup(previous_state, runner_state, context_present)
    )

    state["last_payload_hash"] = payload_hash
    state["last_seen_text"] = latest_text[:4000]
    state["last_seen_user_index"] = tracking["latest_user_index"]
    state["last_seen_assistant_index"] = tracking["latest_assistant_index"]
    state["context_present"] = context_present
    state["conversation_stage"] = conversation_stage
    state["runner_state"] = runner_state
    state["last_checked_at"] = utc_now_iso()
    save_json(state_path, state)
    if payload_changed:
        send_notification("GPT 会话有新内容", "检测到浏览器 GPT 会话更新，开始尝试自动跟进。")
    if activate_codex:
        activate_codex_app(log_path)

    if args.disable_codex:
        append_log(log_path, "codex disabled by flag")
        return state, False

    if not attempt_followup:
        append_log(
            log_path,
            f"skip-followup stage={conversation_stage} runner_state={runner_state} context={context_present}",
        )
        return state, False

    prompt = build_codex_prompt(snapshot_path, repo_path, args.target_url, output_path)
    codex_exit = run_codex(repo_path, prompt, output_path, log_path)
    if codex_exit != 0 or not output_path.exists():
        send_notification("GPT 巡检未完成", "Codex 自动跟进失败，已写日志。")
        return state, False

    reply_text = output_path.read_text(encoding="utf-8").strip()
    if not reply_text or reply_text == "NO_ACTION":
        state["last_consumed_assistant_index"] = tracking["latest_assistant_index"]
        state["runner_state"] = "assistant_replied_consumed"
        save_json(state_path, state)
        append_log(log_path, "codex decided no action")
        return state, False

    send_process = subprocess.run(
        [
            sys.executable,
            str(sender_script),
            "--url",
            args.target_url,
            "--text-file",
            str(output_path),
        ],
        check=False,
        capture_output=True,
        text=True,
    )
    if send_process.returncode != 0:
        state["runner_state"] = "stalled_send_failed"
        state["last_stalled_reason"] = "send_process_failed"
        save_json(state_path, state)
        append_log(log_path, f"send-followup-failed rc={send_process.returncode}")
        send_notification("GPT 巡检未完成", "消息发送失败，已进入 stalled。")
        return state, False

    confirmed, confirmed_payload = confirm_send(
        bridge_script,
        tracking["latest_user_index"],
        reply_text,
        args.send_confirm_attempts,
        args.send_confirm_poll_seconds,
        log_path,
    )
    if not confirmed:
        state["runner_state"] = "stalled_send_unconfirmed"
        state["last_stalled_reason"] = "send_unconfirmed"
        save_json(state_path, state)
        append_log(log_path, "send-followup-unconfirmed")
        send_notification("GPT 巡检未完成", "消息发送未确认，已进入 stalled。")
        return state, False

    confirmed_tracking = extract_tracking(confirmed_payload)
    state["last_payload_hash"] = digest_payload(confirmed_payload)
    state["last_seen_text"] = confirmed_tracking["latest_text"][:4000]
    state["last_seen_user_index"] = confirmed_tracking["latest_user_index"]
    state["last_seen_assistant_index"] = confirmed_tracking["latest_assistant_index"]
    state["last_sent_text"] = reply_text[:4000]
    state["last_sent_at"] = utc_now_iso()
    state["last_sent_user_index"] = confirmed_tracking["latest_user_index"]
    state["last_consumed_assistant_index"] = tracking["latest_assistant_index"]
    state["conversation_stage"] = infer_conversation_stage(confirmed_payload)
    state["runner_state"] = infer_runner_state(state, confirmed_tracking, args.stalled_after_seconds)
    state.pop("last_stalled_reason", None)
    save_json(state_path, state)
    append_log(log_path, "sent follow-up message to chatgpt")
    send_notification("GPT 巡检已发送", "已向浏览器 GPT 发送一轮跟进消息。")
    return state, True


def observe_followup_window(
    initial_state: dict[str, Any],
    *,
    args: argparse.Namespace,
    repo_path: Path,
    bridge_script: Path,
    sender_script: Path,
    log_path: Path,
    state_path: Path,
    snapshot_path: Path,
    output_path: Path,
) -> dict[str, Any]:
    state = dict(initial_state)
    deadline = time.monotonic() + max(args.followup_window_seconds, 0)
    sent_rounds = 1
    while sent_rounds < args.max_auto_rounds and time.monotonic() < deadline:
        time.sleep(max(args.followup_poll_seconds, 1))
        read_exit, read_payload = run_bridge(bridge_script, "read")
        if read_exit != 0:
            error = read_payload.get("error", {})
            error_code = error.get("code", "UNKNOWN_ERROR")
            append_log(log_path, f"followup-bridge-read-failed code={error_code}")
            continue
        state, sent_reply = process_payload_change(
            read_payload,
            state,
            args=args,
            repo_path=repo_path,
            bridge_script=bridge_script,
            log_path=log_path,
            state_path=state_path,
            snapshot_path=snapshot_path,
            output_path=output_path,
            sender_script=sender_script,
        )
        if sent_reply:
            sent_rounds += 1
    return state


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo", default=DEFAULT_REPO)
    parser.add_argument("--target-url", default=DEFAULT_TARGET_URL)
    parser.add_argument("--state-dir", default=str(DEFAULT_STATE_DIR))
    parser.add_argument("--disable-codex", action="store_true")
    parser.add_argument("--disable-codex-activate", action="store_true")
    parser.add_argument("--followup-window-seconds", type=int, default=DEFAULT_FOLLOWUP_WINDOW_SECONDS)
    parser.add_argument("--followup-poll-seconds", type=int, default=DEFAULT_FOLLOWUP_POLL_SECONDS)
    parser.add_argument("--max-auto-rounds", type=int, default=DEFAULT_MAX_AUTO_ROUNDS)
    parser.add_argument("--stalled-after-seconds", type=int, default=DEFAULT_STALLED_AFTER_SECONDS)
    parser.add_argument("--send-confirm-attempts", type=int, default=DEFAULT_SEND_CONFIRM_ATTEMPTS)
    parser.add_argument("--send-confirm-poll-seconds", type=int, default=DEFAULT_SEND_CONFIRM_POLL_SECONDS)
    args = parser.parse_args(argv)

    repo_path = Path(args.repo)
    state_dir = Path(args.state_dir)
    bridge_script = repo_path / "scripts" / "chatgpt_browser_bridge.py"
    sender_script = repo_path / "scripts" / "chatgpt_accessibility_send.py"
    log_path = DEFAULT_LOG
    state_path = state_dir / "state.json"
    snapshot_path = state_dir / "latest_snapshot.json"
    output_path = state_dir / "last_codex_message.txt"

    state_dir.mkdir(parents=True, exist_ok=True)
    state = load_json(state_path, default={})

    read_exit, read_payload = run_bridge(bridge_script, "read")
    if read_exit != 0:
        error = read_payload.get("error", {})
        error_code = error.get("code", "UNKNOWN_ERROR")
        append_log(log_path, f"bridge-read-failed code={error_code}")
        if state.get("last_error_code") != error_code:
            send_notification("GPT 巡检受阻", f"浏览器桥接读取失败：{error_code}")
        state["last_error_code"] = error_code
        state["last_checked_at"] = utc_now_iso()
        save_json(state_path, state)
        return 0

    state.pop("last_error_code", None)
    state, sent_reply = process_payload_change(
        read_payload,
        state,
        args=args,
        repo_path=repo_path,
        bridge_script=bridge_script,
        log_path=log_path,
        state_path=state_path,
        snapshot_path=snapshot_path,
        output_path=output_path,
        sender_script=sender_script,
    )
    if sent_reply:
        observe_followup_window(
            state,
            args=args,
            repo_path=repo_path,
            bridge_script=bridge_script,
            sender_script=sender_script,
            log_path=log_path,
            state_path=state_path,
            snapshot_path=snapshot_path,
            output_path=output_path,
        )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
