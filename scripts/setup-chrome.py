"""Install a matching official Chrome for Testing pair for the disposable CI runner."""
import json
import os
from pathlib import Path
import shutil
import subprocess
import urllib.request
import zipfile

platform = "win64" if os.name == "nt" else "linux64"
root = Path(os.environ["RUNNER_TEMP"]) / "shop-browser"
root.mkdir(exist_ok=True)
manifest = "https://googlechromelabs.github.io/chrome-for-testing/last-known-good-versions-with-downloads.json"
with urllib.request.urlopen(manifest, timeout=30) as response:
    stable = json.load(response)["channels"]["Stable"]
for tool, variable in (("chrome", "SHOP_CHROME_BINARY"), ("chromedriver", "SHOP_CHROMEDRIVER")):
    url = next(entry["url"] for entry in stable["downloads"][tool] if entry["platform"] == platform)
    archive = root / (tool + ".zip")
    with urllib.request.urlopen(url, timeout=90) as response, archive.open("wb") as stream:
        shutil.copyfileobj(response, stream)
    if os.name == "nt":
        with zipfile.ZipFile(archive) as bundle:
            bundle.extractall(root)
    else:
        subprocess.run(["unzip", "-oq", str(archive), "-d", str(root)], check=True)
    binary = root / f"{tool}-{platform}" / (tool + (".exe" if os.name == "nt" else ""))
    if not binary.is_file():
        raise FileNotFoundError(binary)
    with open(os.environ["GITHUB_ENV"], "a", encoding="utf-8") as env:
        print(f"{variable}={binary}", file=env)
print("Chrome and ChromeDriver distribution version", stable["version"])
