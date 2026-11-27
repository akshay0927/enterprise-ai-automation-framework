"""Verify uploaded JSON against independent downloads of the platform Surefire artifacts."""
from html.parser import HTMLParser
import json
from pathlib import Path
import sys

from execution_summary import bounded, require, summarize


class DashboardCheck(HTMLParser):
    def __init__(self):
        super().__init__()
        self.headings = 0
        self.cells = 0
        self.text = []

    def handle_starttag(self, tag, attrs):
        require(tag not in {"script", "iframe", "object"} and all(not k.startswith("on") for k, _ in attrs),
                "unexpected_active_content")
        self.headings += tag == "h1"
        self.cells += tag == "td"

    def handle_data(self, data):
        self.text.append(data)


def read(path):
    return json.loads(bounded(path, 2*1024*1024))


def main():
    root = Path("target")
    bundle = root / "published-quality"
    comparison = read(bundle / "comparison.json")
    for platform in ("Windows", "Linux"):
        uploaded = read(root / "platforms" / ("execution-summary-" + platform) / "execution-summary.json")
        replay = summarize(root / "raw-evidence" / ("smoke-reports-" + platform))
        require(all(uploaded.get(key) == value for key, value in replay.items()), "artifact_replay_mismatch")
        require(uploaded["testedCommit"] == comparison["testedCommit"] == sys.argv[1], "artifact_revision_mismatch")
        print(platform, "source fields reproduced:", len(replay), "tests:", len(replay["tests"]))
    parser = DashboardCheck()
    parser.feed(bounded(bundle / "dashboard.html", 2*1024*1024))
    parser.close()
    require(parser.headings == 1 and parser.cells > 0 and sys.argv[1] in "".join(parser.text), "invalid_dashboard")
    print("Published dashboard verified against checked-out revision")


if __name__ == "__main__":
    main()
