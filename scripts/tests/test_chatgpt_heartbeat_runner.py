import importlib.util
import pathlib
import unittest


MODULE_PATH = pathlib.Path(__file__).resolve().parents[1] / "chatgpt_heartbeat_runner.py"


def load_module():
    spec = importlib.util.spec_from_file_location("chatgpt_heartbeat_runner", MODULE_PATH)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module


class HeartbeatRunnerTests(unittest.TestCase):
    def setUp(self):
        self.module = load_module()

    def test_digest_payload_is_stable(self):
        payload = {"data": {"bodyText": "abc", "messages": []}}
        self.assertEqual(
            self.module.digest_payload(payload),
            self.module.digest_payload({"data": {"messages": [], "bodyText": "abc"}}),
        )

    def test_extract_latest_message_text_prefers_latest_message(self):
        payload = {
            "data": {
                "messages": [{"text": "old"}, {"content": "latest"}],
                "bodyText": "fallback",
            }
        }
        self.assertEqual(self.module.extract_latest_message_text(payload), "latest")

    def test_extract_latest_message_text_falls_back_to_body_text(self):
        payload = {"data": {"messages": [], "bodyText": "page body"}}
        self.assertEqual(self.module.extract_latest_message_text(payload), "page body")

    def test_extract_tracking_reads_latest_user_and_assistant_indexes(self):
        payload = {
            "data": {
                "messages": [
                    {"index": 10, "role": "user", "text": "u1"},
                    {"index": 11, "role": "assistant", "text": "a1"},
                    {"index": 12, "role": "user", "text": "u2"},
                ]
            }
        }
        tracking = self.module.extract_tracking(payload)
        self.assertEqual(tracking["latest_index"], 12)
        self.assertEqual(tracking["latest_user_index"], 12)
        self.assertEqual(tracking["latest_assistant_index"], 11)

    def test_has_context_marker_detects_synced_prompt(self):
        payload = {
            "data": {
                "messages": [
                    {"role": "user", "text": "同步我们当前方案，请按这个口径继续协作：\nfoo"}
                ]
            }
        }
        self.assertTrue(self.module.has_context_marker(payload, "同步我们当前方案"))

    def test_infer_conversation_stage_detects_assistant_turn(self):
        payload = {
            "data": {
                "messages": [
                    {"role": "user", "text": "u"},
                    {"role": "assistant", "text": "a"},
                ],
                "bodyText": "普通正文",
            }
        }
        self.assertEqual(self.module.infer_conversation_stage(payload), "waiting_our_followup")

    def test_should_activate_codex_when_new_assistant_reply_arrives(self):
        previous_state = {
            "conversation_stage": "waiting_gpt_reply",
            "last_seen_text": "上一条用户消息",
        }
        self.assertTrue(
            self.module.should_activate_codex(
                previous_state, "waiting_our_followup", "这是 GPT 的新回复"
            )
        )

    def test_should_not_activate_codex_for_duplicate_assistant_reply(self):
        previous_state = {
            "conversation_stage": "waiting_our_followup",
            "last_seen_text": "这是 GPT 的新回复",
        }
        self.assertFalse(
            self.module.should_activate_codex(
                previous_state, "waiting_our_followup", "这是 GPT 的新回复"
            )
        )

    def test_should_not_activate_codex_when_waiting_for_gpt(self):
        previous_state = {
            "conversation_stage": "waiting_gpt_reply",
            "last_seen_text": "上一条用户消息",
        }
        self.assertFalse(
            self.module.should_activate_codex(
                previous_state, "waiting_gpt_reply", "上一条用户消息"
            )
        )

    def test_should_attempt_followup_requires_context_marker(self):
        previous_state = {
            "conversation_stage": "waiting_gpt_reply",
            "last_seen_text": "上一条用户消息",
        }
        self.assertFalse(
            self.module.should_attempt_followup(
                previous_state,
                "assistant_replied_pending_followup",
                False,
            )
        )

    def test_should_attempt_followup_for_new_assistant_reply_with_context(self):
        previous_state = {
            "conversation_stage": "waiting_gpt_reply",
            "last_seen_text": "上一条用户消息",
        }
        self.assertTrue(
            self.module.should_attempt_followup(
                previous_state,
                "assistant_replied_pending_followup",
                True,
            )
        )

    def test_infer_runner_state_detects_pending_unconsumed_assistant_reply(self):
        previous_state = {
            "last_consumed_assistant_index": 20,
            "last_sent_user_index": 21,
        }
        tracking = {
            "latest_index": 22,
            "latest_role": "assistant",
            "latest_user_index": 21,
            "latest_assistant_index": 22,
        }
        self.assertEqual(
            self.module.infer_runner_state(previous_state, tracking, 180),
            "assistant_replied_pending_followup",
        )

    def test_infer_runner_state_detects_waiting_for_assistant_after_send(self):
        previous_state = {
            "last_consumed_assistant_index": 30,
            "last_sent_user_index": 31,
            "last_sent_at": "2026-04-02T03:00:00+00:00",
        }
        tracking = {
            "latest_index": 31,
            "latest_role": "user",
            "latest_user_index": 31,
            "latest_assistant_index": 30,
        }
        self.assertEqual(
            self.module.infer_runner_state(previous_state, tracking, 999999),
            "user_sent_waiting_assistant",
        )


if __name__ == "__main__":
    unittest.main()
