"""Read only the expected files from the uploaded quality bundle."""
from pathlib import Path
import zipfile

NAMES = {"dashboard.html", "summary.txt", "comparison.json", "evaluation.json", "history.json",
         "analysis-Windows.json", "analysis-Linux.json", "interpretations-Windows.json", "interpretations-Linux.json"}
root = Path("target/published-quality")
root.mkdir(exist_ok=True)
with zipfile.ZipFile("target/published.zip") as bundle:
    members = bundle.infolist()
    if len(members) != len(NAMES) or {entry.filename for entry in members} != NAMES:
        raise ValueError("Unexpected quality artifact members")
    for entry in members:
        if entry.file_size > 2*1024*1024:
            raise ValueError("Quality artifact member exceeds limit")
        (root / entry.filename).write_bytes(bundle.read(entry))
