#!/usr/bin/env python3
"""Unit tests for the ChatGPT browser bridge CLI."""

from __future__ import annotations

import importlib.util
import pathlib
import sys
import unittest
from unittest import mock
import subprocess


MODULE_PATH = (
    pathlib.Path(__file__).resolve().parents[1] / "chatgpt_browser_bridge.py"
)


def load_module():
    spec = importlib.util.spec_from_file_location(
        "chatgpt_browser_bridge",
        MODULE_PATH,
    )
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


class ChatGptBrowserBridgeCliTest(unittest.TestCase):
    def test_read_success_envelope(self):
        module = load_module()

        with mock.patch.object(
            module,
            "handle_read",
            return_value=module.CommandResult(
                url="https://chatgpt.com/c/example",
                title="Example chat",
                data={"messages": [], "composer": {"available": True}},
            ),
        ):
            exit_code, payload = module.dispatch(["read"])

        self.assertEqual(exit_code, 0)
        self.assertTrue(payload["ok"])
        self.assertEqual(payload["command"], "read")
        self.assertEqual(payload["url"], "https://chatgpt.com/c/example")
        self.assertEqual(payload["title"], "Example chat")
        self.assertIn("data", payload)

    def test_list_tabs_success_envelope(self):
        module = load_module()

        with mock.patch.object(
            module,
            "handle_list_tabs",
            return_value=module.CommandResult(
                url="",
                title="",
                data={"tabs": [{"window_index": 1, "tab_index": 2}]},
            ),
        ):
            exit_code, payload = module.dispatch(["list-tabs"])

        self.assertEqual(exit_code, 0)
        self.assertTrue(payload["ok"])
        self.assertEqual(payload["command"], "list-tabs")
        self.assertIn("tabs", payload["data"])

    def test_type_requires_text(self):
        module = load_module()

        exit_code, payload = module.dispatch(["type"])

        self.assertEqual(exit_code, module.EXIT_ENVIRONMENT)
        self.assertFalse(payload["ok"])
        self.assertEqual(payload["error"]["code"], "TEXT_REQUIRED")

    def test_send_uses_explicit_handler(self):
        module = load_module()

        with mock.patch.object(
            module,
            "handle_send",
            return_value=module.CommandResult(
                url="https://chatgpt.com/c/example",
                title="Example chat",
                data={"sent": True},
            ),
        ) as mocked_send:
            exit_code, payload = module.dispatch(["send"])

        mocked_send.assert_called_once()
        self.assertEqual(exit_code, 0)
        self.assertTrue(payload["data"]["sent"])

    def test_bridge_error_becomes_error_envelope(self):
        module = load_module()

        with mock.patch.object(
            module,
            "handle_read",
            side_effect=module.BridgeCommandError(
                "TARGET_TAB_NOT_FOUND",
                "未找到标签页",
                module.EXIT_TAB_NOT_FOUND,
            ),
        ):
            exit_code, payload = module.dispatch(["read"])

        self.assertEqual(exit_code, module.EXIT_TAB_NOT_FOUND)
        self.assertFalse(payload["ok"])
        self.assertEqual(payload["command"], "read")
        self.assertEqual(payload["error"]["code"], "TARGET_TAB_NOT_FOUND")


class ChatGptBrowserBridgeLogicTest(unittest.TestCase):
    def test_find_chatgpt_tabs_parses_active_flag(self):
        module = load_module()
        separator = module.FIELD_SEPARATOR
        output = (
            f"101{separator}1{separator}3{separator}当前会话{separator}https://chatgpt.com/c/active{separator}true\n"
            f"102{separator}2{separator}1{separator}另一个会话{separator}https://chatgpt.com/c/other{separator}false"
        )

        with mock.patch.object(module, "run_osascript", return_value=output):
            tabs = module.find_chatgpt_tabs()

        self.assertEqual(len(tabs), 2)
        self.assertTrue(tabs[0].active)
        self.assertEqual(tabs[0].url, "https://chatgpt.com/c/active")
        self.assertFalse(tabs[1].active)

    def test_select_target_tab_prefers_active_tab(self):
        module = load_module()
        first = module.TabInfo(101, 1, 1, "A", "https://chatgpt.com/c/a", False)
        second = module.TabInfo(102, 2, 4, "B", "https://chatgpt.com/c/b", True)

        with mock.patch.object(module, "find_chatgpt_tabs", return_value=[first, second]):
            with mock.patch.object(module, "activate_tab") as mocked_activate:
                selected = module.select_target_tab()

        self.assertEqual(selected.url, "https://chatgpt.com/c/b")
        mocked_activate.assert_called_once_with(second)

    def test_perform_send_requires_existing_input(self):
        module = load_module()
        tab = module.TabInfo(101, 1, 1, "A", "https://chatgpt.com/c/a", True)

        with mock.patch.object(
            module,
            "read_snapshot",
            return_value={
                "url": tab.url,
                "title": tab.title,
                "bodyText": "页面正常",
                "composer": {"currentText": ""},
            },
        ):
            with self.assertRaises(module.BridgeCommandError) as captured:
                module.perform_send(tab, max_chars=1000)

        self.assertEqual(captured.exception.code, "NOTHING_TO_SEND")

    def test_perform_send_confirms_composer_changed(self):
        module = load_module()
        tab = module.TabInfo(101, 1, 1, "A", "https://chatgpt.com/c/a", True)
        before = {
            "url": tab.url,
            "title": tab.title,
            "bodyText": "页面正常",
            "composer": {"currentText": "bridge smoke"},
        }
        after = {
            "url": tab.url,
            "title": tab.title,
            "bodyText": "页面正常",
            "composer": {"currentText": ""},
        }

        with mock.patch.object(module, "read_snapshot", side_effect=[before, after]):
            with mock.patch.object(
                module,
                "execute_javascript_on_selected_tab",
                return_value='{"sent": true, "method": "button"}',
            ):
                with mock.patch.object(module.time, "sleep"):
                    result = module.perform_send(tab, max_chars=1000)

        self.assertTrue(result["sent"])
        self.assertEqual(result["method"], "button")
        self.assertEqual(result["composer"]["currentText"], "")

    def test_run_osascript_maps_apple_events_disabled(self):
        module = load_module()
        process = subprocess.CompletedProcess(
            args=["osascript"],
            returncode=1,
            stdout="",
            stderr="Google Chrome got an error: Executing JavaScript through AppleScript is turned off.",
        )

        with mock.patch.object(module, "ensure_chrome_running"):
            with mock.patch.object(module.subprocess, "run", return_value=process):
                with self.assertRaises(module.BridgeCommandError) as captured:
                    module.run_osascript("return 1")

        self.assertEqual(
            captured.exception.code,
            "APPLE_EVENTS_JAVASCRIPT_DISABLED",
        )
        self.assertEqual(captured.exception.exit_code, module.EXIT_ENVIRONMENT)

    def test_handle_type_checks_page_readable_before_typing(self):
        module = load_module()
        args = module.create_parser().parse_args(["type", "--text", "bridge smoke"])
        tab = module.TabInfo(101, 1, 1, "A", "https://chatgpt.com/c/a", True)
        snapshot = {
            "url": tab.url,
            "title": tab.title,
            "bodyText": "页面正常",
            "composer": {"currentText": ""},
        }

        with mock.patch.object(module, "select_target_tab", return_value=tab):
            with mock.patch.object(module, "read_snapshot", return_value=snapshot):
                with mock.patch.object(module, "ensure_page_readable") as mocked_readable:
                    with mock.patch.object(
                        module,
                        "perform_type",
                        return_value={"mode": "textarea", "currentText": "bridge smoke"},
                    ):
                        module.handle_type(args)

        mocked_readable.assert_called_once_with(snapshot)


if __name__ == "__main__":
    unittest.main()
