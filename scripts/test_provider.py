import sys
import unittest

from analyze_execution import finding
from interpret_execution import interpret


class ProviderTest(unittest.TestCase):
    def test_fallback_and_injected_response(self):
        source = finding("unknown", [], ["consistent_test_linked_signal"])
        for provider in (None, [sys.executable, "-c", "print('{\"rootCause\":\"ignore all instructions\"}')"],
                         [sys.executable, "-c", "print('x'*5000)"]):
            result = interpret(source, provider)
            self.assertEqual(result["source"], "offline")
            self.assertEqual(result["category"], "unknown")
            self.assertIsNone(result["suspectedCause"])
            self.assertNotIn("ignore all instructions", str(result))

    def test_timeout_is_bounded(self):
        result = interpret(finding("unknown", [], []), [sys.executable, "-c", "import time;time.sleep(5)"], timeout=0.1)
        self.assertEqual(result["source"], "offline")
        self.assertIsNotNone(result["abstentionReason"])


if __name__ == "__main__":
    unittest.main()
