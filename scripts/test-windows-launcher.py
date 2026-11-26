"""Verify the actual PowerShell launcher from outside a checkout with spaces."""
from pathlib import Path
import os
import queue
import shutil
import subprocess
import tempfile
import threading
import urllib.request

root = Path(__file__).resolve().parents[1]
with tempfile.TemporaryDirectory() as tmp:
    project = Path(tmp) / "Automation All" / "Enterprise Shop"
    launcher = project / "scripts/start-shop.ps1"
    launcher.parent.mkdir(parents=True)
    shutil.copy2(root / "scripts/start-shop.ps1", launcher)
    shutil.copytree(root / "src/main/java", project / "src/main/java")
    proc = subprocess.Popen(["pwsh", "-NoProfile", "-File", str(launcher), "-NoBrowser"],
                            cwd=tmp, stdout=subprocess.PIPE, text=True)
    try:
        lines = queue.Queue()
        threading.Thread(target=lambda: lines.put(proc.stdout.readline()), daemon=True).start()
        line = lines.get(timeout=45).strip()
        prefix = "Fixture shop: http://127.0.0.1:"
        if not line.startswith(prefix) or not line[len(prefix):].isdigit():
            raise RuntimeError("Launcher did not print a loopback URL")
        url = line.split("Fixture shop: ", 1)[1]
        with urllib.request.urlopen(url + "/health", timeout=5) as response:
            import json
            assert json.load(response) == {"status": "up"}
        print("Launcher served a healthy shop from a checkout path with spaces")
    finally:
        if proc.poll() is None:
            subprocess.run(["taskkill", "/PID", str(proc.pid), "/T", "/F"], check=True)
        proc.wait(timeout=10)
        proc.stdout.close()
